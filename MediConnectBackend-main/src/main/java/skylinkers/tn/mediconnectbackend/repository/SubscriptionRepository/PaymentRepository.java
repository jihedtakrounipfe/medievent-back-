package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Payment;
import skylinkers.tn.mediconnectbackend.entities.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByStripeSessionId(String stripeSessionId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.stripeSessionId = :stripeSessionId")
    Optional<Payment> findByStripeSessionIdForUpdate(String stripeSessionId);
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);
    List<Payment> findByUserId(Long userId);
    List<Payment> findByUserIdAndCreditAppliedGreaterThanOrderByCreatedAtDesc(Long userId, BigDecimal minimumCredit);
    List<Payment> findByUserIdAndStatus(Long userId, PaymentStatus status);
    boolean existsByPatientPlan_Id(Long patientPlanId);
    boolean existsByDoctorPlan_Id(Long doctorPlanId);
    @Query(value = "SELECT p.* FROM payments p JOIN users u ON u.id = p.user_id WHERE u.email = :email ORDER BY p.created_at DESC LIMIT 1", nativeQuery = true)
    Optional<Payment> findTopByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    Optional<Payment> findTopBySubscription_IdOrderByCreatedAtDesc(Long subscriptionId);
    List<Payment> findByUserIdAndStripeSessionIdStartingWith(Long userId, String prefix);
}
