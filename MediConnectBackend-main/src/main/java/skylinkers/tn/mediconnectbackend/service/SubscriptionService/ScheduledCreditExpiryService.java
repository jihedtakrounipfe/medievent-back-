package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.UserCredit;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.UserCreditRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledCreditExpiryService {

    private final UserCreditRepository userCreditRepository;
    private final AppUserRepository appUserRepository;
    private final SubscriptionEmailService subscriptionEmailService;

    /**
     * Run daily at midnight to expire credits
     * Cron: 0 0 0 * * * (midnight every day)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireCredits() {
        log.info("Starting scheduled credit expiry job...");
        
        LocalDate today = LocalDate.now();
        
        // Find all credits that have expired but still have balance
        List<UserCredit> expiredCredits = userCreditRepository
                .findByExpiresAtBeforeAndBalanceGreaterThan(today, BigDecimal.ZERO);
        
        if (expiredCredits.isEmpty()) {
            log.info("No expired credits found. Job completed.");
            return;
        }
        
        log.info("Found {} expired credits to process", expiredCredits.size());
        
        for (UserCredit credit : expiredCredits) {
            try {
                BigDecimal expiredAmount = credit.getBalance();
                
                // Zero out the balance
                credit.setBalance(BigDecimal.ZERO);
                userCreditRepository.save(credit);
                
                // Send email notification
                var user = appUserRepository.findById(credit.getUserId()).orElse(null);
                if (user != null) {
                    subscriptionEmailService.sendCreditExpiryNotification(user.getEmail(), expiredAmount);
                    log.info("Credit expiry notification sent to user {} for amount {}", credit.getUserId(), expiredAmount);
                } else {
                    log.warn("User not found for credit expiry notification: {}", credit.getUserId());
                }
                
                log.info("Expired credit {} for user {} with amount {}", credit.getId(), credit.getUserId(), expiredAmount);
            } catch (Exception ex) {
                log.error("Failed to process expired credit {}", credit.getId(), ex);
            }
        }
        
        log.info("Credit expiry job completed. Processed {} credits.", expiredCredits.size());
    }
}

