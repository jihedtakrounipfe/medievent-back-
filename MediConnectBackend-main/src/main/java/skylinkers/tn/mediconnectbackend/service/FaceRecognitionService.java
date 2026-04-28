package skylinkers.tn.mediconnectbackend.service;

import skylinkers.tn.mediconnectbackend.entities.AppUser;

public interface FaceRecognitionService {

    void enrollFace(AppUser user, byte[] imageData);

    FaceVerificationResult verifyFace(AppUser user, byte[] imageData);

    void deleteFaceTemplate(AppUser user);

    boolean isEnrolled(AppUser user);

    record FaceVerificationResult(boolean matched, Double similarity, String message) {}
}
