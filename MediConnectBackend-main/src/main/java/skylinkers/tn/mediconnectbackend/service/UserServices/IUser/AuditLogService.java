package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

import jakarta.servlet.http.HttpServletRequest;
import skylinkers.tn.mediconnectbackend.dto.response.AdminAuditStatsResponse;
import skylinkers.tn.mediconnectbackend.dto.response.AuditLogResponse;
import skylinkers.tn.mediconnectbackend.dto.response.UserAuditSummaryResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * SOLID — ISP: Dedicated interface for audit concerns only.
 *              Any service that needs to log calls this — they don't
 *              need to know about patient or doctor logic.
 */
public interface AuditLogService {

    /**
     * Legacy method — kept for backward compatibility with existing callers.
     * Appends an audit entry with minimal context (no HttpServletRequest).
     *
     * @param user      the actor (may be null for anonymised/system actions)
     * @param action    VERB_NOUN identifier (e.g. "AUTH_LOGIN")
     * @param ip        source IP address
     * @param userAgent HTTP user-agent string
     * @param success   false for failed attempts
     * @param details   optional context message
     */
    void log(AppUser user, String action, String ip, String userAgent,
             boolean success, String details);

    /**
     * Comprehensive auth event logging with full request context.
     * Automatically parses device type, browser, OS from User-Agent.
     *
     * @param action         structured action type (e.g. LOGIN_SUCCESS, LOGIN_FAILED)
     * @param email          email of the actor (required — available even for failed logins)
     * @param keycloakId     Keycloak UUID (may be null for failed logins)
     * @param request        the HTTP request (for IP, User-Agent, URL, method)
     * @param success        false for failed attempts
     * @param failureReason  human-readable failure cause (null on success)
     */
    void logAuth(AuditAction action, String email, String keycloakId,
                 HttpServletRequest request, boolean success, String failureReason);

    /**
     * Log a profile change with old/new values as JSON for diff tracking.
     *
     * @param action        e.g. PROFILE_UPDATED, PROFILE_PICTURE_CHANGED
     * @param user          the user whose profile changed
     * @param oldValueJson  previous state as JSON (may be null)
     * @param newValueJson  new state as JSON (may be null)
     * @param request       the HTTP request for context
     */
    void logProfileChange(AuditAction action, AppUser user,
                          String oldValueJson, String newValueJson,
                          HttpServletRequest request);

    /**
     * Log an administrative action performed by an admin on a target user.
     *
     * @param action      e.g. DOCTOR_APPROVED, DOCTOR_REJECTED, USER_DEACTIVATED
     * @param admin       the admin performing the action
     * @param targetUser  the user being acted upon
     * @param description human-readable description (may include reason)
     * @param request     the HTTP request for context
     */
    void logAdminAction(AuditAction action, AppUser admin, AppUser targetUser,
                        String description, HttpServletRequest request);

    Page<AuditLogResponse> getLogsForUser(Long userId, Pageable pageable);

    UserAuditSummaryResponse getSummaryForUser(Long userId);

    AdminAuditStatsResponse getAdminAuditStats();

    /** Security: check if a user has exceeded the brute-force threshold in the last 15 min. */
    boolean isAccountBruteForced(Long userId);
}
