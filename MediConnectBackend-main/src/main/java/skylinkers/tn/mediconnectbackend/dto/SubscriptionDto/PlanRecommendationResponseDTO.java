package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRecommendationResponseDTO {
    private String recommendedPlan; // BASIC, PLUS, PREMIUM, STUDENT
    private String reasoning;
    private Double confidenceScore;
    private String planDescription; // Description of recommended plan
}