package skylinkers.tn.mediconnectbackend.dto.request;

import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String phone;

    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 5)
    private String bloodType;

    private String allergies;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 20)
    private String emergencyContactPhone;

    @Size(max = 500)
    private String profilePicture;
}

