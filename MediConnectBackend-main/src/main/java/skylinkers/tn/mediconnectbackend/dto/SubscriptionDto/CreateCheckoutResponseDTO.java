package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCheckoutResponseDTO {
    private String sessionId;
    private String url;
    private String message;
    private BigDecimal basePrice;
    private BigDecimal studentDiscount;
    private BigDecimal promoDiscount;
    private BigDecimal creditApplied;
    private BigDecimal finalAmount;
}