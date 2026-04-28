package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeValidationDTO {
    private String code;
    private Long userId; // User applying the code
    private String planType; // PATIENT or DOCTOR
    private String planName; // BASIC, PREMIUM, etc.
    private String billingCycle; // MONTHLY or YEARLY
    private BigDecimal planPrice; // Original price
}