package skylinkers.tn.mediconnectbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogResponse {
    private Long id;

    // WHO
    private Long userId;
    private String userEmail;
    private String userType;
    private String keycloakId;

    // WHAT
    private String action;
    private String category;
    private String description;
    private String entityType;
    private String entityId;

    // WHERE
    private String ipAddress;
    private String userAgent;
    private String deviceType;
    private String browser;
    private String os;
    private String requestUrl;
    private String httpMethod;

    // WHEN
    private LocalDateTime timestamp;

    // OUTCOME
    private boolean success;
    private String failureReason;
    private String details;

    // SESSION
    private String sessionId;
}
