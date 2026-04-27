package skylinkers.tn.mediconnectbackend.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import skylinkers.tn.mediconnectbackend.entities.*;
import skylinkers.tn.mediconnectbackend.entities.enums.*;
import skylinkers.tn.mediconnectbackend.repository.*;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.*;
import skylinkers.tn.mediconnectbackend.service.impl.MedicalEventServiceImpl;
import skylinkers.tn.mediconnectbackend.dto.MedicalEventDTO;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final MedicalEventServiceImpl eventService; // Use the implementation directly for the test

    @Override
    public void run(String... args) {
        log.info("[STARTUP] Triggering Final Test for takrounipc@gmail.com");

        try {
            AppUser user = userRepository.findByEmail("takrounipc@gmail.com")
                    .orElseGet(() -> {
                        Patient p = Patient.builder()
                                .email("takrounipc@gmail.com")
                                .firstName("Jihed")
                                .lastName("Takrouni")
                                .keycloakId("DUMMY_JIHED")
                                .isActive(true).isVerified(true)
                                .build();
                        p.setUserType(UserType.PATIENT);
                        return userRepository.save(p);
                    });

            // 1. Set Interest
            user.setInterests(Set.of("Innovation Medicale 2026"));
            userRepository.save(user);

            // 2. Mock Event
            MedicalEventDTO testEvent = MedicalEventDTO.builder()
                .title("EVENT TEST: Innovation Medicale 2026")
                .description("Ceci est un test final de notification par email.")
                .eventDate(LocalDateTime.now().plusDays(5))
                .location("Virtuel")
                .targetAudience(EventAudience.PUBLIC)
                .tags(Set.of("Innovation Medicale 2026"))
                .maxParticipants(100)
                .build();

            // 3. Trigger!
            eventService.createEvent(testEvent, "house@mediconnect.tn");
            log.info("✅ SUCCESS: Test email triggered for takrounipc@gmail.com!");

        } catch (Exception e) {
            log.error("❌ ERROR during startup test: {}", e.getMessage());
        }
    }
}
