package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.StudentVerification;
import skylinkers.tn.mediconnectbackend.entities.enums.SubVerificationStatus;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.StudentVerificationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationExpiryScheduler {

    private final StudentVerificationRepository verificationRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireApprovedVerifications() {
        log.info("Running verification expiry check at {}", LocalDateTime.now());
        LocalDateTime now = LocalDateTime.now();
        List<StudentVerification> expiredVerifications = verificationRepository
                .findByStatusAndExpiresAtBefore(SubVerificationStatus.APPROVED, now);

        if (expiredVerifications.isEmpty()) {
            return;
        }

        for (StudentVerification verification : expiredVerifications) {
            verification.setStatus(SubVerificationStatus.EXPIRED);
        }

        verificationRepository.saveAll(expiredVerifications);
        log.info("Expired {} student verification(s)", expiredVerifications.size());
    }
}
