package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import skylinkers.tn.mediconnectbackend.entities.enums.BillingCycle;
import skylinkers.tn.mediconnectbackend.entities.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = true)
    private Subscription subscription;

    @Column(nullable = false)
    private String stripeSessionId;

    @Column
    private String stripePaymentIntentId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "TND";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private BillingCycle billingCycle = BillingCycle.MONTHLY;

    @Column
    private String stripePaymentMethodId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    // Payment Stripe
    @ManyToOne
    @JoinColumn(name = "patient_plan_id")
    private PatientPlan patientPlan;

    @ManyToOne
    @JoinColumn(name = "doctor_plan_id")
    private DoctorPlan doctorPlan;

    @Column(name = "promo_code")
    private String promoCode;

    @Column(name = "promo_discount_amount")
    private BigDecimal promoDiscountAmount;

    @Column(name = "credit_applied", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal creditApplied = BigDecimal.ZERO;

    @Column(name = "remaining_credit", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal remainingCredit = BigDecimal.ZERO;

    @Column(name = "reserved_user_credit", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal reservedUserCredit = BigDecimal.ZERO;

    @Column(name = "original_price_tnd", precision = 12, scale = 2)
    private BigDecimal originalPriceTnd;

    @Column(name = "upgrade_from_subscription_id")
    private Long upgradeFromSubscriptionId;

    @Column(name = "student_discount_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal studentDiscountAmount = BigDecimal.ZERO;
}