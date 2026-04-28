package skylinkers.tn.mediconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.dto.request.ForgotPasswordRequest;
import skylinkers.tn.mediconnectbackend.dto.request.ResetPasswordRequest;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import skylinkers.tn.mediconnectbackend.dto.response.AuthResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.entities.PasswordResetToken;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.repository.PasswordResetTokenRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.DoctorService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.PatientService;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakAdminClient;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient.KeycloakTokenResponse;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Handles the two-step password reset flow:
 * 1. initiateReset  - validates email, generates 6-digit code, sends email.
 * 2. confirmReset   - validates code, resets password, clears all MFA state,
 *                     and automatically signs the user back in when possible.
 *
 * Security: step 1 always returns a generic 200 message to avoid user enumeration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final int CODE_LENGTH = 6;
    private static final int EXPIRY_MINUTES = 15;
    private final SecureRandom random = new SecureRandom();

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final KeycloakAdminClient keycloakAdminClient;
    private final KeycloakTokenClient keycloakTokenClient;
    private final EmailService emailService;
    private final TwoFactorService twoFactorService;
    private final LoginAttemptService loginAttemptService;
    private final PatientService patientService;
    private final DoctorService doctorService;

    @Transactional
    public void initiateReset(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        userRepository.findByEmail(email).ifPresent(user -> {
            tokenRepository.deleteByEmail(email);

            String code = generateCode();

            PasswordResetToken token = PasswordResetToken.builder()
                    .email(email)
                    .code(code)
                    .expiryAt(LocalDateTime.now().plusMinutes(EXPIRY_MINUTES))
                    .used(false)
                    .build();
            tokenRepository.save(token);

            emailService.sendPasswordResetCode(email, user.getFirstName(), code);
            log.info("[PASSWORD-RESET] Code sent to {}", email);
        });
    }

    @Transactional
    public AuthResponse confirmReset(ResetPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Les mots de passe ne correspondent pas.");
        }

        PasswordResetToken token = tokenRepository
                .findByEmailAndCodeAndUsedFalse(email, request.getCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Code invalide ou expire."));

        if (token.getExpiryAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le code a expire. Veuillez en demander un nouveau.");
        }

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable."));

        try {
            keycloakAdminClient.resetPasswordByEmail(email, request.getNewPassword());
        } catch (Exception e) {
            log.error("[PASSWORD-RESET] Keycloak reset failed for {}: {}", email, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Impossible de reinitialiser le mot de passe. Verifiez que Keycloak est disponible.");
        }

        try {
            keycloakAdminClient.clearRequiredActions(user.getKeycloakId());
        } catch (Exception ex) {
            log.warn("[PASSWORD-RESET] Could not clear required actions for {}: {}", email, ex.getMessage());
        }

        if (user.getGoogleId() != null) {
            user.setGoogleInternalPassword(request.getNewPassword());
        }

        twoFactorService.disableAllMfa(user);
        loginAttemptService.resetAttempts(email);

        token.setUsed(true);
        tokenRepository.save(token);
        userRepository.save(user);

        log.info("[PASSWORD-RESET] Password reset completed for {} - MFA fully reset", email);

        String successMessage = "Mot de passe reinitialise avec succes. Toutes les methodes MFA ont ete desactivees. Reconfigurez-les depuis vos parametres de securite.";

        try {
            KeycloakTokenResponse tokens = keycloakTokenClient.passwordGrant(email, request.getNewPassword());
            return AuthResponse.builder()
                    .accessToken(tokens.accessToken())
                    .refreshToken(tokens.refreshToken())
                    .tokenType(tokens.tokenType())
                    .expiresIn(tokens.expiresIn())
                    .user(mapToUserResponse(user))
                    .message(successMessage)
                    .build();
        } catch (Exception ex) {
            log.warn("[PASSWORD-RESET] Auto-login failed after reset for {}: {}", email, ex.getMessage());
            return AuthResponse.builder()
                    .message(successMessage)
                    .build();
        }
    }

    private Object mapToUserResponse(AppUser user) {
        if (user instanceof Patient) {
            return patientService.getPatientById(user.getId());
        }
        if (user instanceof Doctor) {
            return doctorService.getDoctorById(user.getId());
        }
        return AppUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userType(user.getUserType() != null ? user.getUserType() : UserType.ADMINISTRATOR)
                .isActive(user.isActive())
                .isVerified(user.getIsVerified())
                .profilePicture(user.getProfilePicture())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .faceEnabled(user.isFaceEnabled())
                .faceEnrolled(user.isFaceEnrolled())
                .build();
    }

    private String generateCode() {
        int upperBound = (int) Math.pow(10, CODE_LENGTH);
        int n = random.nextInt(upperBound);
        return String.format("%0" + CODE_LENGTH + "d", n);
    }
}
