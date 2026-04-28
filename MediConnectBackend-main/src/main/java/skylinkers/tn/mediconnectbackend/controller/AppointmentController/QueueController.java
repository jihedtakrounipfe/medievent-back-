package skylinkers.tn.mediconnectbackend.controller.AppointmentController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.service.AppointmentServices.QueueService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/queue")
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
@Slf4j
public class QueueController {

    private final QueueService queueService;

    // ─── File du médecin (chargement initial) ─────────────────────────────────
    @GetMapping("/doctor/{medecinName}")
    public ResponseEntity<List<Appointment>> getQueue(@PathVariable String medecinName) {
        return ResponseEntity.ok(queueService.getQueueForDoctor(medecinName));
    }

    // ─── Patient signale son arrivée ──────────────────────────────────────────
    @PostMapping("/arrive/{idAppointment}")
    public ResponseEntity<Appointment> patientArrived(@PathVariable Long idAppointment) {
        try {
            return ResponseEntity.ok(queueService.patientArrived(idAppointment));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ─── Position du patient dans la file ─────────────────────────────────────
    @GetMapping("/position/{idAppointment}")
    public ResponseEntity<Map<String, Object>> getPosition(@PathVariable Long idAppointment) {
        return ResponseEntity.ok(queueService.getPatientQueuePosition(idAppointment));
    }

    // ─── Médecin appelle le suivant ───────────────────────────────────────────
    @PostMapping("/next/{medecinName}")
    public ResponseEntity<Void> callNext(@PathVariable String medecinName) {
        queueService.callNextPatient(medecinName);
        return ResponseEntity.ok().build();
    }
}