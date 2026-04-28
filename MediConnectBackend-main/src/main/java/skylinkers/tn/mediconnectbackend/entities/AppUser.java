package skylinkers.tn.mediconnectbackend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.entities.enums.Gender;
import skylinkers.tn.mediconnectbackend.security.converter.AES256Converter;
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
import java.util.List;

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

    /**
     * Mirrors Keycloak's OTP credential state.
     * True when the user has opted into 2FA and has a TOTP credential configured.
     * Updated by TwoFactorService on enable/disable — gives a fast local check
     * without a Keycloak Admin API call on every request.
     */
    @Column(name = "two_factor_enabled", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean twoFactorEnabled = false;

    @Column(name = "face_enabled", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean faceEnabled = false;

    @Column(name = "face_enrolled", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean faceEnrolled = false;

    /** True when the user has activated TOTP authenticator-app MFA. */
    @Column(name = "totp_enabled", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean totpEnabled = false;

    /** True when the user has completed TOTP setup (scanned QR + verified first code). */
    @Column(name = "totp_enrolled", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean totpEnrolled = false;

    /**
     * AES-256-GCM encrypted TOTP shared secret (Base32 encoded before encryption).
     * Null until the user completes TOTP enrollment. Cleared on disable.
     */
    @Convert(converter = AES256Converter.class)
    @Column(name = "totp_secret", length = 500)
    private String totpSecret;

    /** Admin-enforced 2FA: user MUST complete 2FA regardless of their own preference. */
    @Column(name = "two_factor_enforced", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 0")
    private boolean twoFactorEnforced = false;

    /** Mean login hour (0–23) — updated nightly by scheduled task for risk feature derivation. */
    @Column(name = "avg_login_hour")
    private Double avgLoginHour;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Relations to IAM support entities ──────────────────────────

    /**
     * Google subject ID — set when the user registers or links their Google account.
     * Null for users who have never used Google Sign-In.
     */
    @Column(name = "google_id", unique = true, length = 255)
    private String googleId;

    /**
     * AES-256-GCM encrypted Keycloak password used for backend token generation
     * during Google Sign-In flows. Never exposed to the user.
     * Kept in sync with the Keycloak password so password_grant always works.
     */
    @Convert(converter = AES256Converter.class)
    @Column(name = "google_internal_password", length = 400)
    private String googleInternalPassword;

    /** Google OAuth2 tokens — lazy, may be null if Google not linked. */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private OAuthToken oauthToken;

    /** Biometric embedding — lazy, null until enrollment. */
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private BiometricData biometricData;

    /** Full audit trail of this user's actions. */
    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JsonIgnore
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

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getGoogleInternalPassword() {
        return googleInternalPassword;
    }

    public void setGoogleInternalPassword(String googleInternalPassword) {
        this.googleInternalPassword = googleInternalPassword;
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

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public void setTwoFactorEnabled(boolean twoFactorEnabled) {
        this.twoFactorEnabled = twoFactorEnabled;
    }

    public boolean isTwoFactorEnforced() {
        return twoFactorEnforced;
    }

    public void setTwoFactorEnforced(boolean twoFactorEnforced) {
        this.twoFactorEnforced = twoFactorEnforced;
    }

    public boolean isFaceEnabled() {
        return faceEnabled;
    }

    public void setFaceEnabled(boolean faceEnabled) {
        this.faceEnabled = faceEnabled;
    }

    public boolean isFaceEnrolled() {
        return faceEnrolled;
    }

    public void setFaceEnrolled(boolean faceEnrolled) {
        this.faceEnrolled = faceEnrolled;
    }

    public Double getAvgLoginHour() {
        return avgLoginHour;
    }

    public void setAvgLoginHour(Double avgLoginHour) {
        this.avgLoginHour = avgLoginHour;
    }

    public boolean isTotpEnabled() {
        return totpEnabled;
    }

    public void setTotpEnabled(boolean totpEnabled) {
        this.totpEnabled = totpEnabled;
    }

    public boolean isTotpEnrolled() {
        return totpEnrolled;
    }

    public void setTotpEnrolled(boolean totpEnrolled) {
        this.totpEnrolled = totpEnrolled;
    }

    public String getTotpSecret() {
        return totpSecret;
    }

    public void setTotpSecret(String totpSecret) {
        this.totpSecret = totpSecret;
    }
}
