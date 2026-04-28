package skylinkers.tn.mediconnectbackend.repository.AppointmentRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import skylinkers.tn.mediconnectbackend.entities.Consultation;

public interface ConsultationRepository extends JpaRepository<Consultation,Long> {

}
