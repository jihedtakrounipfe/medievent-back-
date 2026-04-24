package skylinkers.tn.mediconnectbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Used by POST /api/doctors — requires admin approval before account is APPROVED. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDoctorRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String keycloakId;

    @NotBlank @Size(max = 100)
    private String firstName;

    @NotBlank @Size(max = 100)
    private String lastName;

    /** 11-digit RPPS number — validated format then verified manually by admin. */
    @NotBlank
    @Pattern(regexp = "^[0-9]{11}$", message = "RPPS number must be exactly 11 digits")
    @JsonProperty("rppsNumber")
    private String rppsNumber;

    @NotNull
    @JsonProperty("specialization")
    private Specialization specialization;

    @Size(max = 20)
    @Pattern(regexp = "^\\+216\\d{8}$", message = "Numéro de téléphone invalide. Format attendu: +216XXXXXXXX")
    private String phone;

    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 500)
    private String address;

    @Size(max = 500)
    private String officeAddress;

    @Size(max = 50)
    private String licenseNumber;

    @Min(10) @Max(120)
    private Integer consultationDuration;

    @DecimalMin("0.000") @DecimalMax("999999.999")
    private BigDecimal consultationFee;

    @Size(max = 500)
    private String profilePicture;

    private String recaptchaToken;
}


