package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.DoctorPlan;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorPlanRepository extends JpaRepository<DoctorPlan, Long> {

    Optional<DoctorPlan> findByNameIgnoreCase(String name);

    List<DoctorPlan> findByIsActiveTrue();

    boolean existsByNameIgnoreCase(String name);

}