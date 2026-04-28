package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Immutable record of every significant action performed by or on a user.
 *
 * HDS + RGPD compliance: All authentication events, data modifications,
 * and administrative actions MUST be logged here with full context.
 *
 * Design note: AuditLog rows are NEVER updated or deleted (except
 * via RGPD right-to-erasure, which anonymizes rather than hard-deletes).
 * No @LastModifiedDate — the entity is append-only.
 *
 * The `action` field follows a VERB_NOUN convention:
 *   AUTH_LOGIN, AUTH_LOGOUT, AUTH_FAILED, AUTH_2FA_ENABLED,
 *   PROFILE_UPDATE, BIOMETRIC_ENROLL, BIOMETRIC_REVOKE,
 *   OAUTH_LINKED, OAUTH_REVOKED, ACCOUNT_DEACTIVATED, etc.
 */
@Entity
@Table(
        name = "mc_audit_log",
        indexes = {
                @Index(name = "idx_audit_user_id",   columnList = "user_id"),
                @Index(name = "idx_audit_timestamp",  columnList = "timestamp"),
                @Index(name = "idx_audit_action",     columnList = "action")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private AppUser user;

    /**
     * Verb_Noun action identifier. Max 60 chars.
     * Examples: AUTH_LOGIN, PROFILE_UPDATE, BIOMETRIC_ENROLL
     */
    @Column(nullable = false, length = 60)
    private String action;

    /** IPv4 or IPv6 source address of the request. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /** Browser/client user-agent string — nullable for M2M calls. */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    /** FALSE for failed attempts (wrong 2FA, facial mismatch, etc.). */
    @Column(nullable = false)
    private boolean success;

    /** Optional free-text context (e.g., failure reason, changed field name). */
    @Column(length = 2000)
    private String details;

    // ── Denormalised fields (kept for RGPD erasure + audit after user deletion) ──

    @Column(name = "user_email", length = 255)
    private String userEmail;

    @Column(name = "keycloak_id", length = 70)
    private String keycloakId;

    /** Grouping category: AUTH, PROFILE, ADMIN, MEDICAL, APPOINTMENT, SYSTEM */
    @Column(name = "category", length = 30)
    private String category;

    // ── Adaptive 2FA / ML risk scoring ───────────────────────────────────────

    @Column(name = "risk_score")
    private Integer riskScore;

    /** ML decision: FORCED | OPTIONAL | SKIPPED */
    @Column(name = "twofa_decision", length = 10)
    private String twofaDecision;

    /** 2FA challenge outcome: PASSED | FAILED | FLAGGED | NA */
    @Column(name = "twofa_outcome", length = 10)
    private String twofaOutcome;

    @Column(name = "model_version", length = 80)
    private String modelVersion;

    /** Feedback label for ML retraining: 1 = suspicious, 0 = legitimate, null = unresolved */
    @Column(name = "feedback_label")
    private Integer feedbackLabel;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getKeycloakId() { return keycloakId; }
    public void setKeycloakId(String keycloakId) { this.keycloakId = keycloakId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }

    public String getTwofaDecision() { return twofaDecision; }
    public void setTwofaDecision(String twofaDecision) { this.twofaDecision = twofaDecision; }

    public String getTwofaOutcome() { return twofaOutcome; }
    public void setTwofaOutcome(String twofaOutcome) { this.twofaOutcome = twofaOutcome; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public Integer getFeedbackLabel() { return feedbackLabel; }
    public void setFeedbackLabel(Integer feedbackLabel) { this.feedbackLabel = feedbackLabel; }
}
