package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DoctorResponse {

    private Long id;
    private UserType userType;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String rppsNumber;
    private Specialization specialization;
    private String licenseNumber;
    private Integer consultationDuration;
    private String officeAddress;
    private VerificationStatus verificationStatus;
    private boolean googleCalendarLinked;
    private Long clinicId;
    private BigDecimal consultationFee;
    private BigDecimal rating;
    private Boolean isVerified;
    private boolean isActive;
    private String profilePicture;
    private LocalDateTime createdAt;
    private java.util.Set<String> interests;
}

