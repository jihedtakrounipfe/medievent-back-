package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import skylinkers.tn.mediconnectbackend.dto.response.AuditLogResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.AuditLog;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AuditLogRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private static final int  BRUTE_FORCE_THRESHOLD = 5;
    private static final long BRUTE_FORCE_WINDOW_MIN = 15;

    private final AuditLogRepository auditLogRepository;

    /**
     * @Async + REQUIRES_NEW: audit logging never blocks the business transaction
     * and never causes a rollback in the caller if it fails.
     */
    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AppUser user, String action, String ip, String userAgent,
                    boolean success, String details) {
        AuditLog entry = AuditLog.builder()
                .user(user)
                .action(action)
                .ipAddress(ip)
                .userAgent(userAgent)
                .success(success)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUser() != null ? log.getUser().getId() : null)
                        .action(log.getAction())
                        .ipAddress(log.getIpAddress())
                        .timestamp(log.getTimestamp())
                        .success(log.isSuccess())
                        .details(log.getDetails())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountBruteForced(Long userId) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(BRUTE_FORCE_WINDOW_MIN);
        long failedAttempts = auditLogRepository.countFailedLoginsSince(userId, windowStart);
        return failedAttempts >= BRUTE_FORCE_THRESHOLD;
    }
}
