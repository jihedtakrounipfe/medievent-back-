package skylinkers.tn.mediconnectbackend.controller;
import lombok.extern.slf4j.Slf4j;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.dto.MedicalEventDTO;
import skylinkers.tn.mediconnectbackend.service.MedicalEventService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/doctor/events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DOCTOR')")
public class MedicalEventController {

    private final MedicalEventService eventService;

    @PostMapping
    public ResponseEntity<MedicalEventDTO> create(@RequestBody MedicalEventDTO dto, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.createEvent(dto, jwt.getClaimAsString("email")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalEventDTO> update(@PathVariable Long id, @RequestBody MedicalEventDTO dto, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.updateEvent(id, dto, jwt.getClaimAsString("email")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        eventService.deleteEvent(id, jwt.getClaimAsString("email"));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<MedicalEventDTO>> getMyEvents(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt != null ? jwt.getClaimAsString("email") : null;
        log.info("[DOCTOR-API] GET /my. Email from JWT: {}", email);
        try {
            return ResponseEntity.ok(eventService.getMyEvents(email));
        } catch (Exception e) {
            log.error("[DOCTOR-API] Error fetching events for {}: {}", email, e.getMessage(), e);
            throw e;
        }
    }
    @PostMapping("/{id}/speakers/{doctorId}")
    public ResponseEntity<MedicalEventDTO> addSpeaker(@PathVariable Long id, @PathVariable Long doctorId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.addSpeaker(id, doctorId, jwt.getClaimAsString("email")));
    }

    @DeleteMapping("/{id}/speakers/{doctorId}")
    public ResponseEntity<Void> removeSpeaker(@PathVariable Long id, @PathVariable Long doctorId, @AuthenticationPrincipal Jwt jwt) {
        eventService.removeSpeaker(id, doctorId, jwt.getClaimAsString("email"));
        return ResponseEntity.noContent().build();
    }
}
