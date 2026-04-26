package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.MedicalEvent;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MedicalEventRepository extends JpaRepository<MedicalEvent, Long> {
    List<MedicalEvent> findAllByStatus(EventStatus status);
    List<MedicalEvent> findAllByOrganizerId(Long doctorId);
    long countByOrganizerAndEventDateBetween(skylinkers.tn.mediconnectbackend.entities.Doctor organizer, LocalDateTime start, LocalDateTime end);

    /** Events that start between [windowStart, windowEnd] and haven't had their reminder sent yet. */
    @Query("SELECT e FROM MedicalEvent e WHERE e.status = 'APPROVED' AND e.reminderSent = false AND e.eventDate BETWEEN :windowStart AND :windowEnd")
    List<MedicalEvent> findEventsNeedingReminder(@Param("windowStart") LocalDateTime windowStart,
                                                  @Param("windowEnd") LocalDateTime windowEnd);

    /** Events that started between [windowStart, windowEnd] and haven't had the start notification sent yet. */
    @Query("SELECT e FROM MedicalEvent e WHERE e.status = 'APPROVED' AND e.startedNotificationSent = false AND e.eventDate BETWEEN :windowStart AND :windowEnd")
    List<MedicalEvent> findEventsNeedingStartNotification(@Param("windowStart") LocalDateTime windowStart,
                                                           @Param("windowEnd") LocalDateTime windowEnd);
}
