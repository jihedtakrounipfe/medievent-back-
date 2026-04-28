package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceCalculationRequestDTO {
    private Long userId;
    private Long planId;
    private String billingCycle; // MONTHLY or YEARLY
    private String promoCode;
    private String planType;// Optional
}
