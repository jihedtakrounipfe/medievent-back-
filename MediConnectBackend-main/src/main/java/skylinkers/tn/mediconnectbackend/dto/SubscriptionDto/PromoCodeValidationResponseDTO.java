package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeValidationResponseDTO {
    private Boolean valid;
    private String message; // Error message if invalid
    private BigDecimal discountAmount; // How much they save
    private BigDecimal finalPrice; // Price after discount
    private String discountType; // PERCENTAGE or FIXED
    private BigDecimal discountValue;
}