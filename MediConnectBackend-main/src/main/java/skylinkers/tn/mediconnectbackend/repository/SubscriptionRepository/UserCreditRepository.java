package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.UserCredit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCreditRepository extends JpaRepository<UserCredit, UUID> {

    // Find credit balance for a user
    Optional<UserCredit> findByUserId(Long userId);

    // Find all expired credits (for scheduled job)
    List<UserCredit> findByExpiresAtBeforeAndBalanceGreaterThan(LocalDate date, java.math.BigDecimal balance);

    // Check if user has valid (non-expired) credit
    Optional<UserCredit> findByUserIdAndExpiresAtGreaterThanEqual(Long userId, LocalDate date);
}
