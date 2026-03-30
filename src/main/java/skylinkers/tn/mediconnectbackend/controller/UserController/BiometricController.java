package skylinkers.tn.mediconnectbackend.controller.UserController;

import skylinkers.tn.mediconnectbackend.dto.response.PatientResponse;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.BiometricService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.PatientService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * All endpoints require an active Keycloak session (authenticated user).
 *
 * RGPD — biometric data is special-category (Art. 9):
 *   - Enrollment requires explicit prior consent (handled at frontend before calling here)
 *   - Revocation is available to the user at any time (DELETE /me/biometric)
 */
@RestController
@RequestMapping("/api/v1/users")
public class BiometricController {

    private final BiometricService biometricService;
    private final PatientService   patientService;

    public BiometricController(BiometricService biometricService, PatientService patientService) {
        this.biometricService = biometricService;
        this.patientService   = patientService;
    }

    /** Step 1: Enroll — user submits a captured photo. */
    @PostMapping(value = "/me/biometric/enroll", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> enrollBiometric(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("photo") MultipartFile photo) throws IOException {

        Long userId = resolveUserId(jwt);
        biometricService.enrollBiometric(userId, photo.getBytes());

        return ResponseEntity.ok(Map.of(
                "enrolled", true,
                "message",  "Biometric enrollment successful"
        ));
    }

    /** Step 2: Verify — used by Keycloak custom authenticator callback. */
    @PostMapping(value = "/me/biometric/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> verifyBiometric(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("photo") MultipartFile photo) throws IOException {

        Long userId = resolveUserId(jwt);
        boolean matched = biometricService.verifyBiometric(userId, photo.getBytes());

        return ResponseEntity.ok(Map.of(
                "verified", matched,
                "message",  matched ? "Identity verified" : "Facial match failed"
        ));
    }

    /** RGPD Art. 17: user revokes their biometric data at any time. */
    @DeleteMapping("/me/biometric")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> revokeBiometric(@AuthenticationPrincipal Jwt jwt) {
        Long userId = resolveUserId(jwt);
        biometricService.revokeBiometric(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/biometric/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> getBiometricStatus(@AuthenticationPrincipal Jwt jwt) {
        Long userId = resolveUserId(jwt);
        return ResponseEntity.ok(Map.of("enrolled", biometricService.isEnrolled(userId)));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long resolveUserId(Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return patientService.getPatientByKeycloakId(keycloakId).getId();
    }
}
