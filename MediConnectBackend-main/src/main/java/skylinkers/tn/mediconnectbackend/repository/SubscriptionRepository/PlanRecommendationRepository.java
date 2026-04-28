package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.PlanRecommendation;

import java.util.List;

@Repository
public interface PlanRecommendationRepository extends JpaRepository<PlanRecommendation, Long> {
    List<PlanRecommendation> findByUserId(Long userId);
}