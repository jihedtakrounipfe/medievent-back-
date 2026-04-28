package skylinkers.tn.mediconnectbackend.service.risk;

public record TwoFaDecision(Long auditLogId, String decision, int riskScore) {}
