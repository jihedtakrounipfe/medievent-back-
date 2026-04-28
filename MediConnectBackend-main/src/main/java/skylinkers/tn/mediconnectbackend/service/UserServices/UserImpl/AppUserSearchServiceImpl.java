package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import skylinkers.tn.mediconnectbackend.dto.request.UserSearchCriteria;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.IAppUserSearchService;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppUserSearchServiceImpl implements IAppUserSearchService {

    private final AppUserRepository userRepository;

    // ────────────────────────────────────────────────────────────────────────
    // Search
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public Page<AppUserResponse> search(UserSearchCriteria criteria, Pageable pageable) {
        Specification<AppUser> spec = buildSpecification(criteria);
        return userRepository.findAll(spec, pageable)
                .map(this::toResponse);
    }

    @Override
    public AppUserResponse getByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException(
                        "User not found for keycloakId: " + keycloakId));
    }

    @Override
    public AppUserResponse getById(Long id) {
        return userRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NoSuchElementException(
                        "User not found for id: " + id));
    }

    // ────────────────────────────────────────────────────────────────────────
    // Specification builder
    // ────────────────────────────────────────────────────────────────────────

    private Specification<AppUser> buildSpecification(UserSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            var doctorRoot = cb.treat(root, Doctor.class);
            var patientRoot = cb.treat(root, Patient.class);
            Predicate isDoctor = cb.equal(root.type(), Doctor.class);
            Predicate isPatient = cb.equal(root.type(), Patient.class);

            // Free-text across email, firstName, lastName, rppsNumber
            if (c.getQ() != null && !c.getQ().isBlank()) {
                String term = "%" + c.getQ().toLowerCase().trim() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("email")),     term),
                        cb.like(cb.lower(root.get("firstName")), term),
                        cb.like(cb.lower(root.get("lastName")),  term),
                        cb.and(
                                isDoctor,
                                cb.like(cb.lower(doctorRoot.get("rppsNumber")), term)
                        )
                ));
            }

            // Legacy name search
            if (c.getName() != null && !c.getName().isBlank()) {
                String pattern = "%" + c.getName().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")),  pattern)
                ));
            }

            if (c.getEmail() != null && !c.getEmail().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("email")),
                        "%" + c.getEmail().toLowerCase() + "%"
                ));
            }

            // User type (discriminator)
            if (c.getUserType() != null) {
                predicates.add(cb.equal(root.get("userType"), c.getUserType()));
            }

            if (c.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), c.getIsActive()));
            }

            // Specialization (typed enum from criteria)
            if (c.getSpecialization() != null) {
                predicates.add(cb.and(
                        isDoctor,
                        cb.equal(doctorRoot.get("specialization"), c.getSpecialization())
                ));
            }

            // Verification status (string → enum)
            if (c.getStatus() != null && !c.getStatus().isBlank()) {
                try {
                    VerificationStatus vs = VerificationStatus.valueOf(c.getStatus().trim().toUpperCase());
                    predicates.add(cb.and(
                            isDoctor,
                            cb.equal(doctorRoot.get("verificationStatus"), vs)
                    ));
                } catch (IllegalArgumentException ignored) {}
            }

            // Blood type — supports single value or comma-separated list
            if (c.getBloodType() != null && !c.getBloodType().isBlank()) {
                String[] types = c.getBloodType().split(",");
                if (types.length == 1) {
                    String bloodType = types[0].trim().toUpperCase();
                    predicates.add(cb.and(
                            isPatient,
                            cb.equal(cb.upper(patientRoot.get("bloodType")), bloodType)
                    ));
                } else {
                    List<String> typeList = Arrays.stream(types)
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .toList();
                    predicates.add(cb.and(
                            isPatient,
                            cb.upper(patientRoot.get("bloodType")).in(typeList)
                    ));
                }
            }

            // Age range (computed from dateOfBirth)
            if (c.getAgeMin() != null) {
                LocalDate maxBirth = LocalDate.now().minusYears(c.getAgeMin());
                predicates.add(cb.lessThanOrEqualTo(root.get("dateOfBirth"), maxBirth));
            }
            if (c.getAgeMax() != null) {
                LocalDate minBirth = LocalDate.now().minusYears((long) c.getAgeMax() + 1).plusDays(1);
                predicates.add(cb.greaterThanOrEqualTo(root.get("dateOfBirth"), minBirth));
            }

            // Sign-up date range
            if (c.getSignedUpSince() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        c.getSignedUpSince().atStartOfDay()));
            }
            if (c.getSignedUpUntil() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        c.getSignedUpUntil().atTime(23, 59, 59)));
            }

            // 2FA status
            if (c.getIs2FAEnabled() != null) {
                predicates.add(cb.equal(root.get("twoFactorEnabled"), c.getIs2FAEnabled()));
            }

            // City (officeAddress for doctors, address for patients)
            if (c.getCity() != null && !c.getCity().isBlank()) {
                String pattern = "%" + c.getCity().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.and(
                                isDoctor,
                                cb.like(cb.lower(doctorRoot.get("officeAddress")), pattern)
                        ),
                        cb.like(cb.lower(root.get("address")),       pattern)
                ));
            }

            if (c.getIsVerified() != null && Boolean.TRUE.equals(c.getIsVerified())) {
                predicates.add(cb.equal(root.get("isVerified"), true));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ────────────────────────────────────────────────────────────────────────
    // Mapper
    // ────────────────────────────────────────────────────────────────────────

    private AppUserResponse toResponse(AppUser user) {
        var builder = AppUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userType(user.getUserType())
                .isActive(user.isActive())
                .isVerified(user.getIsVerified())
                .profilePicture(user.getProfilePicture())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dateOfBirth(user.getDateOfBirth())
                .age(computeAge(user.getDateOfBirth()))
                .twoFactorEnabled(user.isTwoFactorEnabled());

        if (user instanceof Doctor doctor) {
            builder.specialization(doctor.getSpecialization() != null
                            ? doctor.getSpecialization().name() : null)
                    .specializationLabel(specializationLabel(doctor.getSpecialization()))
                    .officeAddress(doctor.getOfficeAddress())
                    .verificationStatus(doctor.getVerificationStatus() != null
                            ? doctor.getVerificationStatus().name() : null)
                    .rppsNumber(doctor.getRppsNumber())
                    .consultationFee(doctor.getConsultationFee());
        }

        if (user instanceof Patient patient) {
            builder.bloodType(patient.getBloodType())
                    .biometricEnrolled(patient.isBiometricEnrolled());
        }

        return builder.build();
    }

    private Integer computeAge(LocalDate dateOfBirth) {
        if (dateOfBirth == null) return null;
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    private String specializationLabel(Specialization spec) {
        if (spec == null) return null;
        return switch (spec) {
            case GENERAL_PRACTICE -> "Médecine générale";
            case CARDIOLOGY       -> "Cardiologie";
            case DERMATOLOGY      -> "Dermatologie";
            case PEDIATRICS       -> "Pédiatrie";
            case NEUROLOGY        -> "Neurologie";
            case ORTHOPEDICS      -> "Orthopédie";
            case PSYCHIATRY       -> "Psychiatrie";
            case RADIOLOGY        -> "Radiologie";
            case OTHER            -> "Autre";
        };
    }
}
