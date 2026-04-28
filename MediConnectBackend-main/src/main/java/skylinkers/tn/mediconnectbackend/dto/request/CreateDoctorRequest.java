package skylinkers.tn.mediconnectbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Used by POST /api/doctors — requires admin approval before account is APPROVED. */
@Data
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
    private String rppsNumber;

    @NotNull
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

    public  String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
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

    public @NotBlank @Pattern(regexp = "^[0-9]{11}$", message = "RPPS number must be exactly 11 digits") String getRppsNumber() {
        return rppsNumber;
    }

    public void setRppsNumber(@NotBlank @Pattern(regexp = "^[0-9]{11}$", message = "RPPS number must be exactly 11 digits") String rppsNumber) {
        this.rppsNumber = rppsNumber;
    }

    public @NotNull Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(@NotNull Specialization specialization) {
        this.specialization = specialization;
    }

    public @Size(max = 20) String getPhone() {
        return phone;
    }

    public void setPhone(@Size(max = 20) String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public @Size(max = 500) String getAddress() {
        return address;
    }

    public void setAddress(@Size(max = 500) String address) {
        this.address = address;
    }

    public @Size(max = 500) String getOfficeAddress() {
        return officeAddress;
    }

    public void setOfficeAddress(@Size(max = 500) String officeAddress) {
        this.officeAddress = officeAddress;
    }

    public @Size(max = 500) String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(@Size(max = 500) String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public @Size(max = 50) String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(@Size(max = 50) String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public @Min(10) @Max(120) Integer getConsultationDuration() {
        return consultationDuration;
    }

    public void setConsultationDuration(@Min(10) @Max(120) Integer consultationDuration) {
        this.consultationDuration = consultationDuration;
    }

    public @DecimalMin("0.000") @DecimalMax("999999.999") BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(@DecimalMin("0.000") @DecimalMax("999999.999") BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }


}

