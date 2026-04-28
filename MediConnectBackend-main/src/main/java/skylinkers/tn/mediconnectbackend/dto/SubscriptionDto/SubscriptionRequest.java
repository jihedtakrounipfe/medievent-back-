package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import skylinkers.tn.mediconnectbackend.entities.enums.BillingCycle;
import skylinkers.tn.mediconnectbackend.entities.enums.PaymentProvider;
import skylinkers.tn.mediconnectbackend.entities.enums.PlanType;

import java.util.UUID;

@Data
public class SubscriptionRequest {

    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "planType is required")
    private PlanType planType; // PATIENT or DOCTOR

    @NotNull(message = "planId is required")
    private Long planId; // id of PatientPlan or DoctorPlan

    @NotNull(message = "billingCycle is required")
    private BillingCycle billingCycle; // MONTHLY or YEARLY

    @NotNull(message = "paymentProvider is required")
    private PaymentProvider paymentProvider; // KONNECT or PAYMEE

    private Boolean autoRenew = true;

}