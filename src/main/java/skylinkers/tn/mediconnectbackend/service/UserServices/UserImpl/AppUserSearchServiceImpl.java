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

            // Discriminator filter — restricts to PATIENT or DOCTOR rows
            if (c.getUserType() != null) {
                predicates.add(cb.equal(root.get("userType"), c.getUserType()));
            }

            if (c.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), c.getIsActive()));
            }

            // Doctor-specific filters — safe to add even if userType not set
            if (c.getSpecialization() != null) {
                predicates.add(cb.equal(root.get("specialization"), c.getSpecialization()));
            }

            if (c.getCity() != null && !c.getCity().isBlank()) {
                String pattern = "%" + c.getCity().toLowerCase() + "%";
                // officeAddress for doctors, address for patients
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("officeAddress")), pattern),
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
                .createdAt(user.getCreatedAt());

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
