package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpgradeDowngradeRequestDTO {
    private Long newPlanId;
    private String newBillingCycle; // MONTHLY or YEARLY
    private String promoCode; // Optional promo code for plan switch checkout
}
