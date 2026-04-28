package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lightweight projection used in search results — no sensitive fields.
 * Full profile is fetched via /patients/{id} or /doctors/{id}.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppUserResponse {

    private Long          id;
    private String        firstName;
    private String        lastName;
    private String        email;
    private String        phone;
    private UserType      userType;
    private Boolean       isActive;
    private String        profilePicture;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Doctor-specific — null for patients
    private String     specialization;
    private String     specializationLabel;
    private String     officeAddress;
    private String     verificationStatus;
    private Boolean    isVerified;
    private String     rppsNumber;
    private BigDecimal consultationFee;

    // Patient-specific — null for doctors
    private String    bloodType;
    private Boolean   biometricEnrolled;
    private String    gender;
    private LocalDate dateOfBirth;
    private Integer   age;

    // Security
    private Boolean twoFactorEnabled;
    private Boolean faceEnabled;
    private Boolean faceEnrolled;
}
