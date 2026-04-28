package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.PatientPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientPlanRepository extends JpaRepository<PatientPlan, Long> {

    Optional<PatientPlan> findByNameIgnoreCase(String name);

    List<PatientPlan> findByIsActiveTrue();

    boolean existsByNameIgnoreCase(String name);

}