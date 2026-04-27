package skylinkers.tn.mediconnectbackend.entities;

import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract root of the SINGLE_TABLE JPA inheritance hierarchy.
 *
 * All subtypes (Patient, Doctor, Administrator) map to the single
 * `users` table. The `user_type` column acts as the JPA discriminator.
 *
 * No passwordHash: credentials are owned entirely by Keycloak.
 * The keycloakId field is the bridge between the IAM layer and domain layer.
 *
 * SOLID — SRP: This class owns only identity + shared lifecycle fields.
 *              Role-specific data lives exclusively in subclasses.
 */
@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_keycloak_id", columnList = "keycloak_id", unique = true),
                @Index(name = "idx_users_email",       columnList = "email",       unique = true),
                @Index(name = "idx_users_user_type",   columnList = "user_type")
        }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "user_type", discriminatorType = DiscriminatorType.STRING, length = 30)
@EntityListeners(AuditingEntityListener.class)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * UUID issued by Keycloak. Acts as the stable bridge between
     * Spring domain objects and the Keycloak IAM realm.
     * Never expose this in public-facing responses.
     */
    @Column(name = "keycloak_id", unique = true, nullable = false, length = 70)
    private String keycloakId = "TEMP";

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    /**
     * Mirrors the JPA discriminator value as a typed enum.
     * insertable = false / updatable = false: Hibernate writes
     * the raw string via the discriminator column, not this field.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_type", insertable = false, updatable = false)
    private UserType userType;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "is_verified", nullable = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isVerified = Boolean.FALSE;

    @Column(name = "date_of_birth", nullable = true)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = true, length = 30)
    private Gender gender;

    @Column(name = "address", nullable = true, length = 500)
    private String address;

    @Column(name = "emergency_contact", nullable = true, length = 255)
    private String emergencyContact;

    /** Swift Object Storage URL for the profile picture. */
    @Column(name = "profile_picture", length = 500)
    private String profilePicture;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "interest")
    @Builder.Default
    private Set<String> interests = new HashSet<>();

    // ── Relations to IAM support entities ──────────────────────────

    /** Google OAuth2 tokens — lazy, may be null if Google not linked. */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OAuthToken oauthToken;

    /** Biometric embedding — lazy, null until enrollment. */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BiometricData biometricData;

    /** Full audit trail of this user's actions. */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<AuditLog> auditLogs = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKeycloakId() {
        return keycloakId;
    }

    public void setKeycloakId(String keycloakId) {
        this.keycloakId = keycloakId;
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

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<String> getInterests() {
        return interests;
    }

    public void setInterests(Set<String> interests) {
        this.interests = interests;
    }

    public OAuthToken getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(OAuthToken oauthToken) {
        this.oauthToken = oauthToken;
    }

    public BiometricData getBiometricData() {
        return biometricData;
    }

    public void setBiometricData(BiometricData biometricData) {
        this.biometricData = biometricData;
    }

    public List<AuditLog> getAuditLogs() {
        return auditLogs;
    }

    public void setAuditLogs(List<AuditLog> auditLogs) {
        this.auditLogs = auditLogs;
    }
}
