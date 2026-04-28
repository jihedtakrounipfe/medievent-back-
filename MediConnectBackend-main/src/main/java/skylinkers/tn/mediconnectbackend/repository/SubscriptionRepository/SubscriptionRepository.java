package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Subscription;
import skylinkers.tn.mediconnectbackend.entities.enums.SubscriptionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Get active subscription for a user
    Optional<Subscription> findByUser_IdAndStatus(Long userId, SubscriptionStatus status);

    // Get all subscriptions for a user
    List<Subscription> findByUser_Id(Long userId);
    List<Subscription> findByUser_IdAndCancelledAtIsNotNullOrderByCancelledAtDesc(Long userId);

    // Get all subscriptions that have cancellation category metadata
    List<Subscription> findByCancellationCategoryIsNotNull();

    // Find expired subscriptions — for the scheduled job
    List<Subscription> findByStatusAndEndDateBefore(SubscriptionStatus status, LocalDate date);

    // Check if user already has an active subscription
    boolean existsByUserAndStatus(AppUser user, SubscriptionStatus status);

    boolean existsByPatientPlan_Id(Long patientPlanId);

    boolean existsByDoctorPlan_Id(Long doctorPlanId);

    // Find active subscriptions with auto-renewal for a patient plan
    List<Subscription> findByPatientPlan_IdAndStatusAndAutoRenewTrue(Long patientPlanId, SubscriptionStatus status);

    // Find active subscriptions with auto-renewal for a doctor plan
    List<Subscription> findByDoctorPlan_IdAndStatusAndAutoRenewTrue(Long doctorPlanId, SubscriptionStatus status);

    // Find ALL active subscriptions for a patient plan (regardless of autoRenew)
    List<Subscription> findByPatientPlan_IdAndStatus(Long patientPlanId, SubscriptionStatus status);

    // Find ALL active subscriptions for a doctor plan (regardless of autoRenew)
    List<Subscription> findByDoctorPlan_IdAndStatus(Long doctorPlanId, SubscriptionStatus status);

    List<Subscription> findByPatientPlan_Id(Long planId);
    List<Subscription> findByDoctorPlan_Id(Long planId);
}