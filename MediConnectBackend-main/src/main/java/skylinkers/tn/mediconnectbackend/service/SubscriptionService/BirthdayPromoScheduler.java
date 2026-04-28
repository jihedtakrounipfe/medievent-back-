package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.PromoCode;
import skylinkers.tn.mediconnectbackend.entities.Subscription;
import skylinkers.tn.mediconnectbackend.entities.enums.*;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.BirthdayUserRepository;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.PromoCodeRepository;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.SubscriptionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class BirthdayPromoScheduler {

    @Autowired private BirthdayUserRepository birthdayUserRepository;
    @Autowired private PromoCodeRepository promoCodeRepository;
    @Autowired private SubscriptionEmailService emailService;
    @Autowired private SubscriptionRepository subscriptionRepository;

    @Scheduled(cron = "0 * * * * *")
    public void sendBirthdayPromos() {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int day = today.getDayOfMonth();

        List<AppUser> users = birthdayUserRepository.findByBirthMonthAndDay(month, day);

        for (AppUser user : users) {
            try {
                // Skip admins
                if (user.getUserType() == null || user.getUserType() == UserType.ADMINISTRATOR) continue;

                Optional<Subscription> activeSub = subscriptionRepository
                        .findByUser_IdAndStatus(user.getId(), SubscriptionStatus.ACTIVE);

                // Skip users on top tier — discount is useless
                boolean isOnTopTierPlan = activeSub.map(sub -> {
                    if (sub.getPatientPlan() != null) {
                        return sub.getPatientPlan().getName().toString().equalsIgnoreCase("PREMIUM");
                    }
                    if (sub.getDoctorPlan() != null) {
                        return sub.getDoctorPlan().getName().toString().equalsIgnoreCase("GOLD");
                    }
                    return false;
                }).orElse(false);

                if (isOnTopTierPlan) continue;

                boolean hasActiveSub = activeSub.isPresent();

                String keycloakPrefix = (user.getKeycloakId() != null && user.getKeycloakId().length() >= 6)
                        ? user.getKeycloakId().substring(0, 6).toUpperCase()
                        : String.valueOf(user.getId()).replace("-", "").substring(0, 6).toUpperCase();

                String code = "BDAY-" + keycloakPrefix + "-" + today.getYear();

                if (promoCodeRepository.existsByCode(code)) continue;

                PromoCode promo = new PromoCode();
                promo.setCode(code);
                promo.setDescription("🎂 Birthday gift - auto generated");
                promo.setDiscountType(DiscountType.PERCENTAGE);
                promo.setDiscountValue(BigDecimal.valueOf(15));
                promo.setStartDate(today);
                promo.setEndDate(today.plusDays(7));
                promo.setIsActive(true);
                promo.setMaxUsesTotal(1);
                promo.setMaxUsesPerUser(1);
                promo.setCurrentUseCount(0);
                promo.setPlanType(PromoCodePlanType.valueOf(user.getUserType().name()));
                promo.setPlanName("ALL");
                promo.setBillingCycle(PromoCodeBillingCycle.BOTH);
                promo.setMinPurchaseAmount(BigDecimal.ZERO);
                promo.setCreatedByAdmin(null);

                promoCodeRepository.save(promo);

                // Only email if not currently subscribed
                if (!hasActiveSub) {
                    emailService.sendBirthdayPromoEmail(user, code);
                }

            } catch (Exception e) {
                System.err.println("Failed birthday promo for user: " + user.getId() + " — " + e.getMessage());
            }
        }
    }
}