package skylinkers.tn.mediconnectbackend.service.risk;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.AuditLog;
import skylinkers.tn.mediconnectbackend.dto.request.RiskScoreRequest;
import skylinkers.tn.mediconnectbackend.dto.response.RiskScoreResponse;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AuditLogRepository;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorDecisionService {

    private final RiskFeatureBuilder riskFeatureBuilder;
    private final RiskScoringClient riskScoringClient;
    private final AuditLogRepository auditLogRepository;

    @Transactional
    public TwoFaDecision evaluate(LoginContext ctx) {
        RiskScoreRequest req = riskFeatureBuilder.build(ctx);
        RiskScoreResponse resp = riskScoringClient.score(req);

        AuditLog log = AuditLog.builder()
                .user(ctx.user())
                .action("AUTH_LOGIN")
                .ipAddress(ctx.ipAddress())
                .userAgent(ctx.userAgent())
                .timestamp(LocalDateTime.now())
                .success(true)
                .userEmail(ctx.user().getEmail())
                .riskScore(resp.riskScore())
                .twofaDecision(resp.decision())
                .modelVersion(resp.modelVersion())
                .build();

        AuditLog saved = auditLogRepository.save(log);

        return new TwoFaDecision(saved.getId(), resp.decision(), resp.riskScore());
    }

    @Transactional
    public void recordOutcome(Long auditLogId, String outcome) {
        int label = "FAILED".equals(outcome) || "FLAGGED".equals(outcome) ? 1 : 0;
        auditLogRepository.updateFeedback(auditLogId, outcome, label);
    }
}
