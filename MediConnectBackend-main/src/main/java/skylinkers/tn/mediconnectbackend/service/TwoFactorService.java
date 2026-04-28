package skylinkers.tn.mediconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.dto.response.TwoFactorMethodDTO;
import skylinkers.tn.mediconnectbackend.dto.response.TwoFactorStatusResponse;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakAdminClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the email-based 2FA toggle for users.
 *
 * Strategy: email OTP only — handled entirely in our Spring Boot layer.
 * Keycloak is NOT involved in the 2FA flow (avoids ROPC conflicts with
 * required actions like CONFIGURE_TOTP).
 *
 * State is tracked solely in the MySQL users.two_factor_enabled column.
 * The 2FA verification code lifecycle is managed by AuthServiceImpl.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorService {

    private final AppUserRepository userRepository;
    private final TotpService totpService;
    private final FaceRecognitionService faceRecognitionService;
    private final KeycloakAdminClient keycloakAdminClient;

    /**
     * Get the current 2FA status for a user — reads from MySQL only.
     *
     * @param keycloakId the user's Keycloak UUID (JWT sub claim)
     */
    public TwoFactorStatusResponse getStatus(String keycloakId) {
        boolean enabled = userRepository.findByKeycloakId(keycloakId)
                .map(AppUser::isTwoFactorEnabled)
                .orElse(false);

        return TwoFactorStatusResponse.builder()
                .enabled(enabled)
                .methods(List.of(
                        TwoFactorMethodDTO.builder()
                                .type("EMAIL")
                                .label("Vérification par e-mail")
                                .configured(enabled)
                                .available(true)
                                .build()
                ))
                .build();
    }

    /**
     * Enable email 2FA for a user — sets the DB flag only.
     * No Keycloak required action is added (would break ROPC grant).
     *
     * @param keycloakId the user's Keycloak UUID
     */
    public void enable2FA(String keycloakId) {
        log.info("[2FA] Enabling email 2FA for keycloakId={}", keycloakId);
        userRepository.findByKeycloakId(keycloakId).ifPresent(user -> {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);
        });
    }

    /**
     * Disable email 2FA for a user — clears the DB flag only.
     *
     * @param keycloakId the user's Keycloak UUID
     */
    @Transactional
    public void disable2FA(String keycloakId) {
        log.info("[2FA] Disabling email 2FA for keycloakId={}", keycloakId);
        userRepository.findByKeycloakId(keycloakId).ifPresent(this::disableAllMfa);
    }

    /**
     * Returns the ordered list of login MFA methods that are still valid under
     * the current dependency rules:
     * - EMAIL is the base method.
     * - TOTP requires EMAIL to stay active.
     * - FACE requires EMAIL or an already-configured TOTP method.
     */
    @Transactional
    public List<String> getEnabledMethods(AppUser user) {
        sanitizeInvalidMfaState(user);

        List<String> methods = new ArrayList<>();
        boolean emailEnabled = user.isTwoFactorEnabled();
        boolean totpEnabled = emailEnabled
                && user.isTotpEnabled()
                && user.isTotpEnrolled()
                && user.getTotpSecret() != null
                && !user.getTotpSecret().isBlank();
        boolean faceEnabled = (emailEnabled || totpEnabled)
                && user.isFaceEnabled()
                && user.isFaceEnrolled();

        if (faceEnabled) {
            methods.add("FACE");
        }
        if (totpEnabled) {
            methods.add("TOTP");
        }
        if (emailEnabled) {
            methods.add("EMAIL");
        }
        return methods;
    }

    public boolean canConfigureTotp(AppUser user) {
        sanitizeInvalidMfaState(user);
        return user.isTwoFactorEnabled();
    }

    public boolean canConfigureFace(AppUser user) {
        sanitizeInvalidMfaState(user);
        return user.isTwoFactorEnabled() || (
                user.isTotpEnabled()
                        && user.isTotpEnrolled()
                        && user.getTotpSecret() != null
                        && !user.getTotpSecret().isBlank()
        );
    }

    @Transactional
    public void disableAllMfa(AppUser user) {
        log.info("[2FA] Resetting all MFA methods for userId={}", user.getId());

        if (user.isFaceEnabled() || user.isFaceEnrolled()) {
            try {
                faceRecognitionService.deleteFaceTemplate(user);
            } catch (Exception ex) {
                log.warn("[2FA] Face cleanup failed for userId={}: {}", user.getId(), ex.getMessage());
                user.setFaceEnabled(false);
                user.setFaceEnrolled(false);
            }
        }

        if (user.isTotpEnabled() || user.isTotpEnrolled()
                || (user.getTotpSecret() != null && !user.getTotpSecret().isBlank())) {
            totpService.disableTotp(user);
        }

        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        try {
            keycloakAdminClient.disableOtp(user.getKeycloakId());
        } catch (Exception ex) {
            log.warn("[2FA] Keycloak OTP cleanup failed for userId={}: {}", user.getId(), ex.getMessage());
        }

        try {
            keycloakAdminClient.clearRequiredActions(user.getKeycloakId());
        } catch (Exception ex) {
            log.warn("[2FA] Keycloak required-action cleanup failed for userId={}: {}", user.getId(), ex.getMessage());
        }
    }

    @Transactional
    public void sanitizeInvalidMfaState(AppUser user) {
        boolean emailDisabled = !user.isTwoFactorEnabled();
        boolean hasDependentMethods = user.isTotpEnabled()
                || user.isTotpEnrolled()
                || (user.getTotpSecret() != null && !user.getTotpSecret().isBlank())
                || user.isFaceEnabled()
                || user.isFaceEnrolled();

        if (emailDisabled && hasDependentMethods) {
            log.warn("[2FA] Invalid MFA dependency chain detected for userId={} - clearing dependent methods",
                    user.getId());
            disableAllMfa(user);
        }
    }

    /** Returns a stub status response for dev mode (no Keycloak JWT available). */
    public TwoFactorStatusResponse devModeStubStatus() {
        return TwoFactorStatusResponse.builder()
                .enabled(false)
                .methods(List.of(
                        TwoFactorMethodDTO.builder()
                                .type("EMAIL")
                                .label("Vérification par e-mail")
                                .configured(false)
                                .available(true)
                                .build()
                ))
                .build();
    }
}
