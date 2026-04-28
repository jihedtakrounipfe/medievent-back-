package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromoCodeResponseDTO {
    private Long id;
    private String code;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private Integer maxUsesTotal;
    private Integer maxUsesPerUser;
    private Integer currentUseCount;
    private String planType;
    private String planName;
    private String billingCycle;
    private BigDecimal minPurchaseAmount;
    private String createdByAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}