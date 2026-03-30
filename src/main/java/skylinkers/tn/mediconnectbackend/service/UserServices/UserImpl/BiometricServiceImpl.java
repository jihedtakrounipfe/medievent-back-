package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.BiometricData;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.exception.BiometricException;
import skylinkers.tn.mediconnectbackend.exception.ResourceNotFoundException;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.BiometricDataRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.PatientRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.BiometricService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

/**
 * Delegates the actual recognition work to the Python face-recognition-service
 * via an internal REST call. Only the resulting embedding (returned by the
 * microservice as a Base64 string) is stored — raw photos are never persisted.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BiometricServiceImpl implements BiometricService {

    private final BiometricDataRepository biometricDataRepository;
    private final PatientRepository       patientRepository;
    private final AppUserRepository       appUserRepository;
    private final AuditLogService         auditLogService;
    private final RestTemplate            restTemplate;

    @Value("${mediconnect.face-recognition.service-url}")
    private String faceServiceUrl;

    @Override
    @Transactional
    public void enrollBiometric(Long userId, byte[] photoBytes) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        // Deactivate any existing embedding before re-enrollment
        if (biometricDataRepository.existsByUserIdAndIsActiveTrue(userId)) {
            biometricDataRepository.deactivateByUserId(userId);
        }

        // Call external face-recognition-service to generate embedding
        String embeddingBase64 = callFaceService("/enroll", photoBytes);

        BiometricData data = BiometricData.builder()
                .user(user)
                .embeddingVector(embeddingBase64)
                .isActive(true)
                .build();
        biometricDataRepository.save(data);

        // Flag enrollment on the user (Patient or Doctor)
        flagBiometricEnrolled(userId, true);

        auditLogService.log(user, "BIOMETRIC_ENROLL", null, null, true, null);
        log.info("Biometric enrolled for userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyBiometric(Long userId, byte[] photoBytes) {
        BiometricData stored = biometricDataRepository
                .findByUserIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new BiometricException("No active biometric enrollment for userId=" + userId));

        // Build payload for the face-recognition-service
        String photoBase64 = Base64.getEncoder().encodeToString(photoBytes);
        Map<String, String> payload = Map.of(
                "photo",    photoBase64,
                "embedding", stored.getEmbeddingVector()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    faceServiceUrl + "/verify", request, Map.class);
            boolean matched = Boolean.TRUE.equals(response.getBody().get("match"));

            AppUser user = appUserRepository.findById(userId).orElse(null);
            auditLogService.log(user, "BIOMETRIC_VERIFY", null, null, matched,
                    matched ? null : "Similarity below threshold");
            return matched;
        } catch (Exception e) {
            log.error("Face recognition service error for userId={}: {}", userId, e.getMessage());
            throw new BiometricException("Biometric verification service unavailable");
        }
    }

    @Override
    @Transactional
    public void revokeBiometric(Long userId) {
        biometricDataRepository.deactivateByUserId(userId);
        flagBiometricEnrolled(userId, false);

        AppUser user = appUserRepository.findById(userId).orElse(null);
        auditLogService.log(user, "BIOMETRIC_REVOKE", null, null, true, "RGPD erasure");
        log.warn("Biometric revoked for userId={}", userId);
    }

    @Override
    public boolean isEnrolled(Long userId) {
        return biometricDataRepository.existsByUserIdAndIsActiveTrue(userId);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private String callFaceService(String path, byte[] photoBytes) {
        String photoBase64 = Base64.getEncoder().encodeToString(photoBytes);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(Map.of("photo", photoBase64), headers);
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    faceServiceUrl + path, request, Map.class);
            return (String) response.getBody().get("embedding");
        } catch (Exception e) {
            throw new BiometricException("Face recognition service unavailable: " + e.getMessage());
        }
    }

    private void flagBiometricEnrolled(Long userId, boolean enrolled) {
        patientRepository.findById(userId).ifPresent(p -> {
            p.setBiometricEnrolled(enrolled);
            patientRepository.save(p);
        });
        // Doctors can also enroll — handled symmetrically by their repository
    }
}
