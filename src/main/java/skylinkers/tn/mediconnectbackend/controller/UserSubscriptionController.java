package skylinkers.tn.mediconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.service.MedicalEventService;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class UserSubscriptionController {

    private final MedicalEventService eventService;

    @PostMapping("/{id}/subscribe")
    public ResponseEntity<?> subscribe(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).body("JWT is null - user not authenticated.");
        try {
            eventService.subscribeToEvent(id, jwt.getClaimAsString("email"));
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error in subscribeToEvent: " + e.getMessage() + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "none"));
        }
    }

    @PostMapping("/{id}/unsubscribe")
    public ResponseEntity<Void> unsubscribe(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();
        eventService.unsubscribeFromEvent(id, jwt.getClaimAsString("email"));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/is-subscribed")
    public ResponseEntity<?> isSubscribed(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.ok(Map.of("subscribed", false));
        try {
            boolean sub = eventService.isSubscribed(id, jwt.getClaimAsString("email"));
            return ResponseEntity.ok(Map.of("subscribed", sub));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error", "cause", e.getCause() != null ? e.getCause().getMessage() : "none"));
        }
    }
}
