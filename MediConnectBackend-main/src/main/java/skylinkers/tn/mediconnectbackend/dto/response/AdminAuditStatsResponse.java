package skylinkers.tn.mediconnectbackend.dto.response;

import java.util.List;

public record AdminAuditStatsResponse(
        Long totalUsers,
        Long totalPatients,
        Long totalDoctors,
        Long activeUsers,
        Long totalAuditLogs,
        Long successfulAuditLogs,
        Long failedAuditLogs,
        List<AuditActionStatResponse> topActions,
        List<AuditCategoryStatResponse> categoryBreakdown,
        List<MostActiveAccountResponse> mostActiveAccounts
) {}
