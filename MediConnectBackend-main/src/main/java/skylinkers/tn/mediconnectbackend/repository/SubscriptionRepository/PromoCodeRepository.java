package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.PromoCode;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCode, Long> {
    Optional<PromoCode> findByCode(String code);
    Optional<PromoCode> findByCodeIgnoreCase(String code);
    List<PromoCode> findByIsActiveTrue();
    List<PromoCode> findByCreatedByAdmin_Id(Long adminId);
    List<PromoCode> findByEndDateAfter(LocalDate date);
    boolean existsByCode(String code);
}