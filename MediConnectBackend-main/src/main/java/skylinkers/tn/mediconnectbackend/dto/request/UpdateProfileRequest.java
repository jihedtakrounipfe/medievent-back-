package skylinkers.tn.mediconnectbackend.dto.request;

import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Shared profile update — used by PATCH /api/patients/me and /api/doctors/me */
@Data
public class UpdateProfileRequest {

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String address;

    private LocalDate dateOfBirth;

    private Gender gender;

    @Size(max = 5)
    private String bloodType;

    private String allergies;

    private String emergencyContact;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 20)
    private String emergencyContactPhone;

    @Size(max = 500)
    private String profilePicture;

    private Specialization specialization;

    @Size(max = 50)
    private String licenseNumber;

    private Integer consultationDuration;

    private BigDecimal consultationFee;

    @Size(max = 500)
    private String officeAddress;

    public @Size(max = 100) String getFirstName() {
        return firstName;
    }

    public void setFirstName(@Size(max = 100) String firstName) {
        this.firstName = firstName;
    }

    public @Size(max = 100) String getLastName() {
        return lastName;
    }

    public void setLastName(@Size(max = 100) String lastName) {
        this.lastName = lastName;
    }

    public @Size(max = 20) String getPhone() {
        return phone;
    }

    public void setPhone(@Size(max = 20) String phone) {
        this.phone = phone;
    }

    public @Size(max = 500) String getAddress() {
        return address;
    }

    public void setAddress(@Size(max = 500) String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public @Size(max = 500) String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(@Size(max = 500) String profilePicture) {
        this.profilePicture = profilePicture;
    }
}

