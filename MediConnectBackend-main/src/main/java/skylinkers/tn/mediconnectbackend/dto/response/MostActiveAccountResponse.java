package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.enums.UserType;

import java.time.LocalDateTime;

public record MostActiveAccountResponse(
        Long userId,
        String userEmail,
        String firstName,
        String lastName,
        UserType userType,
        Long actionCount,
        Long successCount,
        Long failedCount,
        LocalDateTime lastActivityAt
) {}
