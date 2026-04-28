package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.entities.Appointment;

import java.util.List;
import java.util.Map;

public interface IAppointmentServices {

    // ─── Existantes (inchangées) ──────────────────────────────────────────
    Appointment ajouterRdv(Appointment app, String keycloakId);
    List<Appointment> AfficherAppointments();
    Appointment modifierAppointment(Appointment app);
    void supprimerAppointment(long idapp);
    List<Appointment> getAppointmentsByDoctor(String medecin);
    Appointment updateStatus(Long id, AppointmentStatus status);
    List<Appointment> getAppointmentsForPatient(String keycloakId);

    // ─── Nouvelle ─────────────────────────────────────────────────────────
    Map<String, Object> getDashboard(String medecin);
    public void updateUrgent(Long id, boolean value);
}