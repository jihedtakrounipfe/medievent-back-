package skylinkers.tn.mediconnectbackend.utils;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.service.AppointmentServices.Servicemail;

@Slf4j
@Component
@RequiredArgsConstructor
public class RescheduleNotificationListener {



        private final Servicemail servicemail; // On utilise ton beau service de mail

        @Async
        @EventListener
        public void onReschedule(AppointmentRescheduledEvent event) {
            Appointment rdv = event.getAppointment();

            if (rdv.getPatient() != null && rdv.getPatient().getEmail() != null) {

                // On récupère le nom du patient (si tu as un champ firstName/lastName)
                String patientName = rdv.getPatient().getFirstName() != null ? rdv.getPatient().getFirstName() : "Patient";

                servicemail.sendRescheduleEmailToPatient(
                        rdv.getPatient().getEmail(),
                        patientName,
                        rdv.getMedecin(),
                        event.getAncienneHeure().toString(),
                        event.getNouvelleHeure().toString()
                );

                log.info("✅ Email de reprogrammation envoyé à {}", rdv.getPatient().getEmail());
            }
        }
    }

