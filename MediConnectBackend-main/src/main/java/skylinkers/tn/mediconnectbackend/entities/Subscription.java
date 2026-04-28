package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import skylinkers.tn.mediconnectbackend.entities.enums.BillingCycle;
import skylinkers.tn.mediconnectbackend.entities.enums.PaymentProvider;
import skylinkers.tn.mediconnectbackend.entities.enums.PlanType;
import skylinkers.tn.mediconnectbackend.entities.enums.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanType planType; // PATIENT or DOCTOR

    @ManyToOne
    @JoinColumn(name = "patient_plan_id")
    private PatientPlan patientPlan;

    @ManyToOne
    @JoinColumn(name = "doctor_plan_id")
    private DoctorPlan doctorPlan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BillingCycle billingCycle; // MONTHLY or YEARLY

    @Column(nullable = false)
    @Builder.Default
    private Boolean autoRenew = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // Set when user cancels — nullable
    @Column
    private LocalDateTime cancelledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider paymentProvider; // KONNECT or PAYMEE

    // Payment gateway reference
    @Column
    private String paymentRef;  // Konnect's paymentRef or Paymee's token

    @Column
    private String paymentUrl;  // redirect URL

    @Column
    private LocalDateTime lastPaymentAt;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid;  // Actual amount paid after discounts/credits

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String cancellationReason;        // raw user text
    private String cancellationCategory;      // PRICE | FEATURES | UX | OTHER

}