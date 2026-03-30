package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.dto.request.CreateAdminRequest;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import skylinkers.tn.mediconnectbackend.entities.Administrator;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.exception.DuplicateEmailException;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AdministratorRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AdministratorService;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakAdminClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdministratorServiceImpl implements AdministratorService {

    private final AdministratorRepository administratorRepository;
    private final AppUserRepository appUserRepository;
    private final KeycloakAdminClient keycloakAdminClient;

    @Override
    @Transactional
    public AppUserResponse registerAdmin(CreateAdminRequest request) {
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already exists: " + request.getEmail());
        }

        String keycloakId = request.getKeycloakId();
        if (keycloakId == null || keycloakId.isBlank()) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new IllegalArgumentException("Password is required when keycloakId is not provided.");
            }
            keycloakId = keycloakAdminClient.createAdministratorUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName()
            );
        }

        Administrator admin = new Administrator();
        admin.setUserType(UserType.ADMINISTRATOR);
        admin.setEmail(request.getEmail());
        admin.setKeycloakId(keycloakId);
        admin.setFirstName(request.getFirstName());
        admin.setLastName(request.getLastName());
        admin.setAdminLevel(request.getAdminLevel());
        admin.setDepartment(request.getDepartment());
        if (request.getProfilePicture() != null && !request.getProfilePicture().isBlank()) {
            admin.setProfilePicture(request.getProfilePicture().trim());
        }

        Administrator saved = administratorRepository.save(admin);
        log.info("Administrator registered: id={}, email={}", saved.getId(), saved.getEmail());

        return AppUserResponse.builder()
                .id(saved.getId())
                .email(saved.getEmail())
                .firstName(saved.getFirstName())
                .lastName(saved.getLastName())
                .phone(saved.getPhone())
                .userType(saved.getUserType())
                .isActive(saved.isActive())
                .profilePicture(saved.getProfilePicture())
                .createdAt(saved.getCreatedAt())
                .build();
    }
}
