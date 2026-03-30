package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import skylinkers.tn.mediconnectbackend.dto.request.*;
import skylinkers.tn.mediconnectbackend.dto.response.AuthResponse;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.VerificationCodeService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.*;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakAdminClient;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient.KeycloakTokenResponse;
import skylinkers.tn.mediconnectbackend.utils.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

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
    private final KeycloakTokenClient keycloakTokenClient;
    private final KeycloakAdminClient keycloakAdminClient;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;


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

        KeycloakTokenResponse tokens;
        try {
            tokens = keycloakTokenClient.passwordGrant(request.getEmail(), request.getPassword());
        } catch (HttpClientErrorException.BadRequest e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("unauthorized_client") && body.contains("direct access grants")) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Keycloak client 'angular-spa' is not allowed for Direct Access Grants. Enable 'Direct Access Grants' in Keycloak (Clients → angular-spa) or switch to Authorization Code + PKCE.",
                        e
                );
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.", e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak is unreachable. Verify keycloak.server-url and that Keycloak is running.",
                    e
            );
        }

        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found in local DB. Please complete registration first."));

        if (!user.isActive()) {
            throw new RuntimeException("User account is disabled");
        }

        return AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType(tokens.tokenType())
                .expiresIn(tokens.expiresIn())
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    public AuthResponse loginWith2FA(LoginWith2FARequest request) {
        throw new UnsupportedOperationException("2FA login is handled by Keycloak interactive flows.");
    }

    // ────────────────────────────────────────────────────────────────────────
    // Registration
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public AuthResponse registerPatient(CreatePatientRequest request) {
        log.info("[AUTH] Patient registration: {}", request.getEmail());

        String keycloakId;
        try {
            keycloakId = keycloakAdminClient.createPatientUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName()
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak admin authentication failed. Verify KEYCLOAK_ADMIN_USERNAME/KEYCLOAK_ADMIN_PASSWORD and that admin-cli direct access grants are enabled.",
                    e
            );
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak is unreachable. Verify keycloak.server-url and that Keycloak is running.",
                    e
            );
        }
        request.setKeycloakId(keycloakId);

        var patient = patientService.createPatient(request);
        verificationCodeService.generateAndSend(request.getEmail(), request.getFirstName());

        return AuthResponse.builder()
                .message("Verification code sent to your email.")
                .user(patient)
                .build();
    }

    @Override
    public AuthResponse registerDoctor(CreateDoctorRequest request) {
        log.info("[AUTH] Doctor registration: {} (RPPS: {})", request.getEmail(), request.getRppsNumber());

        String keycloakId;
        try {
            keycloakId = keycloakAdminClient.createDoctorUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getRppsNumber()
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak admin authentication failed. Verify KEYCLOAK_ADMIN_USERNAME/KEYCLOAK_ADMIN_PASSWORD and that admin-cli direct access grants are enabled.",
                    e
            );
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak is unreachable. Verify keycloak.server-url and that Keycloak is running.",
                    e
            );
        }
        request.setKeycloakId(keycloakId);

        var doctor = doctorService.createDoctor(request);
        verificationCodeService.generateAndSend(request.getEmail(), request.getFirstName());

        return AuthResponse.builder()
                .message("Verification code sent to your email.")
                .user(doctor)
                .build();
    }

    @Override
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        boolean ok = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
        if (!ok) {
            throw new IllegalArgumentException("Invalid or expired verification code.");
        }

        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setIsVerified(Boolean.TRUE);
        appUserRepository.save(user);

        keycloakAdminClient.setEmailVerifiedByEmail(request.getEmail(), true);
        emailService.sendWelcomeEmail(request.getEmail(), user.getFirstName());

        return AuthResponse.builder()
                .message("Email verified successfully.")
                .build();
    }

    @Override
    public AuthResponse resendVerification(ResendVerificationRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        verificationCodeService.generateAndSend(user.getEmail(), user.getFirstName());
        return AuthResponse.builder()
                .message("Verification code sent.")
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Logout & Refresh
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void logout(String keycloakId) {
        log.info("[AUTH] Logout for keycloakId={}", keycloakId);

        keycloakAdminClient.logoutUserSessions(keycloakId);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("[AUTH] Token refresh requested");

        KeycloakTokenResponse tokens;
        try {
            tokens = keycloakTokenClient.refresh(refreshToken);
        } catch (HttpClientErrorException.BadRequest e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("invalid_grant") && body.contains("Token is not active")) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token expired or revoked. Please log in again.",
                        e
                );
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.", e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak is unreachable. Verify keycloak.server-url and that Keycloak is running.",
                    e
            );
        }

        return AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType(tokens.tokenType())
                .expiresIn(tokens.expiresIn())
                .build();
    }

    private Object mapToUserResponse(AppUser user) {
        if (user instanceof Patient) {
            return patientService.getPatientById(user.getId());
        }
        if (user instanceof Doctor) {
            return doctorService.getDoctorById(user.getId());
        }
        UserType type = user.getUserType();
        return AppUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userType(type)
                .isActive(user.isActive())
                .isVerified(user.getIsVerified())
                .profilePicture(user.getProfilePicture())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
