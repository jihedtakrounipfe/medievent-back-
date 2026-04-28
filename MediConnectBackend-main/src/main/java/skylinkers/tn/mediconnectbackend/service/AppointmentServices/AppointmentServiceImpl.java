package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.entities.enums.UrgencyLevel;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.PatientRepository;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements IAppointmentServices {

    private final AppointmentRepository appointmentRepository;
    private final AnalyticsService analyticsService;
    private final ServiceIA aiService;
    private final PatientRepository patientRepository;
    private Appointment appointment;
    @Autowired
    private HolidayService holidayService;


//    @Override
//    public Appointment ajouterRdv(Appointment app) {
//        return appointmentRepository.save(app);
//    }

    @Override
    public Appointment ajouterRdv(Appointment app, String keycloakId) {
        if (app.getUrgencyLevel() == null) {
            app.setUrgencyLevel(UrgencyLevel.NORMAL);
        }

        // 👇 Vérification jour férié
        if (app.getDate() != null && holidayService.isHoliday(app.getDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Impossible de prendre un RDV un jour férié tunisien."
            );
        }

        Patient patient = patientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException(
                        "Patient introuvable pour keycloakId: " + keycloakId));

        app.setPatient(patient);
        app.setStatus(AppointmentStatus.PENDING);
        return appointmentRepository.save(app);
    }

    @Override
    public List<Appointment> AfficherAppointments() {
        return List.of();
    }


//    @Override
//    public List<Appointment> AfficherAppointments() {
//        return appointmentRepository.findAll();
//    }

    @Override
    public List<Appointment> getAppointmentsForPatient(String keycloakId) {
        Patient patient = patientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new RuntimeException(
                        "Patient introuvable pour keycloakId: " + keycloakId));

        return appointmentRepository.findByPatient(patient);
    }


    @Override
    public Appointment modifierAppointment(Appointment app) {
        if (app.getIdAppointment() == null) {
            throw new RuntimeException("ID du rendez-vous manquant pour la modification");
        }
        Appointment existing = appointmentRepository.findById(app.getIdAppointment())
                .orElseThrow(() -> new RuntimeException("Rendez-vous non trouvé avec l'ID: " + app.getIdAppointment()));

        existing.setTypeRdv(app.getTypeRdv());
        existing.setSpecialite(app.getSpecialite());
        existing.setMedecin(app.getMedecin());
        existing.setLaboratoire(app.getLaboratoire());
        existing.setDate(app.getDate());
        existing.setHeure(app.getHeure());
        existing.setMotif(app.getMotif());
        existing.setStatus(app.getStatus());

        return appointmentRepository.save(existing);
    }

    @Override
    public void supprimerAppointment(long idapp) {
        appointmentRepository.deleteById(idapp);
    }

    @Override
    public List<Appointment> getAppointmentsByDoctor(String medecin) {
        return appointmentRepository.findByMedecin(medecin);
    }

    @Override
    public Appointment updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rendez-vous non trouvé avec l'ID: " + id));
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

//    @Override
//    public List<Appointment> getAppointmentsForPatient() {
//        return appointmentRepository.findByStatusNot(AppointmentStatus.CANCELLED);
//    }




        //a verifier bech tetfasakh ou non
    @Override
    public Map<String, Object> getDashboard(String medecin) {
        return analyticsService.getFullDashboard(medecin);
    }

    public void updateUrgent(Long id, boolean value) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Appointment not found: " + id));

        appointment.setUrgent(value);
        appointmentRepository.save(appointment);
    }







}