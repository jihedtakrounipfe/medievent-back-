package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.Data;

@Data
public class StudentVerificationRequestDTO {
    private Long userId;
    private String fullName;
    private String universityName;
    private String studentIdNumber;
    private String facultyEmail;
}