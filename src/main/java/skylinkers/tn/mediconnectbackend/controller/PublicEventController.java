package skylinkers.tn.mediconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skylinkers.tn.mediconnectbackend.dto.MedicalEventDTO;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;
import skylinkers.tn.mediconnectbackend.service.MedicalEventService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class PublicEventController {

    private final MedicalEventService eventService;

    @GetMapping("/active")
    public ResponseEntity<List<MedicalEventDTO>> getActiveEvents() {
        List<MedicalEventDTO> approved = eventService.getEventsByStatus(EventStatus.APPROVED);
        List<MedicalEventDTO> pending = eventService.getEventsByStatus(EventStatus.PENDING);
        List<MedicalEventDTO> all = new java.util.ArrayList<>();
        all.addAll(approved);
        all.addAll(pending);
        return ResponseEntity.ok(all);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalEventDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }
}
