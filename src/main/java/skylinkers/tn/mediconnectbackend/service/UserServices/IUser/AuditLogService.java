package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

import skylinkers.tn.mediconnectbackend.dto.response.AuditLogResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * SOLID — ISP: Dedicated interface for audit concerns only.
 *              Any service that needs to log calls this — they don't
 *              need to know about patient or doctor logic.
 */
public interface AuditLogService {

    /**
     * Append an audit entry. Called by all service methods that
     * mutate state or perform security-sensitive reads.
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

    Page<AuditLogResponse> getLogsForUser(Long userId, Pageable pageable);

    /** Security: check if a user has exceeded the brute-force threshold in the last 15 min. */
    boolean isAccountBruteForced(Long userId);
}
