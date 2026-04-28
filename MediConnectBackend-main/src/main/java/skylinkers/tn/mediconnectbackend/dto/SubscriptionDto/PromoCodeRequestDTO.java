package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeRequestDTO {
    private String code;
    private String description;
    private String discountType; // PERCENTAGE or FIXED
    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Integer maxUsesTotal;
    private Integer maxUsesPerUser;
    private String planType; // PATIENT, DOCTOR, BOTH
    private String planName; // FREE, BASIC, PREMIUM, ALL
    private String billingCycle; // MONTHLY, YEARLY, BOTH
    private BigDecimal minPurchaseAmount;
}