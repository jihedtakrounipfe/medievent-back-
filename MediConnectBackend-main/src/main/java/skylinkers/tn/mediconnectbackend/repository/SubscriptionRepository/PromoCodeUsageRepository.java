package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.PromoCode;
import skylinkers.tn.mediconnectbackend.entities.PromoCodeUsage;
import skylinkers.tn.mediconnectbackend.entities.AppUser;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromoCodeUsageRepository extends JpaRepository<PromoCodeUsage, Long> {
    Optional<PromoCodeUsage> findByPromoCodeAndUser(PromoCode promoCode, AppUser user);
    List<PromoCodeUsage> findByPromoCode(PromoCode promoCode);
    List<PromoCodeUsage> findByUser(AppUser user);
    boolean existsByPromoCodeAndUser(PromoCode promoCode, AppUser user);
    long countByPromoCodeAndUser(PromoCode promoCode, AppUser user);
    
    // Find usages by promo code name for student promo exclusivity checks
    List<PromoCodeUsage> findByPromoCode_Code(String code);
}