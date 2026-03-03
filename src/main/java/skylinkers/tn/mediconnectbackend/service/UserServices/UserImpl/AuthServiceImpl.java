package skylinkers.tn.mediconnectbackend.service.UserServices;

import skylinkers.tn.mediconnectbackend.dto.request.*;
import skylinkers.tn.mediconnectbackend.dto.response.AuthResponse;
import skylinkers.tn.mediconnectbackend.dto.response.DoctorResponse;
import skylinkers.tn.mediconnectbackend.dto.response.PatientResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestrates authentication flows.
 *
 * NOTE: While Keycloak is disabled (DevSecurityConfig permits all),
 *       login simply validates credentials against the local DB.
 *       Swap the body of login() / logout() once Keycloak is re-enabled.
 *
 * SOLID — SRP: only auth orchestration; no direct DB access (delegates to
 *              PatientService / DoctorService for entity creation).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final PatientService patientService;
    private final DoctorService  doctorService;
    private final AppUserRepository appUserRepository;


    // ── TODO (Keycloak re-enable): inject KeycloakAdminClient, JwtDecoder ──

    // ────────────────────────────────────────────────────────────────────────
    // Login
    // ────────────────────────────────────────────────────────────────────────

    /**
     * DEV MODE: Keycloak disabled — returns a stub token.
     * PROD MODE: delegate to Keycloak Resource Owner Password flow
     *            (or redirect to Authorization Code flow from the frontend).
     */
    @Override
    public AuthResponse login(LoginRequest request) {

        log.info("[AUTH] Login attempt for {}", request.getEmail());

        AppUser user = appUserRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isActive()) {
            throw new RuntimeException("User account is disabled");
        }

        // ⚠ DEV MODE: password validation skipped because Keycloak is disabled
        // Later Keycloak will validate password before we reach here.


        return AuthResponse.builder()
                .accessToken("dev-stub-access-token")
                .refreshToken("dev-stub-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .user(mapToUserResponse(user))   // ← NOW user is returned
                .build();
    }

    @Override
    public AuthResponse loginWith2FA(LoginWith2FARequest request) {
        log.info("[AUTH] 2FA login attempt for {}", request.getEmail());

        // TODO (Keycloak): validate TOTP via Keycloak OTP API then issue tokens
        return AuthResponse.builder()
                .accessToken("dev-stub-2fa-access-token")
                .refreshToken("dev-stub-refresh-token")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .message("DEV MODE — 2FA stub")
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Registration
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public AuthResponse registerPatient(CreatePatientRequest request) {
        log.info("[AUTH] Patient registration: {}", request.getEmail());

        if (request.getKeycloakId() == null || request.getKeycloakId().isBlank()) {
            request.setKeycloakId("TEMP-" + UUID.randomUUID());
        } else {
            request.setKeycloakId(request.getKeycloakId());
        }

        // Creates the entity in MySQL via PatientService
        var patient = patientService.createPatient(request);

        // TODO (Keycloak): after DB creation, also create Keycloak account:
        //   keycloakAdminClient.createUser(request.getEmail(), request.getPassword());
        //   keycloakAdminClient.sendVerificationEmail(patient.getKeycloakId());

        return AuthResponse.builder()
                .message("Registration successful. Please verify your email.")
                .user(patient)
                .build();
    }

    @Override
    public AuthResponse registerDoctor(CreateDoctorRequest request) {
        log.info("[AUTH] Doctor registration: {} (RPPS: {})", request.getEmail(), request.getRppsNumber());

        if (request.getKeycloakId() == null || request.getKeycloakId().isBlank()) {
            request.setKeycloakId("TEMP-" + UUID.randomUUID());
        } else {
            request.setKeycloakId(request.getKeycloakId());
        }

        var doctor = doctorService.createDoctor(request);

        // TODO (Keycloak): create Keycloak account with PENDING attribute,
        //   block login until admin approves verificationStatus

        return AuthResponse.builder()
                .message("Registration submitted. Your account is pending admin verification.")
                .user(doctor)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Logout & Refresh
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void logout(String keycloakId) {
        log.info("[AUTH] Logout for keycloakId={}", keycloakId);

        // TODO (Keycloak): keycloakAdminClient.logoutUser(keycloakId);
        // TODO (Google): if user has linked Google, revoke OAuth token from mc_oauth_tokens
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("[AUTH] Token refresh requested");

        // TODO (Keycloak): KeycloakTokenResponse tokens =
        //   keycloakClient.refreshToken(refreshToken);
        //   return buildAuthResponse(tokens);

        return AuthResponse.builder()
                .accessToken("dev-stub-refreshed-token")
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    private Object mapToUserResponse(AppUser user) {

        switch (user.getUserType()) {

            case PATIENT:
                return PatientResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .build();

            case DOCTOR:
                return DoctorResponse.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .build();

            default:
                throw new IllegalStateException("Unsupported user type");
        }
    }
}