package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

/**
 * Bridges the Spring domain layer with the external face-recognition-service
 * (Python/FastAPI microservice using DeepFace/FaceNet).
 *
 * SOLID — DIP: Controllers and other services depend on this interface.
 *              The actual HTTP call to the Python microservice is encapsulated
 *              in BiometricServiceImpl — callers are decoupled from transport.
 */
public interface BiometricService {

    /** Enrolls a user by sending captured photos to the recognition microservice. */
    void enrollBiometric(Long userId, byte[] photoBytes);

    /**
     * Verifies a live photo against the stored embedding.
     * @return true if cosine similarity > 0.85 and liveness check passes
     */
    boolean verifyBiometric(Long userId, byte[] photoBytes);

    /** RGPD: Soft-revokes the embedding and resets the enrollment flag. */
    void revokeBiometric(Long userId);

    boolean isEnrolled(Long userId);
}

