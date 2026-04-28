package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe patient response — never contains keycloakId, socialSecurityNum,
 * or any other sensitive field. Returned to the patient themselves or
 * to an authorized doctor (without SSN in the doctor case).
 */
@Data
@Builder
public class PatientResponse {

    private Long id;
    private UserType userType;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bloodType;
    private String allergies;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String profilePicture;
    private boolean biometricEnrolled;
    private boolean googleCalendarLinked;
    private Double noShowScore;
    private boolean isActive;
    private LocalDateTime createdAt;
    private Boolean isVerified;
    private boolean twoFactorEnabled;
    private boolean faceEnabled;
    private boolean faceEnrolled;
}
