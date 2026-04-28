package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "doctor_plans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorPlan {

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
    private Integer maxConsultationsPerMonth;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasCalendarSync = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasSearchVisibility = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasBasicAnalytics = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasAdvancedAnalytics = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasForumBadge = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasConsultationPrerequisites = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean hasAI = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

}