package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import skylinkers.tn.mediconnectbackend.dto.response.AdminAuditStatsResponse;
import skylinkers.tn.mediconnectbackend.dto.response.AuditLogResponse;
import skylinkers.tn.mediconnectbackend.dto.response.UserAuditSummaryResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.AuditLog;
import skylinkers.tn.mediconnectbackend.entities.enums.AuditAction;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AuditLogRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private static final int BRUTE_FORCE_THRESHOLD = 5;
    private static final long BRUTE_FORCE_WINDOW_MIN = 15;

    private final AuditLogRepository auditLogRepository;
    private final AppUserRepository appUserRepository;
    private final TransactionTemplate requiresNewTx;

    public AuditLogServiceImpl(AuditLogRepository auditLogRepository,
                               AppUserRepository appUserRepository,
                               PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        this.appUserRepository = appUserRepository;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(
                org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void log(AppUser user, String action, String ip, String userAgent,
                    boolean success, String details) {
        persistAuditRow(
                action,
                resolveCategory(action),
                truncate(details),
                ip != null ? ip : extractIp(),
                user != null ? user.getKeycloakId() : null,
                null,
                null,
                success,
                null,
                null,
                user != null ? user.getId() : null,
                userAgent != null ? userAgent : extractUserAgent(),
                user != null ? user.getEmail() : null
        );
    }

    @Override
    public void logAuth(AuditAction action, String email, String keycloakId,
                        HttpServletRequest request, boolean success, String failureReason) {
        AppUser user = email != null ? appUserRepository.findByEmail(email).orElse(null) : null;
        persistAuditRow(
                action.name(),
                "AUTH",
                truncate(failureReason),
                resolveClientIp(request),
                keycloakId != null ? keycloakId : user != null ? user.getKeycloakId() : null,
                null,
                null,
                success,
                null,
                null,
                user != null ? user.getId() : null,
                resolveUserAgent(request),
                email
        );
    }

    @Override
    public void logProfileChange(AuditAction action, AppUser user,
                                 String oldValueJson, String newValueJson,
                                 HttpServletRequest request) {
        HttpServletRequest resolvedRequest = request != null ? request : currentRequest();
        persistAuditRow(
                action.name(),
                "PROFILE",
                truncate(buildProfileDetails(oldValueJson, newValueJson)),
                resolveClientIp(resolvedRequest),
                user != null ? user.getKeycloakId() : null,
                null,
                null,
                true,
                null,
                null,
                user != null ? user.getId() : null,
                resolveUserAgent(resolvedRequest),
                user != null ? user.getEmail() : null
        );
    }

    @Override
    public void logAdminAction(AuditAction action, AppUser admin, AppUser targetUser,
                               String description, HttpServletRequest request) {
        String details = "target=" + (targetUser != null ? targetUser.getEmail() : "unknown")
                + (description != null ? " | " + description : "");
        persistAuditRow(
                action.name(),
                "ADMIN",
                truncate(details),
                resolveClientIp(request),
                admin != null ? admin.getKeycloakId() : null,
                null,
                null,
                true,
                null,
                null,
                admin != null ? admin.getId() : null,
                resolveUserAgent(request),
                admin != null ? admin.getEmail() : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogsForUser(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable)
                .map(entry -> AuditLogResponse.builder()
                        .id(entry.getId())
                        .userId(entry.getUser() != null ? entry.getUser().getId() : null)
                        .userEmail(entry.getUserEmail())
                        .keycloakId(entry.getKeycloakId())
                        .action(entry.getAction())
                        .category(entry.getCategory())
                        .ipAddress(entry.getIpAddress())
                        .userAgent(entry.getUserAgent())
                        .timestamp(entry.getTimestamp())
                        .success(entry.isSuccess())
                        .details(entry.getDetails())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public UserAuditSummaryResponse getSummaryForUser(Long userId) {
        PageRequest topActionsPage = PageRequest.of(0, 5);
        Long totalLogs = auditLogRepository.countByUserId(userId);
        Long successfulLogs = auditLogRepository.countByUserIdAndSuccessTrue(userId);
        Long failedLogs = auditLogRepository.countByUserIdAndSuccessFalse(userId);
        var latestEntry = auditLogRepository.findFirstByUserIdOrderByTimestampDesc(userId).orElse(null);

        return new UserAuditSummaryResponse(
                totalLogs,
                successfulLogs,
                failedLogs,
                latestEntry != null ? latestEntry.getTimestamp() : null,
                auditLogRepository.findTopActionStatsForUser(userId, topActionsPage)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAuditStatsResponse getAdminAuditStats() {
        long activeUsers = appUserRepository.countByUserTypeAndIsActiveTrue(UserType.PATIENT)
                + appUserRepository.countByUserTypeAndIsActiveTrue(UserType.DOCTOR)
                + appUserRepository.countByUserTypeAndIsActiveTrue(UserType.ADMINISTRATOR);

        return new AdminAuditStatsResponse(
                appUserRepository.count(),
                appUserRepository.countByUserType(UserType.PATIENT),
                appUserRepository.countByUserType(UserType.DOCTOR),
                activeUsers,
                auditLogRepository.count(),
                auditLogRepository.countBySuccessTrue(),
                auditLogRepository.countBySuccessFalse(),
                auditLogRepository.findTopActionStats(PageRequest.of(0, 6)),
                auditLogRepository.findCategoryBreakdown(PageRequest.of(0, 6)),
                auditLogRepository.findMostActiveAccounts(PageRequest.of(0, 5))
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAccountBruteForced(Long userId) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(BRUTE_FORCE_WINDOW_MIN);
        long failedAttempts = auditLogRepository.countFailedLoginsSince(userId, windowStart);
        return failedAttempts >= BRUTE_FORCE_THRESHOLD;
    }

    private void persistAuditRow(String action,
                                 String category,
                                 String details,
                                 String ipAddress,
                                 String keycloakId,
                                 String modelVersion,
                                 Integer riskScore,
                                 boolean success,
                                 String twofaDecision,
                                 String twofaOutcome,
                                 Long userId,
                                 String userAgent,
                                 String userEmail) {
        try {
            final Long[] savedIdHolder = new Long[1];
            requiresNewTx.executeWithoutResult(status -> {
                AppUser managedUser = resolveAuditUser(userId, action);
                AuditLog saved = auditLogRepository.saveAndFlush(AuditLog.builder()
                        .user(managedUser)
                        .action(action)
                        .ipAddress(ipAddress)
                        .userAgent(userAgent)
                        .timestamp(LocalDateTime.now())
                        .success(success)
                        .details(details)
                        .userEmail(userEmail)
                        .keycloakId(keycloakId)
                        .category(category)
                        .riskScore(riskScore)
                        .twofaDecision(twofaDecision)
                        .twofaOutcome(twofaOutcome)
                        .modelVersion(modelVersion)
                        .build());
                savedIdHolder[0] = saved.getId();

                log.info("[AUDIT] Persisted audit log id={}, action={}, requestedUserId={}, linkedUserId={}, email={}",
                        saved.getId(),
                        action,
                        userId,
                        saved.getUser() != null ? saved.getUser().getId() : null,
                        userEmail);
            });

            if (savedIdHolder[0] != null) {
                boolean visibleAfterCommit = auditLogRepository.existsById(savedIdHolder[0]);
                log.info("[AUDIT] Post-commit visibility id={}, visibleAfterCommit={}",
                        savedIdHolder[0], visibleAfterCommit);
            }
        } catch (Exception e) {
            log.error("[AUDIT] Failed to persist audit log - action={}, userId={}, email={}, ip={}",
                    action, userId, userEmail, ipAddress, e);
        }
    }

    private AppUser resolveAuditUser(Long userId, String action) {
        if (userId == null) {
            return null;
        }

        AppUser managedUser = appUserRepository.findById(userId).orElse(null);
        if (managedUser == null) {
            log.warn("[AUDIT] User {} is not visible in the audit transaction for action={}; storing row without FK link.",
                    userId, action);
        }
        return managedUser;
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return extractIp();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : extractUserAgent();
    }

    private HttpServletRequest currentRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String extractIp() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent() {
        HttpServletRequest request = currentRequest();
        return request != null ? request.getHeader("User-Agent") : null;
    }

    private String resolveCategory(String action) {
        if (action == null) {
            return "SYSTEM";
        }
        if (action.startsWith("AUTH_") || action.startsWith("LOGIN") || action.startsWith("LOGOUT")) {
            return "AUTH";
        }
        if (action.startsWith("PROFILE_")) {
            return "PROFILE";
        }
        if (action.startsWith("FACE_")) {
            return "AUTH";
        }
        if (action.startsWith("DOCTOR_") || action.startsWith("USER_") || action.startsWith("ACCOUNT_")) {
            return "ADMIN";
        }
        if (action.startsWith("BIOMETRIC_")) {
            return "MEDICAL";
        }
        return "SYSTEM";
    }

    private String buildProfileDetails(String oldValueJson, String newValueJson) {
        if ((oldValueJson == null || oldValueJson.isBlank()) && (newValueJson == null || newValueJson.isBlank())) {
            return null;
        }
        if (oldValueJson == null || oldValueJson.isBlank()) {
            return "new=" + newValueJson;
        }
        if (newValueJson == null || newValueJson.isBlank()) {
            return "old=" + oldValueJson;
        }
        return "old=" + oldValueJson + " | new=" + newValueJson;
    }

    private String truncate(String value) {
        if (value == null || value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }
}
