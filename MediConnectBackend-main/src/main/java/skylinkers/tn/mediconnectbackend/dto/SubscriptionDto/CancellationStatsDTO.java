package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancellationStatsDTO {
    private Map<String, Long> categoryCounts;
}
