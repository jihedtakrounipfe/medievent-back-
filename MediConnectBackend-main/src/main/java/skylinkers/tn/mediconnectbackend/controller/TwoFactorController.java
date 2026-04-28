package skylinkers.tn.mediconnectbackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skylinkers.tn.mediconnectbackend.dto.request.Enable2FARequest;
import skylinkers.tn.mediconnectbackend.dto.response.TwoFactorStatusResponse;
import skylinkers.tn.mediconnectbackend.service.TwoFactorService;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/2fa")
@RequiredArgsConstructor
public class TwoFactorController {

    private final TwoFactorService twoFactorService;

    @GetMapping("/status")
    public ResponseEntity<TwoFactorStatusResponse> getStatus(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            log.debug("[2FA] GET /status - no JWT, returning stub");
            return ResponseEntity.ok(twoFactorService.devModeStubStatus());
        }

        try {
            return ResponseEntity.ok(twoFactorService.getStatus(jwt.getSubject()));
        } catch (Exception e) {
            log.warn("[2FA] GET /status failed - returning stub. Error: {}", e.getMessage());
            return ResponseEntity.ok(twoFactorService.devModeStubStatus());
        }
    }

    @PostMapping("/enable")
    public ResponseEntity<Map<String, String>> enable(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody Enable2FARequest request) {

        if (jwt == null) {
            log.debug("[2FA] POST /enable - dev mode stub");
            return ResponseEntity.ok(Map.of("message", "[DEV] Verification par e-mail activee."));
        }

        log.info("[2FA] POST /enable method={} keycloakId={}", request.getMethod(), jwt.getSubject());
        twoFactorService.enable2FA(jwt.getSubject());
        return ResponseEntity.ok(Map.of(
                "message", "Verification par e-mail activee. Vous pouvez maintenant configurer TOTP ou la reconnaissance faciale."
        ));
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, String>> disable(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            log.debug("[2FA] POST /disable - dev mode stub");
            return ResponseEntity.ok(Map.of("message", "[DEV] Toutes les methodes MFA ont ete desactivees."));
        }

        log.info("[2FA] POST /disable keycloakId={}", jwt.getSubject());
        twoFactorService.disable2FA(jwt.getSubject());
        return ResponseEntity.ok(Map.of(
                "message", "Toutes les methodes MFA ont ete desactivees et reinitialisees."
        ));
    }
}
