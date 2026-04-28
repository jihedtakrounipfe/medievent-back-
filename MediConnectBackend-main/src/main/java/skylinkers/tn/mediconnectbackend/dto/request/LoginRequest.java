package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    // ── MFA step fields (new multi-method flow) ──────────────────────────────

    /** Session token returned by the credential step. Present on all MFA steps. */
    private String mfaSessionToken;

    /** Which MFA method the user is currently submitting: "FACE", "TOTP", or "EMAIL". */
    private String mfaMethod;

    /** The 6-digit code for TOTP or email OTP methods. */
    private String mfaCode;

    /** TOTP recovery code (10-char alphanumeric) as last-resort fallback. */
    private String recoveryCode;

    // ── Legacy fields — kept for backward compatibility during transition ─────

    /** Deprecated: use mfaCode + mfaMethod instead. Email OTP code. */
    private String otpCode;

    /** Deprecated: use mfaMethod=FACE + mfaCode instead. Base64 face image. */
    private String faceImage;

    /** Deprecated: use mfaMethod=EMAIL instead. Switches method to email OTP. */
    private Boolean useEmailFallback;

    // ── Anti-bot ───────────────────────────────────────────────────────────────

    /** Required on the initial credential step; skipped for MFA follow-up steps. */
    private String recaptchaToken;
}
