package skylinkers.tn.mediconnectbackend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record UserAuditSummaryResponse(
        Long totalLogs,
        Long successfulLogs,
        Long failedLogs,
        LocalDateTime lastActivityAt,
        List<AuditActionStatResponse> topActions
) {}
