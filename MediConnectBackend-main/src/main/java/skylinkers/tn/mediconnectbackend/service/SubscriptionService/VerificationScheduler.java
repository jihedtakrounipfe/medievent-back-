package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationScheduler {

    private final StudentVerificationService studentVerificationService;

        /**
         * Runs periodically to process pending student verifications.
         * Default is every 60s to keep student-facing progress responsive.
         */
        @Scheduled(
            fixedRateString = "${student.verification.scheduler.fixed-rate-ms:60000}",
            initialDelayString = "${student.verification.scheduler.initial-delay-ms:15000}"
        )
    public void processPendingVerifications() {
        log.info("Starting scheduled verification processing task");
        try {
            studentVerificationService.processPendingVerifications();
            log.info("Verification processing task completed successfully");
        } catch (Exception e) {
            log.error("Error during scheduled verification processing: {}", e.getMessage(), e);
        }
    }
}
