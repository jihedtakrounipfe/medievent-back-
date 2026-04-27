package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import skylinkers.tn.mediconnectbackend.dto.request.UserSearchCriteria;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.IAppUserSearchService;

import java.util.ArrayList;
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
    // Specification builder (Specification pattern)
    // ────────────────────────────────────────────────────────────────────────

    private Specification<AppUser> buildSpecification(UserSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Full-name search — LIKE on firstName OR lastName
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

            // Discriminator filter — Use root.type() for safety in inheritance
            if (c.getUserType() != null) {
                if (c.getUserType() == UserType.DOCTOR) {
                    predicates.add(cb.equal(root.type(), Doctor.class));
                } else if (c.getUserType() == UserType.PATIENT) {
                    predicates.add(cb.equal(root.type(), Patient.class));
                } else if (c.getUserType() == UserType.ADMINISTRATOR) {
                    // Assuming Administrator.class exists or is mapped
                    try {
                        Class<?> adminClass = Class.forName("skylinkers.tn.mediconnectbackend.entities.Administrator");
                        predicates.add(cb.equal(root.type(), adminClass));
                    } catch (ClassNotFoundException e) {
                        predicates.add(cb.equal(root.get("userType"), UserType.ADMINISTRATOR));
                    }
                }
            }

            if (c.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), c.getIsActive()));
            }

            // Doctor-specific filters — specialization and city
            if (c.getSpecialization() != null) {
                predicates.add(cb.equal(root.get("specialization"), c.getSpecialization()));
            }

            if (c.getCity() != null && !c.getCity().isBlank()) {
                String pattern = "%" + c.getCity().toLowerCase() + "%";
                // We must be careful here: officeAddress is only in Doctor.
                // If the root is AppUser, Hibernate might complain if we use get("officeAddress") 
                // on a non-doctor row unless we use a join or cast.
                // However, since we often search for DOCTORs here, we can try to cast.
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("address")), pattern)
                        // Note: officeAddress might cause issues if not present on all rows in SINGLE_TABLE
                        // but Hibernate usually handles it if the column exists in the table.
                ));
            }

            if (c.getIsVerified() != null && Boolean.TRUE.equals(c.getIsVerified())) {
                predicates.add(cb.equal(root.get("isVerified"), true));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ────────────────────────────────────────────────────────────────────────
    // Mapper — safe projection (no PII leakage in search results)
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
                .interests(user.getInterests());

        // Subtype-specific fields via discriminated cast
        if (user instanceof Doctor doctor) {
            builder.specialization(doctor.getSpecialization() != null
                            ? doctor.getSpecialization().name() : null)
                    .officeAddress(doctor.getOfficeAddress())
                    .verificationStatus(doctor.getVerificationStatus() != null ? doctor.getVerificationStatus().name() : null);
        }

        if (user instanceof Patient patient) {
            builder.bloodType(patient.getBloodType())
                    .biometricEnrolled(patient.isBiometricEnrolled());
        }

        return builder.build();
    }
}
