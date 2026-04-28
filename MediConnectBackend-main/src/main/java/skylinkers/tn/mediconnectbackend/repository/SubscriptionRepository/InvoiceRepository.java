package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.Invoice;

import java.util.Optional;

/**
 * 
 */
@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    /**
     * 
     * @param subscriptionId
     * @return 
     */
    Optional<Invoice> findTopBySubscription_IdOrderByCreatedAtDesc(Long subscriptionId);
}