package skylinkers.tn.mediconnectbackend.service.risk;

import skylinkers.tn.mediconnectbackend.entities.AppUser;

import java.time.LocalDateTime;

public record LoginContext(
        String userId,
        String userAgent,
        String ipAddress,
        LocalDateTime timestamp,
        AppUser user
) {}
