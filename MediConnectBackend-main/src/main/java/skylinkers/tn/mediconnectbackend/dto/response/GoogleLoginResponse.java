package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient.KeycloakTokenResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GoogleLoginResponse {

    private boolean success;
    private boolean isNewUser;
    private boolean requiresLinking;

    private String email;
    private GoogleUserInfo googleProfile;

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long   expiresIn;
    private String userType;
    private String message;

    // ── Multi-method MFA fields ───────────────────────────────────────────────
    private boolean requiresMfa;
    private String  mfaSessionToken;
    private List<String> enabledMethods;
    private String  primaryMethod;
    private Map<String, Integer> attemptsRemaining;
    private boolean allMethodsExhausted;
    private Long    recoveryCodesRemaining;
    private String  failedMethod;

    // ── Legacy MFA fields (kept for static factory compatibility) ─────────────
    /** @deprecated Use requiresMfa + primaryMethod="EMAIL" */
    private boolean requires2FA;
    /** @deprecated Use requiresMfa + primaryMethod="FACE" */
    private boolean requiresFace;
    /** @deprecated Use enabledMethods + primaryMethod */
    private boolean faceFallback;
    /** @deprecated Use attemptsRemaining.get("FACE") */
    private Integer faceAttemptsRemaining;

    public static GoogleLoginResponse success(KeycloakTokenResponse tokens, AppUser user) {
        return GoogleLoginResponse.builder()
                .success(true)
                .isNewUser(false)
                .requires2FA(false)
                .requiresLinking(false)
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType(tokens.tokenType())
                .expiresIn(tokens.expiresIn())
                .email(user.getEmail())
                .userType(user.getUserType() != null ? user.getUserType().name() : null)
                .build();
    }

    public static GoogleLoginResponse newUser(GoogleUserInfo profile) {
        return GoogleLoginResponse.builder()
                .success(false)
                .isNewUser(true)
                .requires2FA(false)
                .requiresLinking(false)
                .googleProfile(profile)
                .email(profile.getEmail())
                .message("Nouveau compte — veuillez compléter votre inscription")
                .build();
    }

    public static GoogleLoginResponse requires2FA(String email) {
        return GoogleLoginResponse.builder()
                .success(false)
                .isNewUser(false)
                .requires2FA(true)
                .requiresLinking(false)
                .email(email)
                .message("Un code de vérification a été envoyé à votre adresse e-mail")
                .build();
    }

    public static GoogleLoginResponse requiresLinking(String email, GoogleUserInfo profile) {
        return GoogleLoginResponse.builder()
                .success(false)
                .isNewUser(false)
                .requires2FA(false)
                .requiresLinking(true)
                .email(email)
                .googleProfile(profile)
                .message("Un compte existe déjà avec cet e-mail. Confirmez votre mot de passe pour lier votre compte Google.")
                .build();
    }

    public static GoogleLoginResponse requiresFace(String email) {
        return GoogleLoginResponse.builder()
                .success(false)
                .isNewUser(false)
                .requires2FA(false)
                .requiresLinking(false)
                .requiresFace(true)
                .email(email)
                .message("Une vérification par reconnaissance faciale est requise.")
                .build();
    }

    public static GoogleLoginResponse faceFallback(String email) {
        return GoogleLoginResponse.builder()
                .success(false)
                .isNewUser(false)
                .requires2FA(true)
                .requiresLinking(false)
                .requiresFace(false)
                .faceFallback(true)
                .email(email)
                .message("Le service de reconnaissance faciale est temporairement indisponible. Un code a été envoyé par e-mail.")
                .build();
    }
}
