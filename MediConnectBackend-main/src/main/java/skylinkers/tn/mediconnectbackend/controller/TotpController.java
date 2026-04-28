package skylinkers.tn.mediconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.TotpService;
import skylinkers.tn.mediconnectbackend.service.TwoFactorService;

import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * TOTP authenticator-app MFA management endpoints.
 * All endpoints require a valid JWT (authenticated user).
 */
@Slf4j
@RestController
@RequestMapping("/api/totp")
@RequiredArgsConstructor
public class TotpController {

    private final TotpService totpService;
    private final AppUserRepository appUserRepository;
    private final TwoFactorService twoFactorService;

    /**
     * GET /api/totp/status
     *
     * Returns the current TOTP enrollment state and remaining recovery code count.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.ok(Map.of(
                    "totpEnabled", false,
                    "totpEnrolled", false,
                    "recoveryCodesRemaining", 0
            ));
        }
        AppUser user = resolveUser(jwt.getSubject());
        twoFactorService.sanitizeInvalidMfaState(user);
        return ResponseEntity.ok(Map.of(
                "totpEnabled", user.isTotpEnabled(),
                "totpEnrolled", user.isTotpEnrolled(),
                "recoveryCodesRemaining", totpService.countRemainingRecoveryCodes(user)
        ));
    }

    /**
     * POST /api/totp/setup/initiate
     *
     * Generates a fresh TOTP secret, persists it as a pending secret (not yet enabled),
     * and returns the QR code data URI + raw secret for manual authenticator-app entry.
     */
    @PostMapping("/setup/initiate")
    public ResponseEntity<Map<String, Object>> initiateSetup(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt.getSubject());
        TotpService.TotpSetupData setup = totpService.initiateTotpSetup(user);
        log.info("[TOTP] Setup initiated for userId={}", user.getId());
        return ResponseEntity.ok(Map.of(
                "secret", setup.secret(),
                "qrCodeDataUri", setup.qrCodeDataUri()
        ));
    }

    /**
     * POST /api/totp/setup/verify
     *
     * Verifies the user's first code against the pending secret, enables TOTP,
     * and returns 8 single-use recovery codes (shown once — never retrievable again).
     *
     * Body: { "code": "123456" }
     */
    @PostMapping("/setup/verify")
    public ResponseEntity<Map<String, Object>> verifySetup(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le code est requis."));
        }
        AppUser user = resolveUser(jwt.getSubject());
        List<String> recoveryCodes = totpService.confirmTotpSetup(user, code.trim());
        log.info("[TOTP] Setup confirmed for userId={}", user.getId());
        return ResponseEntity.ok(Map.of("recoveryCodes", recoveryCodes));
    }

    /**
     * POST /api/totp/setup/cancel
     *
     * Clears a pending TOTP setup without enabling anything.
     */
    @PostMapping("/setup/cancel")
    public ResponseEntity<Map<String, String>> cancelSetup(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt.getSubject());
        totpService.cancelTotpSetup(user);
        return ResponseEntity.ok(Map.of("message", "Configuration TOTP annulée."));
    }

    /**
     * POST /api/totp/disable
     *
     * Disables TOTP, clears the encrypted secret, and deletes all recovery codes.
     */
    @PostMapping("/disable")
    public ResponseEntity<Map<String, String>> disable(@AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt.getSubject());
        totpService.disableTotp(user);
        log.info("[TOTP] Disabled by userId={}", user.getId());
        return ResponseEntity.ok(Map.of("message", "TOTP désactivé."));
    }

    /**
     * POST /api/totp/recovery/regenerate
     *
     * Invalidates all existing recovery codes and generates 8 new ones.
     * Returns the new plaintext codes (shown once).
     */
    @PostMapping("/recovery/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateRecoveryCodes(
            @AuthenticationPrincipal Jwt jwt) {
        AppUser user = resolveUser(jwt.getSubject());
        if (!user.isTotpEnabled()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "TOTP non activé — impossible de régénérer les codes de récupération."));
        }
        List<String> codes = totpService.regenerateRecoveryCodes(user);
        log.info("[TOTP] Recovery codes regenerated for userId={}", user.getId());
        return ResponseEntity.ok(Map.of("recoveryCodes", codes));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private AppUser resolveUser(String keycloakId) {
        return appUserRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Utilisateur introuvable."));
    }
}
