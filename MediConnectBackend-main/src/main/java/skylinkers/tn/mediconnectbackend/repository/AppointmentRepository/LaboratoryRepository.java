package skylinkers.tn.mediconnectbackend.repository.AppointmentRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import skylinkers.tn.mediconnectbackend.entities.Laboratory;

public interface LaboratoryRepository extends JpaRepository<Laboratory,Long> {
}
