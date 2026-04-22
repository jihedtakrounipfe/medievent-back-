package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.EventParticipant;
import skylinkers.tn.mediconnectbackend.entities.enums.ParticipantStatus;
import skylinkers.tn.mediconnectbackend.entities.MedicalEvent;
import skylinkers.tn.mediconnectbackend.entities.AppUser;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventParticipantRepository extends JpaRepository<EventParticipant, Long> {
    List<EventParticipant> findByEvent(MedicalEvent event);
    List<EventParticipant> findByUser(AppUser user);
    List<EventParticipant> findByUserAndStatus(AppUser user, ParticipantStatus status);
    Optional<EventParticipant> findByEventAndUser(MedicalEvent event, AppUser user);
    boolean existsByEventAndUser(MedicalEvent event, AppUser user);
    
    long countByEventAndStatus(MedicalEvent event, ParticipantStatus status);
    Optional<EventParticipant> findFirstByEventAndStatusOrderByCreatedAtAsc(MedicalEvent event, ParticipantStatus status);
}
