package skylinkers.tn.mediconnectbackend.entities;

import skylinkers.tn.mediconnectbackend.security.converter.AES256Converter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Stores the facial recognition embedding vector for a user.
 *
 * RGPD Article 9 compliance:
 *   - Stored in a separate table (mc_biometric_data) with access restricted
 *     exclusively to the face-recognition-service microservice.
 *   - The raw 512-dimensional embedding is AES-256-GCM encrypted at rest.
 *   - Only the embedding is persisted — original photos are NEVER stored.
 *   - Deletion endpoint: DELETE /api/users/{id}/biometric removes this row
 *     AND resets the biometricEnrolled flag on the parent AppUser.
 *
 * SOLID — SRP: This class has one reason to exist: hold the biometric
 *              vector. No auth logic lives here.
 */
@Entity
@Table(name = "mc_biometric_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BiometricData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Owning user. FetchType.LAZY — we never need the full user when loading biometrics. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    /**
     * AES-256-GCM encrypted 512-dimensional float vector (FaceNet output).
     * Stored as a byte[] BLOB. The converter handles Base64 encode/decode
     * transparently via the byte[] ↔ String bridge below.
     *
     * NOTE: The converter operates on String. The byte[] is serialized to
     * Base64 before encryption for converter compatibility.
     */
    @Lob
    @Column(name = "embedding_vector", nullable = false)
    @Convert(converter = AES256Converter.class)
    private String embeddingVector; // Base64(float[512]) → AES-GCM → Base64 persisted

    /** Allows soft-revocation without losing historical data for audit. */
    @Column(name = "is_active", nullable = false)
    @lombok.Builder.Default
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}

