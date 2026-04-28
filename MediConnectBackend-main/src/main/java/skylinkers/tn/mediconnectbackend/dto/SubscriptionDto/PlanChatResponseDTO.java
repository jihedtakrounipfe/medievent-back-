package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlanChatResponseDTO {
    private String message;
    private boolean recommendationReady;
    private String recommendedPlan;
    private String reasoning;
    private Double confidenceScore;
}