package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores a short-lived 6-digit code used for password reset via email.
 * One active token per email at a time (old ones are deleted on new request).
 */
@Entity
@Table(name = "password_reset_tokens",
        indexes = @Index(name = "idx_prt_email", columnList = "email"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    /** 6-digit numeric code sent to the user's email. */
    @Column(nullable = false, length = 10)
    private String code;

    /** When this token stops being valid (15 minutes from creation). */
    @Column(name = "expiry_at", nullable = false)
    private LocalDateTime expiryAt;

    /** True once the token has been successfully used. */
    @Column(nullable = false)
    private boolean used = false;
}
