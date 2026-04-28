package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import skylinkers.tn.mediconnectbackend.entities.enums.SubVerificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_verifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private String universityName;

    @Column
    private String studentIdNumber;

    @Column
    private String facultyEmail;

    // Azure Blob Storage URL of uploaded document
    @Column(nullable = false)
    private String documentUrl;

    // Original filename
    @Column(nullable = false)
    private String documentFileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubVerificationStatus status = SubVerificationStatus.PENDING;

    // Raw OCR text extracted by Azure
    @Column(columnDefinition = "TEXT")
    private String extractedText;

    // Reason if rejected
    @Column
    private String rejectionReason;

    // Confidence score from our validation (0.0 - 1.0)
    @Column
    private Double confidenceScore;

    // Retry count for failed Gemini validations (3-strike limit)
    @Column
    @Builder.Default
    private Integer retryCount = 0;

    @Column
    private LocalDateTime verifiedAt;

    @Column
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void setStatus(SubVerificationStatus status) {
        this.status = status;
        if (status == SubVerificationStatus.APPROVED && this.verifiedAt != null) {
            this.expiresAt = this.verifiedAt.plusDays(90);
        }
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
        if (this.status == SubVerificationStatus.APPROVED && verifiedAt != null) {
            this.expiresAt = verifiedAt.plusDays(90);
        }
    }
}