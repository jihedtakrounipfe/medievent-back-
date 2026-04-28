package skylinkers.tn.mediconnectbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory login attempt tracker.
 *
 * Rules:
 *   - After 5 consecutive failures for the same email, block for 90 seconds.
 *   - The counter resets on successful login.
 *   - No external cache dependency — ConcurrentHashMap is sufficient for a
 *     single-node deployment. For clustered deploys, swap with Redis.
 */
@Slf4j
@Service
public class LoginAttemptService {

    private static final int  MAX_ATTEMPTS   = 5;
    private static final long BLOCK_DURATION = 90L; // seconds

    private record AttemptEntry(int count, Instant blockedUntil) {}

    private final ConcurrentHashMap<String, AttemptEntry> attempts = new ConcurrentHashMap<>();

    /**
     * Records a failed login attempt. Blocks the account after MAX_ATTEMPTS failures.
     */
    public void loginFailed(String email) {
        String key = normalise(email);
        attempts.compute(key, (k, existing) -> {
            int count = (existing == null ? 0 : existing.count()) + 1;
            Instant blockedUntil = (count >= MAX_ATTEMPTS)
                    ? Instant.now().plusSeconds(BLOCK_DURATION)
                    : (existing != null ? existing.blockedUntil() : null);
            log.warn("[LOGIN-LIMIT] Failed attempt #{} for {}", count, key);
            return new AttemptEntry(count, blockedUntil);
        });
    }

    /**
     * Returns true if the account is currently blocked.
     */
    public boolean isBlocked(String email) {
        AttemptEntry entry = attempts.get(normalise(email));
        if (entry == null || entry.blockedUntil() == null) return false;
        if (Instant.now().isAfter(entry.blockedUntil())) {
            // Block expired — clean up
            attempts.remove(normalise(email));
            return false;
        }
        return true;
    }

    /**
     * Returns the number of seconds remaining in the current block (0 if not blocked).
     */
    public long getRemainingBlockSeconds(String email) {
        AttemptEntry entry = attempts.get(normalise(email));
        if (entry == null || entry.blockedUntil() == null) return 0;
        long remaining = entry.blockedUntil().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(0, remaining);
    }

    /**
     * Clears the attempt record after a successful login.
     */
    public void resetAttempts(String email) {
        attempts.remove(normalise(email));
    }

    private String normalise(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
