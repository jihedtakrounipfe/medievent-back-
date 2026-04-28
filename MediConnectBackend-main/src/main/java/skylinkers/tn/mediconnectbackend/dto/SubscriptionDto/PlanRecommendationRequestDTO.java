package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRecommendationRequestDTO {
    private Long userId;
    private Map<String, String> responses; // question -> answer
}