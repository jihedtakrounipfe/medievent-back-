package skylinkers.tn.mediconnectbackend.service.risk;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.dto.request.RiskScoreRequest;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AuditLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RiskFeatureBuilder {

    private final AuditLogRepository auditLogRepo;

    public RiskScoreRequest build(LoginContext ctx) {
        Long userId = Long.parseLong(ctx.userId());
        LocalDateTime now = ctx.timestamp();

        int hour = now.getHour();
        int isWeekend = now.getDayOfWeek().getValue() >= 6 ? 1 : 0;

        int knownDevice = auditLogRepo.existsByUserIdAndUserAgentAndSuccessTrueAndTimestampAfter(
                userId, ctx.userAgent(), now.minusDays(30)) ? 1 : 0;

        int knownIp = auditLogRepo.existsByUserIdAndIpAddressAndSuccessTrueAndTimestampAfter(
                userId, ctx.ipAddress(), now.minusDays(30)) ? 1 : 0;

        long failedAttempts = auditLogRepo.countByUserIdAndSuccessFalseAndTimestampAfter(
                userId, now.minusMinutes(15));

        Double avgLoginHour = ctx.user().getAvgLoginHour();
        double timeAnomaly = avgLoginHour != null
                ? Math.min(Math.abs(hour - avgLoginHour) / 12.0, 1.0)
                : 0.3;

        double userBaseRisk = ctx.user().isTwoFactorEnforced() ? 0.2 : 0.0;

        return new RiskScoreRequest(
                ctx.userId(),
                hour,
                isWeekend,
                knownDevice,
                knownIp,
                (int) failedAttempts,
                timeAnomaly,
                userBaseRisk
        );
    }
}
