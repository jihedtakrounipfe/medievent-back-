package skylinkers.tn.mediconnectbackend.service.AppointmentServices;


import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.entities.enums.UrgencyLevel;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.utils.AppointmentRescheduledEvent;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.AppointmentRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UrgentSchedulingService {

    private static final int SLOT_MINUTES = 30;
    private final AppointmentRepository appointmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Appointment markAsUrgent(Long appointmentId) {
        Appointment urgentApp = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("RDV introuvable: " + appointmentId));

        String medecin = urgentApp.getMedecin();
        LocalDate today = LocalDate.now();
        LocalTime maintenant = LocalTime.now();

        // 1. Déterminer l'heure de début pour l'urgence
        // On prend soit l'heure actuelle (arrondie), soit le début de journée si on est en avance
        LocalTime heureDebutRecherche = maintenant.isBefore(LocalTime.of(8, 0))
                ? LocalTime.of(8, 0)
                : arrondirAuProchainSlot(maintenant);

        // 2. Trouver tous les RDV à partir de cette heure pour les décaler
        List<Appointment> aDecaler = appointmentRepository
                .findAppointmentsToReschedule(medecin, today, heureDebutRecherche)
                .stream()
                // On ne se décale pas soi-même si on était déjà dans la liste
                .filter(rdv -> !rdv.getIdAppointment().equals(urgentApp.getIdAppointment()))
                .toList();

        // 3. Décalage en cascade
        // On commence par le dernier pour éviter les collisions si on utilisait une heure fixe
        // Mais ici on ajoute simplement SLOT_MINUTES à chacun
        for (Appointment rdv : aDecaler) {
            LocalTime ancienneHeure = rdv.getHeure();
            LocalTime nouvelleHeure = ancienneHeure.plusMinutes(SLOT_MINUTES);
            rdv.setHeure(nouvelleHeure);
            appointmentRepository.save(rdv);

            eventPublisher.publishEvent(
                    new AppointmentRescheduledEvent(this, rdv, ancienneHeure, nouvelleHeure)
            );
        }

        // 4. Placer le RDV urgent exactement au créneau libéré
        urgentApp.setDate(today);
        urgentApp.setHeure(heureDebutRecherche);
        urgentApp.setUrgencyLevel(UrgencyLevel.URGENT);
        urgentApp.setStatus(AppointmentStatus.CONFIRMED);

        return appointmentRepository.save(urgentApp);
    }

    /**
     * Utilitaire pour ne pas donner RDV à 11h07 mais à 11h30
     */
    private LocalTime arrondirAuProchainSlot(LocalTime time) {
        int minutes = time.getMinute();
        if (minutes == 0) return time;
        if (minutes <= 30) return time.withMinute(30).withSecond(0).withNano(0);
        return time.plusHours(1).withMinute(0).withSecond(0).withNano(0);
    }
}