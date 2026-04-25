package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.MedicalEvent;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;

import java.util.List;

@Repository
public interface MedicalEventRepository extends JpaRepository<MedicalEvent, Long> {
    List<MedicalEvent> findAllByStatus(EventStatus status);
    List<MedicalEvent> findAllByOrganizerId(Long doctorId);
    long countByOrganizerAndEventDateBetween(skylinkers.tn.mediconnectbackend.entities.Doctor organizer, java.time.LocalDateTime start, java.time.LocalDateTime end);
}
