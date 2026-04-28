package skylinkers.tn.mediconnectbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @NotBlank
    @Size(min = 8, max = 100)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    private String keycloakId;

    @NotBlank @Size(max = 100)
    private String firstName;

    @NotBlank @Size(max = 100)
    private String lastName;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    private Gender gender;

    @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Groupe sanguin invalide")
    private String bloodType;

    @Size(max = 20)
    @Pattern(regexp = "^\\+216\\d{8}$", message = "Numéro de téléphone invalide. Format attendu: +216XXXXXXXX")
    private String phone;

    private String address;

    private String allergies;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 20)
    @Pattern(regexp = "^\\+216\\d{8}$", message = "Téléphone du contact d'urgence invalide. Format attendu: +216XXXXXXXX")
    private String emergencyContactPhone;

    @Size(max = 500)
    private String profilePicture;

    @NotBlank(message = "Le captcha est requis")
    private String recaptchaToken;

    public String getRecaptchaToken() {
        return recaptchaToken;
    }

    public void setRecaptchaToken(String recaptchaToken) {
        this.recaptchaToken = recaptchaToken;
    }

    public @NotBlank @Email String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank @Email String email) {
        this.email = email;
    }

    public @NotBlank @Size(min = 8, max = 100) String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank @Size(min = 8, max = 100) String password) {
        this.password = password;
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

    public @Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Groupe sanguin invalide") String getBloodType() {
        return bloodType;
    }

    public void setBloodType(@Pattern(regexp = "^(A|B|AB|O)[+-]$", message = "Groupe sanguin invalide") String bloodType) {
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

    public @Size(max = 500) String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(@Size(max = 500) String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
