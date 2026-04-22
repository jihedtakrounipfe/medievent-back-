package skylinkers.tn.mediconnectbackend.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import skylinkers.tn.mediconnectbackend.entities.*;
import skylinkers.tn.mediconnectbackend.entities.enums.*;
import skylinkers.tn.mediconnectbackend.repository.*;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.*;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final MedicalEventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final skylinkers.tn.mediconnectbackend.service.security.KeycloakAdminClient keycloakAdminClient;

    private static final String TEST_PASSWORD = "Password123";

    @Override
    public void run(String... args) {
        log.info("[QUICK_INIT] Generating 3 confirmed spots + your waiting list spot...");

        try {
            // 1. Create Organizer
            Doctor organizer = (Doctor) ensureUser("house@mediconnect.tn", "House", "Gregory", UserType.DOCTOR);

            // 2. Create Event (Max 3)
            MedicalEvent event = createEventIfNotExists("L'IA en Médecine 2026", organizer, 3);

            // 3. Create 3 Confirmed Patients
            Patient p1 = (Patient) ensureUser("test_user_a@test.com", "TestA", "User", UserType.PATIENT);
            Patient p2 = (Patient) ensureUser("test_user_b@test.com", "TestB", "User", UserType.PATIENT);
            Patient p3 = (Patient) ensureUser("test_user_c@test.com", "TestC", "User", UserType.PATIENT);

            // 4. Create your testing account (but do not register to event)
            ensureUser("takrounipc@gmail.com", "Takrouni", "Jihed", UserType.PATIENT);

            // 5. Fill indices
            registerParticipant(event, p1);
            registerParticipant(event, p2);
            registerParticipant(event, p3);
            // DO NOT register takrounipc@gmail.com so the user can click 'S'inscrire Maintenant'

            log.info("[SUCCESS] Accounts created. PASSWORD: {}", TEST_PASSWORD);
            log.info(" -> Test accounts: test_user_a@test.com, test_user_b@test.com, test_user_c@test.com");
            log.info(" -> Your account: takrounipc@gmail.com");
        } catch (Exception e) {
            log.warn("[INIT_ERROR] Keycloak connection failed? Creating local-only stubs. {}", e.getMessage());
            // Fallback for local-only if Keycloak is down
            Doctor doc = createDoctorIfNotExists("House", "Gregory", "house@mediconnect.tn");
            MedicalEvent event = createEventIfNotExists("L'IA en Médecine 2026", doc, 3);
            registerParticipant(event, createPatientIfNotExists("User", "TestA", "test_user_a@test.com"));
            registerParticipant(event, createPatientIfNotExists("User", "TestB", "test_user_b@test.com"));
            registerParticipant(event, createPatientIfNotExists("User", "TestC", "test_user_c@test.com"));
            createPatientIfNotExists("Takrouni", "Jihed", "takrounipc@gmail.com");
        }
    }

    private AppUser ensureUser(String email, String last, String first, UserType type) {
        Optional<? extends AppUser> existing = (type == UserType.DOCTOR) ? doctorRepository.findByEmail(email) : patientRepository.findByEmail(email);
        if (existing.isPresent()) return existing.get();

        String kId;
        if (type == UserType.DOCTOR) {
            kId = keycloakAdminClient.createDoctorUser(email, TEST_PASSWORD, first, last, "RPPS-123");
            return doctorRepository.save(Doctor.builder().keycloakId(kId).email(email).lastName(last).firstName(first).isActive(true).isVerified(true).specialization(Specialization.GENERAL_PRACTICE).build());
        } else {
            kId = keycloakAdminClient.createPatientUser(email, TEST_PASSWORD, first, last);
            return patientRepository.save(Patient.builder().keycloakId(kId).email(email).lastName(last).firstName(first).isActive(true).isVerified(true).build());
        }
    }

    private Doctor createDoctorIfNotExists(String lastName, String firstName, String email) {
        return doctorRepository.findByEmail(email).orElseGet(() -> {
            Doctor d = Doctor.builder()
                    .lastName(lastName).firstName(firstName).email(email)
                    .keycloakId("DUMMY_" + email)
                    .isActive(true).isVerified(true)
                    .specialization(Specialization.GENERAL_PRACTICE)
                    .build();
            return doctorRepository.save(d);
        });
    }

    private Patient createPatientIfNotExists(String lastName, String firstName, String email) {
        return patientRepository.findByEmail(email).orElseGet(() -> {
            Patient p = Patient.builder()
                    .lastName(lastName).firstName(firstName).email(email)
                    .keycloakId("DUMMY_" + email)
                    .isActive(true).isVerified(true)
                    .build();
            return patientRepository.save(p);
        });
    }

    private MedicalEvent createEventIfNotExists(String title, Doctor organizer, int max) {
        return eventRepository.findAllByOrganizerId(organizer.getId()).stream()
                .filter(e -> e.getTitle().equals(title)).findFirst()
                .orElseGet(() -> {
                   MedicalEvent e = MedicalEvent.builder()
                           .title(title)
                           .description("Une conférence exceptionnelle sur le futur de la cardiologie et des technologies médicales.")
                           .eventDate(LocalDateTime.now().plusDays(5))
                           .location("SALLE_VIRTUELLE_INTERNE")
                           .targetAudience(EventAudience.PUBLIC)
                           .status(EventStatus.APPROVED)
                           .organizer(organizer)
                           .maxParticipants(max)
                           .build();
                   return eventRepository.save(e);
                });
    }

    private void registerParticipant(MedicalEvent event, AppUser user) {
        if (!participantRepository.existsByEventAndUser(event, user)) {
            long confirmedCount = participantRepository.countByEventAndStatus(event, ParticipantStatus.CONFIRMED);
            ParticipantStatus status = (confirmedCount < event.getMaxParticipants()) 
                    ? ParticipantStatus.CONFIRMED 
                    : ParticipantStatus.WAITING_LIST;

            EventParticipant ep = EventParticipant.builder()
                    .event(event).user(user)
                    .role(ParticipantRole.PARTICIPANT)
                    .status(status)
                    .build();
            participantRepository.save(ep);
        }
    }
}
