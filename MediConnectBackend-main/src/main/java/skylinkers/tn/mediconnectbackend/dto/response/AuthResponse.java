package skylinkers.tn.mediconnectbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    // ── Success fields ────────────────────────────────────────────────────────
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long   expiresIn;
    private Object user;
    private String message;

    // ── Multi-method MFA fields ───────────────────────────────────────────────

    /** true → credential check passed, MFA required. */
    private Boolean requiresMfa;

    /** In-memory session token (UUID, 5-min TTL) holding MFA state + Keycloak tokens. */
    private String mfaSessionToken;

    /** Ordered list of MFA methods enabled for this user: ["FACE","TOTP","EMAIL"]. */
    private List<String> enabledMethods;

    /** The method the frontend should display first (highest priority with attempts left). */
    private String primaryMethod;

    /** Per-method attempt counters — updated on each failed verification. */
    private Map<String, Integer> attemptsRemaining;

    /** true → every method has been exhausted; account will be reset. */
    private Boolean allMethodsExhausted;

    /** How many single-use TOTP recovery codes the user still has available. */
    private Long recoveryCodesRemaining;

    /** The method that just failed (used to update UI counters on error). */
    private String failedMethod;

    // ── Legacy fields — kept for backward compatibility ───────────────────────
    /** @deprecated Use requiresMfa + primaryMethod="EMAIL" */
    private Boolean requires2FA;
    /** @deprecated Use requiresMfa + primaryMethod="FACE" */
    private Boolean requiresFace;
    /** @deprecated Use enabledMethods + primaryMethod to determine fallback state */
    private Boolean faceFallback;
    /** @deprecated Use attemptsRemaining.get("FACE") */
    private Integer faceAttemptsRemaining;
    /** @deprecated No longer used in multi-method flow */
    private Long    auditLogId;
}
