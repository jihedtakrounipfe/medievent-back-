package skylinkers.tn.mediconnectbackend.exception;

/**
 * Thrown when a user has exceeded the maximum number of consecutive
 * failed login attempts and their account is temporarily blocked.
 */
public class TooManyAttemptsException extends RuntimeException {

    private final long remainingSeconds;

    public TooManyAttemptsException(long remainingSeconds) {
        super("Trop de tentatives de connexion. Réessayez dans " + remainingSeconds + " secondes.");
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
