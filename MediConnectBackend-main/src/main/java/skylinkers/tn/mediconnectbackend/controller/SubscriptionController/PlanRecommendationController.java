package skylinkers.tn.mediconnectbackend.controller.SubscriptionController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PlanChatRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PlanChatResponseDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PlanRecommendationRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PlanRecommendationResponseDTO;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.PlanRecommendationService;

@RestController
@RequestMapping("/api/plan-recommendation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class PlanRecommendationController {

    private final PlanRecommendationService planRecommendationService;

    @PostMapping("/recommend")
    public ResponseEntity<PlanRecommendationResponseDTO> recommendPlan(
            @RequestBody PlanRecommendationRequestDTO request) {
        log.info("Plan recommendation request from user: {}", request.getUserId());
        PlanRecommendationResponseDTO response = planRecommendationService.recommendPlan(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/chat")
    public ResponseEntity<PlanChatResponseDTO> chat(
            @RequestBody PlanChatRequestDTO request) {
        log.info("Chat request from user: {}", request.getUserId());
        PlanChatResponseDTO response = planRecommendationService.chat(request);
        return ResponseEntity.ok(response);
    }
}