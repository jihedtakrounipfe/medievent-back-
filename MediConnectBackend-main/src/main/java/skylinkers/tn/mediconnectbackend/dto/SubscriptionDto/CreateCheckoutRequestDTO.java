package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCheckoutRequestDTO {
    private Long userId;
    private Long patientPlanId;  // nullable if upgrading
    private Long doctorPlanId;   // nullable if upgrading
    private String billingCycle;  // MONTHLY or YEARLY
    @JsonAlias({"promo_code", "promo", "code", "couponCode", "coupon_code"})
    private String promoCode;     // Optional promo code entered at checkout
    private Boolean isPlanChange;
    private Long currentSubscriptionId;
    private Long upgradeFromSubscriptionId;
}