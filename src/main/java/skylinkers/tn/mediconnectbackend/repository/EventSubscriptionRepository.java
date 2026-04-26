package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.EventSubscription;
import skylinkers.tn.mediconnectbackend.entities.MedicalEvent;
import java.util.List;
import java.util.Optional;

public interface EventSubscriptionRepository extends JpaRepository<EventSubscription, Long> {
    Optional<EventSubscription> findByUserAndEvent(AppUser user, MedicalEvent event);
    List<EventSubscription> findByEvent(MedicalEvent event);
    boolean existsByUserAndEvent(AppUser user, MedicalEvent event);
}
