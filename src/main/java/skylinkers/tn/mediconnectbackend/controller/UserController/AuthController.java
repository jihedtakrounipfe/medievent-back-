package skylinkers.tn.mediconnectbackend.controller.UserController;

import skylinkers.tn.mediconnectbackend.dto.request.*;
import skylinkers.tn.mediconnectbackend.dto.response.AuthResponse;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.IAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Unified auth entry point — login, registration, logout, token refresh.
 *
 * Keycloak is currently disabled (DevSecurityConfig permits all).
 * All Keycloak-dependent logic is stubbed in AuthServiceImpl with TODO markers.
 * No @PreAuthorize annotations here — will be added when Keycloak is re-enabled.
 *
 * SOLID:
 *   SRP — only auth flow coordination, no business logic
 *   DIP — depends on IAuthService interface, not the impl
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    // ── Login ─────────────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/login
     *
     * Standard email/password login.
     * Returns JWT pair on success, or { requires2FA: true } if TOTP is enabled.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        log.info("[AUTH] POST /login — {}", request.getEmail());
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * POST /api/v1/auth/login/2fa
     *
     * Second step for users with 2FA enabled (doctors & admins mandatory).
     * Frontend calls this after receiving requires2FA: true from /login.
     */
    @PostMapping("/login/2fa")
    public ResponseEntity<AuthResponse> loginWith2FA(
            @Valid @RequestBody LoginWith2FARequest request) {
        log.info("[AUTH] POST /login/2fa — {}", request.getEmail());
        return ResponseEntity.ok(authService.loginWith2FA(request));
    }

    // ── Registration ──────────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/register/patient
     *
     * Patient self-registration.
     * → Creates DB record (PatientService) + triggers Keycloak email verification (stubbed).
     * Returns 201 + message; no tokens issued until email is verified.
     */
    @PostMapping("/register/patient")
    public ResponseEntity<AuthResponse> registerPatient(
            @Valid @RequestBody CreatePatientRequest request) {
        log.info("[AUTH] POST /register/patient — {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerPatient(request));
    }

    /**
     * POST /api/v1/auth/register/doctor
     *
     * Doctor self-registration. Account goes to PENDING until admin approves.
     * Returns 201 + message; no tokens until admin sets verificationStatus = APPROVED.
     */
    @PostMapping("/register/doctor")
    public ResponseEntity<AuthResponse> registerDoctor(
            @Valid @RequestBody CreateDoctorRequest request) {
        log.info("[AUTH] POST /register/doctor — {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerDoctor(request));
    }

    // ── Session Management ────────────────────────────────────────────────────

    /**
     * POST /api/v1/auth/logout
     *
     * Invalidates the Keycloak session server-side + revokes linked Google tokens.
     * @AuthenticationPrincipal will be null while Keycloak is disabled — handled gracefully.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {

        // Keycloak disabled: jwt is null — skip server-side invalidation
        if (jwt != null) {
            authService.logout(jwt.getSubject());
        } else {
            log.info("[AUTH] POST /logout — DEV MODE, no JWT to invalidate");
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * POST /api/v1/auth/refresh
     *
     * Exchanges a valid Keycloak refresh token for a new access token.
     * Called automatically by the Angular JwtInterceptor on 401.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("[AUTH] POST /refresh");
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }
}