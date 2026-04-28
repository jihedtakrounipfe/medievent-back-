package skylinkers.tn.mediconnectbackend.utils;


import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.AppointmentRepository;

import java.time.LocalDate;
import java.util.List;

@Component
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    public AppointmentReminderScheduler(AppointmentRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void sendReminders() {
        Twilio.init(accountSid, authToken);

        LocalDate demain = LocalDate.now(); // now pour tester b date lyoum

        List<Appointment> rdvs = appointmentRepository
                .findByDateAndStatus(demain, AppointmentStatus.CONFIRMED);

        System.out.println("=== Scheduler lancé ===");
        System.out.println("Date cherchée : " + demain);
        System.out.println("Nombre de RDV trouvés : " + rdvs.size());

        for (Appointment rdv : rdvs) {
            String phone = rdv.getPatient().getPhone();
            System.out.println("Patient : " + rdv.getPatient().getFirstName());
            System.out.println("Phone : " + phone);

            if (phone == null || phone.isBlank()) {
                System.out.println("⚠️ Numéro manquant, skipped");
                continue;
            }

            try {
                String texte = String.format(
                        "Bonjour %s, rappel de votre RDV demain le %s à %s avec Dr. %s. - MediConnect",
                        rdv.getPatient().getFirstName(),
                        rdv.getDate(),
                        rdv.getHeure(),
                        rdv.getMedecin()
                );

                Message message = Message.creator(
                        new PhoneNumber("whatsapp:" + phone),
                        new PhoneNumber("whatsapp:+14155238886"),
                        texte
                ).create();

                System.out.println("✅ WhatsApp envoyé ! SID : " + message.getSid());

            } catch (Exception e) {
                System.out.println("❌ Erreur : " + e.getMessage());
            }
        }
    }
}
