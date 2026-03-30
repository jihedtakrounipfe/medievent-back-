package skylinkers.tn.mediconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.VerificationCode;
import skylinkers.tn.mediconnectbackend.repository.VerificationCodeRepository;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final SecureRandom RNG = new SecureRandom();

    private final VerificationCodeRepository repository;
    private final EmailService emailService;

    /**
     * Generates a random 6-digit code, stores it with 10-minute expiry, and sends it by email.
     */
    @Transactional
    public void generateAndSend(String email, String firstName) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        long sentLastHour = repository.countByEmailAndCreatedAtAfter(normalizedEmail, LocalDateTime.now().minusHours(1));
        if (sentLastHour >= 3) {
            throw new IllegalArgumentException("Resend limit exceeded. Please try again later.");
        }

        repository.findByEmailAndUsedFalse(normalizedEmail).forEach(c -> c.setUsed(true));

        VerificationCode code = new VerificationCode();
        code.setEmail(normalizedEmail);
        code.setCode(randomSixDigits());
        code.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        repository.save(code);

        emailService.sendVerificationCode(normalizedEmail, firstName == null ? "" : firstName, code.getCode());
    }

    /**
     * Verifies a code. Marks it as used if valid.
     */
    @Transactional
    public boolean verifyCode(String email, String code) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        String normalizedCode = code == null ? "" : code.trim();
        return repository.findByEmailAndCodeAndUsedFalseAndExpiresAtAfter(normalizedEmail, normalizedCode, LocalDateTime.now())
                .map(vc -> {
                    vc.setUsed(true);
                    return true;
                })
                .orElse(false);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupExpired() {
        try {
            repository.deleteByExpiresAtBefore(LocalDateTime.now());
        } catch (Exception e) {
            log.warn("Verification code cleanup failed", e);
        }
    }

    private String randomSixDigits() {
        int n = RNG.nextInt(1_000_000);
        return String.format("%06d", n);
    }
}

