package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditHistoryEntryDTO {
    private LocalDateTime date;
    private String description;
    private BigDecimal amount;
    private BigDecimal remainingBalance;
    private String eventType; // APPLIED or EARNED
}
