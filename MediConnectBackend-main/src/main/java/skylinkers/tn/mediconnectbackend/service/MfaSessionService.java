package skylinkers.tn.mediconnectbackend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory MFA session store.
 *
 * After a user successfully authenticates with their password, a short-lived
 * MFA session is created. The session tracks which methods are enabled and
 * how many attempts remain per method. It also holds the Keycloak tokens so
 * they can be returned on successful MFA completion without re-calling Keycloak.
 *
 * TTL: 5 minutes. Sessions are cleaned up by a scheduled task.
 */
@Slf4j
@Service
public class MfaSessionService {

    private static final long TTL_SECONDS = 300; // 5 minutes

    public record MfaSessionData(
            String email,
            List<String> enabledMethods,       // ["FACE", "TOTP", "EMAIL"]
            Map<String, Integer> attemptsRemaining, // {"FACE": 3, "TOTP": 3, "EMAIL": 3}
            boolean emailOtpSent,
            String accessToken,
            String refreshToken,
            String tokenType,
            Long expiresIn,
            Instant expiresAt,
            Long riskAuditLogId   // AuditLog row written by the risk scorer on credential step
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public boolean allExhausted() {
            return attemptsRemaining.values().stream().allMatch(v -> v <= 0);
        }

        /** Returns the highest-priority method that still has attempts remaining. */
        public Optional<String> nextAvailableMethod() {
            List<String> priority = List.of("FACE", "TOTP", "EMAIL");
            return priority.stream()
                    .filter(m -> enabledMethods.contains(m))
                    .filter(m -> attemptsRemaining.getOrDefault(m, 0) > 0)
                    .findFirst();
        }
    }

    private final ConcurrentHashMap<String, MfaSessionData> sessions = new ConcurrentHashMap<>();

    /** Create a new MFA session. Returns the session token (UUID). */
    public String create(String email,
                         List<String> enabledMethods,
                         boolean emailOtpSent,
                         String accessToken,
                         String refreshToken,
                         String tokenType,
                         Long expiresIn,
                         Long riskAuditLogId) {
        String token = UUID.randomUUID().toString();
        Map<String, Integer> attempts = new HashMap<>();
        for (String m : enabledMethods) attempts.put(m, 3);
        sessions.put(token, new MfaSessionData(
                email, new ArrayList<>(enabledMethods), attempts,
                emailOtpSent, accessToken, refreshToken, tokenType, expiresIn,
                Instant.now().plusSeconds(TTL_SECONDS), riskAuditLogId
        ));
        return token;
    }

    /** Look up a session. Returns empty if not found or expired. */
    public Optional<MfaSessionData> get(String token) {
        MfaSessionData data = sessions.get(token);
        if (data == null || data.isExpired()) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(data);
    }

    /** Decrement the attempt counter for a method. Returns the updated session. */
    public Optional<MfaSessionData> decrementAttempt(String token, String method) {
        return get(token).map(data -> {
            Map<String, Integer> updated = new HashMap<>(data.attemptsRemaining());
            updated.merge(method, 0, (old, z) -> Math.max(0, old - 1));
            MfaSessionData updatedData = new MfaSessionData(
                    data.email(), data.enabledMethods(), updated,
                    data.emailOtpSent(), data.accessToken(), data.refreshToken(),
                    data.tokenType(), data.expiresIn(), data.expiresAt(),
                    data.riskAuditLogId()
            );
            sessions.put(token, updatedData);
            return updatedData;
        });
    }

    /** Mark email OTP as sent (so resend logic works). */
    public void markEmailOtpSent(String token) {
        get(token).ifPresent(data -> {
            MfaSessionData updated = new MfaSessionData(
                    data.email(), data.enabledMethods(), data.attemptsRemaining(),
                    true, data.accessToken(), data.refreshToken(),
                    data.tokenType(), data.expiresIn(), data.expiresAt(),
                    data.riskAuditLogId()
            );
            sessions.put(token, updated);
        });
    }

    /** Consume (delete) a session on successful MFA. */
    public void consume(String token) {
        sessions.remove(token);
    }

    /** Scheduled cleanup — runs every minute. */
    @Scheduled(fixedDelay = 60_000)
    public void cleanup() {
        int before = sessions.size();
        sessions.entrySet().removeIf(e -> e.getValue().isExpired());
        int removed = before - sessions.size();
        if (removed > 0) log.debug("[MFA-SESSION] Cleaned up {} expired sessions", removed);
    }
}
