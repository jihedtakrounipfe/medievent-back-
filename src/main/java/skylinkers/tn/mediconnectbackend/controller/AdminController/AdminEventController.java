package skylinkers.tn.mediconnectbackend.controller.AdminController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.dto.MedicalEventDTO;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;
import skylinkers.tn.mediconnectbackend.service.MedicalEventService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/events")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminEventController {

    private final MedicalEventService eventService;

    @GetMapping
    public ResponseEntity<List<MedicalEventDTO>> getAll() {
        return ResponseEntity.ok(eventService.getAllEventsForAdmin());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<MedicalEventDTO>> getPending() {
        return ResponseEntity.ok(eventService.getEventsByStatus(EventStatus.PENDING));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<MedicalEventDTO> approve(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.approveEvent(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<MedicalEventDTO> reject(@PathVariable Long id, @RequestBody String reason) {
        return ResponseEntity.ok(eventService.rejectEvent(id, reason));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        // Admin can delete any event
        eventService.deleteEvent(id, "admin");
        return ResponseEntity.noContent().build();
    }
}
