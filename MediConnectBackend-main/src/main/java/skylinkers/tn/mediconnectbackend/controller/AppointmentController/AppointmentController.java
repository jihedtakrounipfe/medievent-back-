package skylinkers.tn.mediconnectbackend.controller.AppointmentController;


import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.AppointmentRepository;
import skylinkers.tn.mediconnectbackend.service.AppointmentServices.Servicemail;
import skylinkers.tn.mediconnectbackend.service.AppointmentServices.*;
import skylinkers.tn.mediconnectbackend.entities.enums.UrgencyLevel;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/appointment")
@CrossOrigin(origins = "http://localhost:4200")
@AllArgsConstructor
public class AppointmentController {

      private IAppointmentServices appointmentServices;
      private Servicemail emailService;
      private AppointmentRepository appointmentRepository;
      private  UrgentSchedulingService urgentSchedulingService;

   // @PostMapping("/ajouterRdv")
   // Appointment addRdv(@RequestBody Appointment app) {
   //     return appointmentServices.ajouterRdv(app);
  //  }

    @PostMapping("/ajouterRdv")
    public Appointment addRdv(@RequestBody Appointment app,
                              @AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject(); // = keycloakId dans AppUser
        return appointmentServices.ajouterRdv(app, keycloakId);
    }

//    @GetMapping("/afficherAppointments")
//    List<Appointment> getAppointements() {
//        return appointmentServices.AfficherAppointments();
//    }

    @GetMapping("/afficherAppointments")
     List<Appointment> getAppointements(
            @AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return appointmentServices.getAppointmentsForPatient(keycloakId);
    }

    @PutMapping("/updateAppointement")
    Appointment updateAppointement(@RequestBody Appointment app) {
        return appointmentServices.modifierAppointment(app);
    }



    // ─── Nouveaux endpoints médecin ──────────────────────────────────

    /** Récupérer tous les RDV d'un médecin */
    @GetMapping("/doctor/{medecin}")
    List<Appointment> getByDoctor(@PathVariable String medecin) {
        return appointmentServices.getAppointmentsByDoctor(medecin);
    }

    @PatchMapping("/{id}/accept")
    @ResponseBody
    public ResponseEntity<String> acceptAppointment(@PathVariable Long id) {

        Appointment app = appointmentServices.updateStatus(id, AppointmentStatus.CONFIRMED);

        String patientEmail = "farah.hachemi@yahoo.fr";  // ← remplacer par vrai email
        String patientName  = app.getMotif();              // ← remplacer par vrai nom
        String doctorName   = app.getMedecin();

        emailService.sendConfirmationEmailToPatient(
                patientEmail,
                patientName,
                doctorName,
                app.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                app.getHeure().format(DateTimeFormatter.ofPattern("HH:mm"))
        );

        return ResponseEntity.ok("Rendez-vous confirmé");
    }

    @PatchMapping("/{id}/refuse")
    @ResponseBody
    public ResponseEntity<String> refuseAppointment(@PathVariable Long id) {

        Appointment app = appointmentServices.updateStatus(id, AppointmentStatus.CANCELLED);

        String patientEmail = "farah.hachemi@yahoo.fr";  // ← remplacer par vrai email
        String patientName  = app.getMotif();              // ← remplacer par vrai nom
        String doctorName   = app.getMedecin();

        emailService.sendRefusalEmailToPatient(
                patientEmail,
                patientName,
                doctorName,
                app.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                app.getHeure().format(DateTimeFormatter.ofPattern("HH:mm"))
        );

        return ResponseEntity.ok("Rendez-vous refusé");
    }

    @PatchMapping("/{id}/done")
    @ResponseBody
    public ResponseEntity<String> doneAppointment(@PathVariable Long id) {
        try {
            System.out.println("=== 1. done appelé, id = " + id);

            Appointment app = appointmentServices.updateStatus(id, AppointmentStatus.DONE);
            System.out.println("=== 2. status mis à DONE");

            String patientEmail = "farah.hachemi@yahoo.fr";
            String patientName  = app.getMotif();
            String doctorName   = app.getMedecin();
            System.out.println("=== 3. patientEmail=" + patientEmail + " doctorName=" + doctorName);

            emailService.sendDoneEmailToPatient(
                    patientEmail,
                    patientName,
                    doctorName,
                    app.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    app.getHeure().format(DateTimeFormatter.ofPattern("HH:mm"))
            );
            System.out.println("=== 4. mail envoyé");

            return ResponseEntity.ok("Consultation terminée");

        } catch (Exception e) {
            System.out.println("=== ERREUR : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }


    @DeleteMapping("/deleteAppointment/{ida}")
    @ResponseBody
    public ResponseEntity<Appointment> deleteAppointment(@PathVariable("ida") Long ida) {
        try {
            System.out.println("=== 1. id = " + ida);

            Appointment app = appointmentServices.updateStatus(ida, AppointmentStatus.CANCELLED);
            System.out.println("=== 2. updateStatus OK");

            String patientEmail = "farah.hachemi@yahoo.fr";
            String doctorEmail  = "farah.hachemi@esprit.tn";
            String patientName  = app.getMotif();

            emailService.sendCancellationEmailToDoctor(
                    doctorEmail,
                    patientName,
                    app.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    app.getHeure().format(DateTimeFormatter.ofPattern("HH:mm"))
            );
            System.out.println("=== 3. mail docteur OK");

            emailService.sendCancellationEmailToPatient(
                    patientEmail,
                    patientName,
                    app.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                    app.getHeure().format(DateTimeFormatter.ofPattern("HH:mm"))
            );
            System.out.println("=== 4. mail patient OK");

            appointmentServices.supprimerAppointment(ida);
            System.out.println("=== 5. suppression OK");

            return ResponseEntity.ok(app);

        } catch (Exception e) {
            System.out.println("=== ERREUR : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

//    @GetMapping("/patient/appointments")
//    public List<Appointment> getAppointmentsForPatient() {
//        return appointmentServices.getAppointmentsForPatient();
//    }


    @PatchMapping("/{id}/urgent")
    public ResponseEntity<Appointment> setUrgent(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean urgent) {

        log.info("🔵 setUrgent appelé — id={} urgent={}", id, urgent); // ← ajouter

        if (urgent) {
            Appointment updated = urgentSchedulingService.markAsUrgent(id);
            return ResponseEntity.ok(updated);
        } else {
            Appointment rdv = appointmentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("RDV introuvable"));
            rdv.setUrgencyLevel(UrgencyLevel.NORMAL);
            return ResponseEntity.ok(appointmentRepository.save(rdv));
        }
    }

}