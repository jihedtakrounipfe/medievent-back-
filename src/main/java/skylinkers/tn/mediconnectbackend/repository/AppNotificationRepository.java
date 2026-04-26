package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import skylinkers.tn.mediconnectbackend.entities.AppNotification;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import java.util.List;

public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {
    List<AppNotification> findByUserOrderByCreatedAtDesc(AppUser user);
    long countByUserAndReadFalse(AppUser user);
    boolean existsByEventIdAndType(Long eventId, String type);
}
