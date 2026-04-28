package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PatientPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private BigDecimal priceMonthly; // TND

    @Column(nullable = false)
    private BigDecimal priceYearly; // TND

    // null = unlimited
    @Column
    private Integer maxAppointmentsPerMonth;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasDocumentUpload = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasMedicationReminder = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasLabResults = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasSelfTestReadings = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasForum = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasAI = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasHealthEvents = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPromoApplicable = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}