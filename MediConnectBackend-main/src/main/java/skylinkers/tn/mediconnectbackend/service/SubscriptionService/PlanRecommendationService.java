package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PlanChatRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PlanChatResponseDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PlanRecommendationRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PlanRecommendationResponseDTO;

public interface PlanRecommendationService {
    PlanRecommendationResponseDTO recommendPlan(PlanRecommendationRequestDTO request);
    PlanChatResponseDTO chat(PlanChatRequestDTO request);
}