package skylinkers.tn.mediconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.enums.AuditAction;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceRecognitionServiceImpl implements FaceRecognitionService {

    private final RestTemplate restTemplate;
    private final AppUserRepository appUserRepository;
    private final AuditLogService auditLogService;

    @Value("${mediconnect.face-recognition.enabled:true}")
    private boolean enabled;

    @Value("${mediconnect.face-recognition.service-url:http://localhost:8000}")
    private String serviceUrl;

    @Value("${mediconnect.face-recognition.enroll-path:/api/face/enroll}")
    private String enrollPath;

    @Value("${mediconnect.face-recognition.verify-path:/api/face/verify}")
    private String verifyPath;

    @Value("${mediconnect.face-recognition.delete-path:/api/face/template}")
    private String deletePath;

    @Override
    @Transactional
    public void enrollFace(AppUser user, byte[] imageData) {
        ensureEnabled();
        try {
            postForMap(enrollPath, buildPayload(user, imageData));
            user.setFaceEnabled(true);
            user.setFaceEnrolled(true);
            appUserRepository.save(user);
            auditLogService.log(user, AuditAction.FACE_ENROLLED.name(), null, null, true, null);
        } catch (Exception ex) {
            auditLogService.log(user, AuditAction.FACE_ENROLLMENT_FAILED.name(), null, null, false, ex.getMessage());
            throw wrap("Impossible d'enregistrer votre visage pour le moment.", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public FaceVerificationResult verifyFace(AppUser user, byte[] imageData) {
        ensureEnabled();
        try {
            Map<String, Object> body = postForMap(verifyPath, buildPayload(user, imageData));
            boolean matched = Boolean.TRUE.equals(body.get("match")) || Boolean.TRUE.equals(body.get("matched"));
            Double similarity = extractDouble(body.get("similarity"), body.get("score"), body.get("confidence"));
            String message = body.get("message") instanceof String value ? value : null;
            auditLogService.log(user,
                    matched ? AuditAction.FACE_VERIFIED.name() : AuditAction.FACE_VERIFICATION_FAILED.name(),
                    null, null, matched, message);
            return new FaceVerificationResult(matched, similarity, message);
        } catch (Exception ex) {
            auditLogService.log(user, AuditAction.FACE_VERIFICATION_FAILED.name(), null, null, false, ex.getMessage());
            throw wrap("Le service de reconnaissance faciale est temporairement indisponible.", ex);
        }
    }

    @Override
    @Transactional
    public void deleteFaceTemplate(AppUser user) {
        if (enabled) {
            try {
                restTemplate.exchange(serviceUrl + deletePath + "/" + user.getKeycloakId(),
                        HttpMethod.DELETE, new HttpEntity<>(new HttpHeaders()), Void.class);
            } catch (Exception ex) {
                log.warn("[FACE] Remote template deletion failed for {}: {}", user.getEmail(), ex.getMessage());
            }
        } else {
            log.info("[FACE] Face service disabled - clearing local enrollment only for {}", user.getEmail());
        }
        user.setFaceEnabled(false);
        user.setFaceEnrolled(false);
        appUserRepository.save(user);
        auditLogService.log(user, AuditAction.FACE_TEMPLATE_DELETED.name(), null, null, true, null);
    }

    @Override
    public boolean isEnrolled(AppUser user) {
        return user.isFaceEnrolled();
    }

    private void ensureEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE,
                    "Le service de reconnaissance faciale est désactivé.");
        }
    }

    private Map<String, Object> buildPayload(AppUser user, byte[] imageData) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", user.getKeycloakId());
        payload.put("email", user.getEmail());
        payload.put("image", Base64.getEncoder().encodeToString(imageData));
        return payload;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForMap(String path, Map<String, Object> payload) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                serviceUrl + path,
                new HttpEntity<>(payload, headers),
                Map.class
        );
        return response.getBody() != null ? response.getBody() : Map.of();
    }

    private Double extractDouble(Object... values) {
        for (Object value : values) {
            if (value instanceof Number number) {
                return number.doubleValue();
            }
            if (value instanceof String stringValue) {
                try {
                    return Double.parseDouble(stringValue);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return null;
    }

    private ResponseStatusException wrap(String message, Exception ex) {
        if (ex instanceof ResponseStatusException responseStatusException) {
            return responseStatusException;
        }
        return new ResponseStatusException(SERVICE_UNAVAILABLE, message, ex);
    }
}
