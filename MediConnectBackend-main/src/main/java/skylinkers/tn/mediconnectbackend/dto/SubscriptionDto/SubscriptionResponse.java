package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.Builder;
import lombok.Data;
import skylinkers.tn.mediconnectbackend.entities.enums.BillingCycle;
import skylinkers.tn.mediconnectbackend.entities.enums.PaymentProvider;
import skylinkers.tn.mediconnectbackend.entities.enums.PlanType;
import skylinkers.tn.mediconnectbackend.entities.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {

    private Long id;
    private Long userId;
    private PlanType planType;
    private SubscriptionStatus status;
    private BillingCycle billingCycle;
    private Boolean autoRenew;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime cancelledAt;
    private PaymentProvider paymentProvider;
    private String paymentRef;
    private String paymentUrl;
    private Long invoiceId;
    private LocalDateTime lastPaymentAt;
    private LocalDateTime createdAt;

    // Plan details
    private String planName; // FREE, BASIC, PREMIUM
    private java.math.BigDecimal planPrice;   // Monthly or yearly price of current plan
    private java.math.BigDecimal amountPaid;  // Actual amount paid for current period

    // Patient plan fields
    private Integer maxAppointmentsPerMonth; // null = unlimited
    private Boolean hasDocumentUpload;
    private Boolean hasMedicationReminder;
    private Boolean hasLabResults;
    private Boolean hasSelfTestReadings;
    private Boolean hasForum;
    private Boolean hasHealthEvents;

    // Doctor plan fields
    private Integer maxConsultationsPerMonth; // null = unlimited
    private Boolean hasCalendarSync;
    private Boolean hasSearchVisibility;
    private Boolean hasBasicAnalytics;
    private Boolean hasAdvancedAnalytics;
    private Boolean hasForumBadge;
    private Boolean hasConsultationPrerequisites;

    // Shared
    private Boolean hasAI;

}