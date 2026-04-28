package skylinkers.tn.mediconnectbackend.dto.request;

import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class GoogleRegisterRequest {

    // From Google — pre-filled, email not changeable
    @NotBlank @Email
    private String email;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String pictureUrl;
    @NotBlank
    private String googleId;

    // User selects role
    @NotBlank
    private String role; // "PATIENT" or "DOCTOR"

    // Optional password — if null/blank, user can only login via Google
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    // Common optional fields
    private String    phone;
    private String    address;

    // Patient-specific
    private LocalDate dateOfBirth;
    private Gender    gender;

    // Doctor-specific
    private String       rppsNumber;
    private Specialization specialization;
    private String       licenseNumber;
    private Integer      consultationDuration;
    private BigDecimal   consultationFee;
    private String       officeAddress;
}
