package skylinkers.tn.mediconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.dto.response.FaceStatusResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.enums.AuditAction;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.FaceRecognitionService;
import skylinkers.tn.mediconnectbackend.service.TwoFactorService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;

import java.io.IOException;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/face")
@RequiredArgsConstructor
public class FaceRecognitionController {

    private final AppUserRepository appUserRepository;
    private final FaceRecognitionService faceRecognitionService;
    private final TwoFactorService twoFactorService;
    private final AuditLogService auditLogService;

    @PostMapping(value = "/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> enroll(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("image") MultipartFile image) throws IOException {
        AppUser user = resolveUser(jwt);
        ensureFacePrerequisite(user);
        faceRecognitionService.enrollFace(user, image.getBytes());
        return ResponseEntity.ok(Map.of("success", true, "message", "Reconnaissance faciale activee avec succes."));
    }

    @GetMapping("/status")
    public ResponseEntity<FaceStatusResponse> status(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt);
        twoFactorService.sanitizeInvalidMfaState(user);
        return ResponseEntity.ok(FaceStatusResponse.builder()
                .faceEnabled(user.isFaceEnabled())
                .faceEnrolled(user.isFaceEnrolled())
                .build());
    }

    @PutMapping("/enable")
    public ResponseEntity<Map<String, String>> enable(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt);
        ensureFacePrerequisite(user);
        user.setFaceEnabled(true);
        appUserRepository.save(user);
        auditLogService.log(user, AuditAction.FACE_ENABLED.name(), null, null, true, null);
        return ResponseEntity.ok(Map.of("message", "Veuillez enregistrer votre visage pour finaliser l'activation."));
    }

    @PutMapping("/disable")
    public ResponseEntity<Map<String, String>> disable(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt);
        faceRecognitionService.deleteFaceTemplate(user);
        auditLogService.log(user, AuditAction.FACE_DISABLED.name(), null, null, true, null);
        return ResponseEntity.ok(Map.of("message", "Reconnaissance faciale desactivee."));
    }

    @PostMapping(value = "/reconfigure", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> reconfigure(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("image") MultipartFile image) throws IOException {
        AppUser user = resolveUser(jwt);
        ensureFacePrerequisite(user);
        faceRecognitionService.enrollFace(user, image.getBytes());
        auditLogService.log(user, AuditAction.FACE_RECONFIGURED.name(), null, null, true, null);
        return ResponseEntity.ok(Map.of("success", true, "message", "Reconnaissance faciale reconfiguree avec succes."));
    }

    private AppUser resolveUser(Jwt jwt) {
        return appUserRepository.findByKeycloakId(jwt.getSubject())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable."));
    }

    private void ensureFacePrerequisite(AppUser user) {
        if (!twoFactorService.canConfigureFace(user)) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Activez d'abord la verification par e-mail avant d'utiliser la reconnaissance faciale.");
        }
    }
}
