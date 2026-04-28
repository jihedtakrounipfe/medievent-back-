package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.AppointmentRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {

    private final AppointmentRepository appointmentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    // ─── Patient signale son arrivée au cabinet ───────────────────────────────
    public Appointment patientArrived(Long idAppointment) {
        Appointment app = appointmentRepository.findById(idAppointment)
                .orElseThrow(() -> new RuntimeException("RDV introuvable : " + idAppointment));

        if (app.getStatus() != AppointmentStatus.CONFIRMED) {
            throw new IllegalStateException("Le RDV doit être CONFIRMED.");
        }

        broadcastQueueUpdate(app.getMedecin());
        return app;
    }

    // ─── Médecin appelle le patient suivant ───────────────────────────────────
    public void callNextPatient(String medecinName) {
        LocalDate today = LocalDate.now();

        // 1. Terminer le patient PENDING actuel
        appointmentRepository
                .findByMedecinAndDateAndStatus(medecinName, today, AppointmentStatus.PENDING)
                .forEach(app -> {
                    app.setStatus(AppointmentStatus.DONE);
                    appointmentRepository.save(app);
                });

        // 2. Prendre le suivant : URGENT d'abord, puis ordre heure
        List<Appointment> waitingList = appointmentRepository.findQueueByMedecinAndDate(
                medecinName, today,
                List.of(AppointmentStatus.CONFIRMED)
        );

        if (!waitingList.isEmpty()) {
            Appointment next = waitingList.get(0);
            next.setStatus(AppointmentStatus.PENDING);
            appointmentRepository.save(next);

            Long patientId = next.getPatient().getId();
            messagingTemplate.convertAndSend(
                    "/topic/patient/" + patientId,
                    (Object) Map.of("event", "YOUR_TURN", "idAppointment", next.getIdAppointment())
            );
        }

        broadcastQueueUpdate(medecinName);
    }

    // ─── Position du patient dans la file ─────────────────────────────────────
    public Map<String, Object> getPatientQueuePosition(Long idAppointment) {
        Appointment myApp = appointmentRepository.findById(idAppointment)
                .orElseThrow(() -> new RuntimeException("RDV introuvable : " + idAppointment));

        // ✅ Si le RDV n'est pas aujourd'hui, chercher le RDV CONFIRMED d'aujourd'hui pour ce patient
        if (!myApp.getDate().equals(LocalDate.now())) {
            Appointment todayApp = appointmentRepository
                    .findByPatientAndDateAndStatus(
                            myApp.getPatient(),
                            LocalDate.now(),
                            AppointmentStatus.CONFIRMED
                    )
                    .stream()
                    .findFirst()
                    .orElse(null);

            if (todayApp == null) {
                return buildResult(-1, -1, 0, false,
                        myApp.getHeure().toString(),
                        myApp.getStatus().toString(),
                        "Vous n'avez pas de RDV confirmé aujourd'hui");
            }

            // ✅ Continuer avec le bon RDV d'aujourd'hui
            myApp = todayApp;
        }

        // Vérifier que le RDV est CONFIRMED
        if (myApp.getStatus() != AppointmentStatus.CONFIRMED) {
            return buildResult(-1, -1, 0, false,
                    myApp.getHeure().toString(),
                    myApp.getStatus().toString(),
                    "Votre RDV n'est pas encore confirmé");
        }

        List<Appointment> allToday = appointmentRepository
                .findByMedecinAndDateOrderByHeureAsc(myApp.getMedecin(), LocalDate.now());

        Appointment finalMyApp = myApp;
        List<Appointment> waitingBeforeMe = allToday.stream()
                .filter(a -> a.getHeure().isBefore(finalMyApp.getHeure()))
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED
                        || a.getStatus() == AppointmentStatus.PENDING)
                .toList();

        Appointment finalMyApp1 = myApp;
        List<Appointment> doneBeforeMe = allToday.stream()
                .filter(a -> a.getHeure().isBefore(finalMyApp1.getHeure()))
                .filter(a -> a.getStatus() == AppointmentStatus.DONE)
                .toList();

        return buildResult(
                waitingBeforeMe.size() + 1,
                waitingBeforeMe.size(),
                doneBeforeMe.size(),
                waitingBeforeMe.isEmpty(),
                myApp.getHeure().toString(),
                myApp.getStatus().toString(),
                "ok"
        );
    }

    // ─── Broadcast WebSocket ──────────────────────────────────────────────────
    public void broadcastQueueUpdate(String medecinName) {
        LocalDate today = LocalDate.now();

        List<Appointment> queue = appointmentRepository.findQueueByMedecinAndDate(
                medecinName, today,
                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.PENDING)
        );

        messagingTemplate.convertAndSend(
                "/topic/queue/" + slugify(medecinName),
                queue
        );

        log.info("Broadcast /topic/queue/{} → {} appointments",
                slugify(medecinName), queue.size());
    }

    // ─── Chargement initial REST ──────────────────────────────────────────────
    public List<Appointment> getQueueForDoctor(String medecinName) {
        return appointmentRepository.findQueueByMedecinAndDate(
                medecinName,
                LocalDate.now(),
                List.of(AppointmentStatus.CONFIRMED, AppointmentStatus.PENDING)
        );
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────
    private Map<String, Object> buildResult(int position, int before, int doneBefore,
                                            boolean isMyTurn, String heure,
                                            String status, String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("position", position);
        result.put("before", before);
        result.put("doneBefore", doneBefore);
        result.put("isMyTurn", isMyTurn);
        result.put("myHeure", heure);
        result.put("status", status);
        result.put("message", message);
        return result;
    }

    private String slugify(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-");
    }
}