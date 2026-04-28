package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import skylinkers.tn.mediconnectbackend.entities.enums.DiscountType;
import skylinkers.tn.mediconnectbackend.entities.enums.PromoCodeBillingCycle;
import skylinkers.tn.mediconnectbackend.entities.enums.PromoCodePlanType;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code; // SAVE20, STUDENT50, etc.

    @Column(length = 500)
    private String description; // Admin note

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DiscountType discountType; // PERCENTAGE or FIXED

    @Column(nullable = false)
    private BigDecimal discountValue; // 20 (for 20%) or 5 (for 5 TND)

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private Integer maxUsesTotal; // Max total uses across all users

    @Column(nullable = false)
    private Integer maxUsesPerUser = 1; // Usually 1

    @Column(nullable = false)
    private Integer currentUseCount = 0; // How many times used so far

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PromoCodePlanType planType; // PATIENT, DOCTOR, or BOTH (custom: need enum update)

    @Column(nullable = false)
    private String planName; // FREE, BASIC, PREMIUM, ALL

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PromoCodeBillingCycle billingCycle; // MONTHLY, YEARLY, or BOTH (custom: need enum update)

    @Column(nullable = false)
    private BigDecimal minPurchaseAmount = BigDecimal.ZERO; // Minimum price to apply

    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false)
    private AppUser createdByAdmin;
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


}