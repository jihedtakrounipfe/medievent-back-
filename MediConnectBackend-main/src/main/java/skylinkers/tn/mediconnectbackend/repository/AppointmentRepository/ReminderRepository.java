package skylinkers.tn.mediconnectbackend.repository.AppointmentRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import skylinkers.tn.mediconnectbackend.entities.Reminder;

public interface ReminderRepository extends JpaRepository<Reminder,Long> {



}
