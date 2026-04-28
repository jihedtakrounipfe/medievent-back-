package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.Builder;
import lombok.Data;
import skylinkers.tn.mediconnectbackend.entities.enums.SubVerificationStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class StudentVerificationResponseDTO {
    private Long id;
    private Long userId;
    private boolean requestFound;
    private String fullName;
    private String universityName;
    private String studentIdNumber;
    private String facultyEmail;
    private SubVerificationStatus status;
    private Integer progressPercentage;
    private String progressStep;
    private String progressMessage;
    private String nextAction;
    private String nextPagePath;
    private String rejectionReason;
    private Double confidenceScore;
    private LocalDateTime createdAt;
    private LocalDateTime verifiedAt;
    private LocalDateTime expiresAt;
}