package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "verification_codes")
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    /**
     * 6-digit OTP for EMAIL_VERIFICATION and 2FA_LOGIN purposes,
     * or a UUID-v4 (36 chars) for PASSWORD_CHANGE_TOKEN purpose.
     * Length 40 accommodates both.
     */
    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    /**
     * Distinguishes the purpose of this code:
     *   "EMAIL_VERIFICATION" — email address confirmation at registration
     *   "2FA_LOGIN"          — two-factor authentication at login
     */
    @Column(nullable = false, length = 30, columnDefinition = "VARCHAR(30) DEFAULT 'EMAIL_VERIFICATION'")
    private String purpose = "EMAIL_VERIFICATION";

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

