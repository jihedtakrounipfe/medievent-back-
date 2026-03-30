package skylinkers.tn.mediconnectbackend.entities;

import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Subclass of AppUser representing any medical professional
 * (general practitioner or specialist). Discriminator value: "DOCTOR"
 *
 * The `specialization` field differentiates GPs (GENERAL_PRACTICE)
 * from specialists, eliminating the need for two separate subclasses
 * and keeping the Spring Security RBAC roles orthogonal to specialization.
 *
 * SOLID — SRP: Doctor owns only professional identity and practice
 *              metadata. Agenda / Appointments live in their own module.
 */
@Entity
@DiscriminatorValue("DOCTOR")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor extends AppUser {

    /**
     * Répertoire Partagé des Professionnels de Santé.
     * Mandatory, verified by admin before account is APPROVED.
     */
    @Column(name = "rpps_number", unique = true, length = 11,columnDefinition = "VARCHAR(11) DEFAULT NULL")
    private String rppsNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 30)
    private Specialization specialization;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    /** Default slot duration in minutes; drives availability grid. */
    @Column(name = "consultation_duration", nullable = true)
    private Integer consultationDuration;

    @Column(name = "office_address", length = 500)
    private String officeAddress;

    /**
     * Set to APPROVED by admin after RPPS verification.
     * A doctor with PENDING or REJECTED status cannot receive appointments.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "verification_status", nullable = true, length = 20)
    private VerificationStatus verificationStatus;

    /** True once Google Calendar OAuth2 flow is completed. */
    @Column(name = "google_calendar_linked", nullable = false,columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean googleCalendarLinked = false;

    /** FK to the Clinic entity in the clinic module (nullable for independent practitioners). */
    @Column(name = "clinic_id")
    private Long clinicId;

    @Column(name = "consultation_fee", precision = 10, scale = 3)
    private BigDecimal consultationFee;

    /** Aggregate rating computed from patient feedback. */
    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    /** FK to the subscription plan chosen by this doctor. */
    @Column(name = "doctor_plan_id", length = 36)
    private String doctorPlanId;

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

    public Integer getConsultationDuration() {
        return consultationDuration;
    }

    public void setConsultationDuration(Integer consultationDuration) {
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

    public String getDoctorPlanId() {
        return doctorPlanId;
    }

    public void setDoctorPlanId(String doctorPlanId) {
        this.doctorPlanId = doctorPlanId;
    }
}
