package skylinkers.tn.mediconnectbackend.dto.request;

import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/** Used by POST /api/patients — triggered after Keycloak registration webhook. */
@Data
public class CreatePatientRequest {

    @NotBlank
    @Email
    private String email;

    private String keycloakId;

    @NotBlank @Size(max = 100)
    private String firstName;

    @NotBlank @Size(max = 100)
    private String lastName;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    private Gender gender;

    @Pattern(regexp = "^[12][0-9]{14}$", message = "Invalid French social security number")
    private String socialSecurityNum;

    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood type")
    private String bloodType;

    @Size(max = 20)
    private String phone;

    private String address;

    private String emergencyContact;

    public @NotBlank @Email String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank @Email String email) {
        this.email = email;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId( String keycloakId) {
        this.keycloakId = keycloakId;
    }

    public @NotBlank @Size(max = 100) String getFirstName() {
        return firstName;
    }

    public void setFirstName(@NotBlank @Size(max = 100) String firstName) {
        this.firstName = firstName;
    }

    public @NotBlank @Size(max = 100) String getLastName() {
        return lastName;
    }

    public void setLastName(@NotBlank @Size(max = 100) String lastName) {
        this.lastName = lastName;
    }

    public @NotNull @Past LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(@NotNull @Past LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public @Pattern(regexp = "^[12][0-9]{14}$", message = "Invalid French social security number") String getSocialSecurityNum() {
        return socialSecurityNum;
    }

    public void setSocialSecurityNum(@Pattern(regexp = "^[12][0-9]{14}$", message = "Invalid French social security number") String socialSecurityNum) {
        this.socialSecurityNum = socialSecurityNum;
    }

    public @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood type") String getBloodType() {
        return bloodType;
    }

    public void setBloodType(@Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Invalid blood type") String bloodType) {
        this.bloodType = bloodType;
    }

    public @Size(max = 20) String getPhone() {
        return phone;
    }

    public void setPhone(@Size(max = 20) String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }
}
