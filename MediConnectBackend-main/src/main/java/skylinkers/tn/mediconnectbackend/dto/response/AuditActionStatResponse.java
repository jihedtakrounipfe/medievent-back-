package skylinkers.tn.mediconnectbackend.dto.response;

public record AuditActionStatResponse(
        String action,
        Long count,
        Long successCount,
        Long failedCount
) {}
