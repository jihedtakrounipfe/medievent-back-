package skylinkers.tn.mediconnectbackend.entities;

import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.security.converter.AES256Converter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Subclass of AppUser representing a patient.
 * Discriminator value: "PATIENT"
 *
 * Sensitive field: socialSecurityNum is transparently encrypted at rest
 * via AES256Converter (AES-256-GCM, IV prepended). The plaintext never
 * reaches the JDBC driver.
 *
 * SOLID — OCP: Adding a new patient-specific feature (e.g. insuranceId)
 *              extends this class without touching AppUser.
 */
@Entity
@DiscriminatorValue("PATIENT")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Patient extends AppUser {

    @Column(name = "date_of_birth", nullable = true)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Gender gender;

    /**
     * French social security number — AES-256-GCM encrypted at rest.
     * RGPD Article 9: treated as sensitive personal data.
     * Only the AES256Converter touches the raw value.
     */
    @Convert(converter = AES256Converter.class)
    @Column(name = "social_security_num", unique = true, length = 400) // length accounts for base64 + IV overhead
    private String socialSecurityNum;

    @Column(name = "blood_type", length = 5)
    private String bloodType;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "emergency_contact", length = 255)
    private String emergencyContact;

    @Column(length = 500)
    private String address;

    /**
     * True once the patient has completed biometric enrollment.
     * Used to gate the facial-recognition MFA flow in Keycloak.
     */
    @Column(name = "biometric_enrolled", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean biometricEnrolled = false;

    /** True once the patient's Google Calendar has been authorized. */
    @Column(name = "google_calendar_linked", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean googleCalendarLinked = false;

    /**
     * ML-computed probability that this patient will miss their next appointment.
     * Range: 0.0 (reliable) to 1.0 (high risk of no-show).
     * Updated asynchronously by the analytics service.
     */
    @Column(name = "no_show_score")
    private Double noShowScore;

    /** FK to the subscription plan chosen by this patient. */
    @Column(name = "patient_plan_id", length = 36)
    private String patientPlanId;

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

    public String getSocialSecurityNum() {
        return socialSecurityNum;
    }

    public void setSocialSecurityNum(String socialSecurityNum) {
        this.socialSecurityNum = socialSecurityNum;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isBiometricEnrolled() {
        return biometricEnrolled;
    }

    public void setBiometricEnrolled(boolean biometricEnrolled) {
        this.biometricEnrolled = biometricEnrolled;
    }

    public boolean isGoogleCalendarLinked() {
        return googleCalendarLinked;
    }

    public void setGoogleCalendarLinked(boolean googleCalendarLinked) {
        this.googleCalendarLinked = googleCalendarLinked;
    }

    public Double getNoShowScore() {
        return noShowScore;
    }

    public void setNoShowScore(Double noShowScore) {
        this.noShowScore = noShowScore;
    }

    public String getPatientPlanId() {
        return patientPlanId;
    }

    public void setPatientPlanId(String patientPlanId) {
        this.patientPlanId = patientPlanId;
    }
}
