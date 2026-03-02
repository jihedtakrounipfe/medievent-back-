package skylinkers.tn.mediconnectbackend.dto.request;

import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/** Used by POST /api/doctors — requires admin approval before account is APPROVED. */
@Data
public class CreateDoctorRequest {

    @NotBlank @Email
    private String email;

    @NotBlank
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
    private String phone;

    @Size(max = 500)
    private String officeAddress;

    @Size(max = 50)
    private String licenseNumber;

    @Min(10) @Max(120)
    private Integer consultationDuration;

    @DecimalMin("0.000") @DecimalMax("999999.999")
    private BigDecimal consultationFee;

    public @NotBlank @Email String getEmail() {
        return email;
    }

    public void setEmail(@NotBlank @Email String email) {
        this.email = email;
    }

    public @NotBlank String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(@NotBlank String keycloakId) {
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

    public @Size(max = 500) String getOfficeAddress() {
        return officeAddress;
    }

    public void setOfficeAddress(@Size(max = 500) String officeAddress) {
        this.officeAddress = officeAddress;
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

