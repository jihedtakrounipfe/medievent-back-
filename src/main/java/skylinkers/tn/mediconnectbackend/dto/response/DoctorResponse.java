package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class DoctorResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String rppsNumber;
    private Specialization specialization;
    private String licenseNumber;
    private int consultationDuration;
    private String officeAddress;
    private VerificationStatus verificationStatus;
    private boolean googleCalendarLinked;
    private Long clinicId;
    private BigDecimal consultationFee;
    private BigDecimal rating;
    private boolean isVerified;
    private boolean isActive;
    private String profilePicture;
    private LocalDateTime createdAt;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRppsNumber() {
        return rppsNumber;
    }

    public void setRppsNumber(String rppsNumber) {
        this.rppsNumber = rppsNumber;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public int getConsultationDuration() {
        return consultationDuration;
    }

    public void setConsultationDuration(int consultationDuration) {
        this.consultationDuration = consultationDuration;
    }

    public String getOfficeAddress() {
        return officeAddress;
    }

    public void setOfficeAddress(String officeAddress) {
        this.officeAddress = officeAddress;
    }

    public VerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(VerificationStatus verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public boolean isGoogleCalendarLinked() {
        return googleCalendarLinked;
    }

    public void setGoogleCalendarLinked(boolean googleCalendarLinked) {
        this.googleCalendarLinked = googleCalendarLinked;
    }

    public Long getClinicId() {
        return clinicId;
    }

    public void setClinicId(Long clinicId) {
        this.clinicId = clinicId;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(BigDecimal consultationFee) {
        this.consultationFee = consultationFee;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

