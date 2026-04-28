package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceSummaryResponseDTO {
    private BigDecimal baseAmount;
    private BigDecimal studentDiscountAmount;
    private BigDecimal promoDiscountAmount;
    private BigDecimal creditUsed;
    private BigDecimal finalAmount;
}
