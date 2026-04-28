package skylinkers.tn.mediconnectbackend.controller.AppointmentController;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import skylinkers.tn.mediconnectbackend.service.AppointmentServices.QueueService;

import java.util.Map;

// Controller WebSocket STOMP
@Controller
@CrossOrigin(origins = "http://localhost:4200")
@RequiredArgsConstructor
public class QueueWebSocketController {

    private final QueueService queueService;

    // Médecin appelle le suivant : /app/queue/next
    @MessageMapping("/queue/next")
    public void callNext(@Payload Map<String, String> payload) {
        queueService.callNextPatient(payload.get("medecinName"));
    }

    // Patient rejoint via STOMP : /app/queue/join
    @MessageMapping("/queue/join")
    public void joinQueue(@Payload Map<String, Long> payload) {
        queueService.patientArrived(payload.get("idAppointment"));
    }
}
