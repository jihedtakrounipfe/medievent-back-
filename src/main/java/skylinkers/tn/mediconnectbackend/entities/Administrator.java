package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import skylinkers.tn.mediconnectbackend.entities.enums.AdminLevel;

/**
 * Subclass of AppUser representing a platform administrator.
 * Discriminator value: "ADMINISTRATOR"
 *
 * Admins are stored in a SEPARATE Keycloak realm (mediconnect-admin)
 * and are never mixed with patient/doctor accounts.
 *
 * 2FA is always enforced (twoFactorEnforced = true, hardcoded default).
 * Facial recognition is required for all critical actions (deleting users,
 * modifying system config) via a Keycloak custom authenticator.
 *
 * All actions are journaled in AuditLog with IP address and user agent.
 */
@Entity
@DiscriminatorValue("ADMINISTRATOR")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Administrator extends AppUser {

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_level", nullable = false, length = 20,columnDefinition = "VARCHAR(20) DEFAULT 'ADMIN'")
    private AdminLevel adminLevel;

    @Column(length = 100)
    private String department;

    /**
     * Always TRUE — 2FA cannot be disabled for admins.
     * Stored in DB to allow audit queries; never exposed to update endpoints.
     */
    @Column(name = "two_factor_enforced", nullable = false,columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean twoFactorEnforced = true;

    /**
     * When TRUE, a facial recognition challenge is required before
     * any DESTRUCTIVE or CRITICAL Keycloak admin action.
     */
    @Column(name = "biometric_required", nullable = false,columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean biometricRequired = true;

    /** Tracks the last known IP for anomaly detection (geo-fencing alerts). */
    @Column(name = "last_login_ip", length = 45)  // IPv6-safe
    private String lastLoginIp;

    public AdminLevel getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(AdminLevel adminLevel) {
        this.adminLevel = adminLevel;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public boolean isTwoFactorEnforced() {
        return twoFactorEnforced;
    }

    public void setTwoFactorEnforced(boolean twoFactorEnforced) {
        this.twoFactorEnforced = twoFactorEnforced;
    }

    public boolean isBiometricRequired() {
        return biometricRequired;
    }

    public void setBiometricRequired(boolean biometricRequired) {
        this.biometricRequired = biometricRequired;
    }

    public String getLastLoginIp() {
        return lastLoginIp;
    }

    public void setLastLoginIp(String lastLoginIp) {
        this.lastLoginIp = lastLoginIp;
    }
}
