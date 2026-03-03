package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;
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

    // Doctor-specific — null for patients
    private String  specialization;
    private String  officeAddress;
    private Boolean isVerified;

    // Patient-specific — null for doctors
    private String  bloodType;
    private Boolean biometricEnrolled;
}