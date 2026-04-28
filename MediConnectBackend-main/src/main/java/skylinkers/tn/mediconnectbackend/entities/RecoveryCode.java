package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Single-use TOTP recovery code.
 * The plaintext code is shown to the user once during setup;
 * only the BCrypt hash is persisted here.
 */
@Entity
@Table(
        name = "recovery_codes",
        indexes = {
                @Index(name = "idx_recovery_user_id", columnList = "user_id"),
                @Index(name = "idx_recovery_used",    columnList = "used")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** BCrypt hash of the plaintext recovery code. */
    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
