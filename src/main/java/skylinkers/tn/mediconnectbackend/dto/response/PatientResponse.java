package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe patient response — never contains keycloakId, socialSecurityNum,
 * or any other sensitive field. Returned to the patient themselves or
 * to an authorized doctor (without SSN in the doctor case).
 */
@Data
@Builder
public class PatientResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private LocalDate dateOfBirth;
    private Gender gender;
    private String bloodType;
    private String allergies;
    private String emergencyContact;
    private String address;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
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

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    private String profilePicture;
    private boolean biometricEnrolled;
    private boolean googleCalendarLinked;
    private Double noShowScore;
    private boolean isActive;
    private LocalDateTime createdAt;
}
