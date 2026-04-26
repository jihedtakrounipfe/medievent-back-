package skylinkers.tn.mediconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.dto.AppNotificationDTO;
import skylinkers.tn.mediconnectbackend.service.MedicalEventService;
import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class AppNotificationController {

    private final MedicalEventService eventService;

    @GetMapping("/my")
    public ResponseEntity<?> getMyNotifications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.ok(List.of());
        try {
            return ResponseEntity.ok(eventService.getMyNotifications(jwt.getClaimAsString("email")));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error in getMyNotifications: " + e.getMessage() + " | Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "none"));
        }
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();
        eventService.markNotificationAsRead(id, jwt.getClaimAsString("email"));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear")
    public ResponseEntity<Void> clear(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).build();
        eventService.clearNotifications(jwt.getClaimAsString("email"));
        return ResponseEntity.noContent().build();
    }
}
