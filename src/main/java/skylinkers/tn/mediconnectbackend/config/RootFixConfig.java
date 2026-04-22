package skylinkers.tn.mediconnectbackend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;
import skylinkers.tn.mediconnectbackend.repository.MedicalEventRepository;

@Configuration
@RequiredArgsConstructor
public class RootFixConfig {

    private final MedicalEventRepository eventRepository;

    @Bean
    public CommandLineRunner approveAllExistingEvents() {
        return args -> {
            System.out.println("[ROOT-FIX] Auto-approving all existing PENDING events...");
            eventRepository.findAll().forEach(event -> {
                if (event.getStatus() == EventStatus.PENDING) {
                    event.setStatus(EventStatus.APPROVED);
                    eventRepository.save(event);
                    System.out.println("[ROOT-FIX] Event '" + event.getTitle() + "' approved.");
                }
            });
            System.out.println("[ROOT-FIX] Cleanup complete.");
        };
    }
}
