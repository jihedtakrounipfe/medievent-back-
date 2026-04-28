package skylinkers.tn.mediconnectbackend.controller.UserController;

import skylinkers.tn.mediconnectbackend.dto.request.*;
import skylinkers.tn.mediconnectbackend.dto.response.AuthResponse;
import skylinkers.tn.mediconnectbackend.entities.enums.AuditAction;
import skylinkers.tn.mediconnectbackend.service.PasswordResetService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.IAuthService;
import skylinkers.tn.mediconnectbackend.utils.RecaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unified auth entry point — login, registration, logout, token refresh,
 * and the two-step authenticated password change flow.
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

    private final IAuthService      authService;
    private final RecaptchaService  recaptchaService;
    private final PasswordResetService passwordResetService;
    private final AuditLogService   auditLogService;

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("[AUTH] POST /login — {}", request.getEmail());
        try {
            // MFA follow-up steps don't need a fresh reCAPTCHA token —
            // the session token itself proves the initial challenge was solved
            boolean isFollowUpStep =
                    (request.getMfaSessionToken() != null && !request.getMfaSessionToken().isBlank()) ||
                    (request.getOtpCode() != null && !request.getOtpCode().isBlank()) ||
                    (request.getFaceImage() != null && !request.getFaceImage().isBlank()) ||
                    Boolean.TRUE.equals(request.getUseEmailFallback());
            if (!isFollowUpStep) {
                recaptchaService.verify(request.getRecaptchaToken());
            }
            AuthResponse response = authService.login(request);
            if (Boolean.TRUE.equals(response.getAllMethodsExhausted())) {
                auditLogService.logAuth(AuditAction.MFA_ALL_EXHAUSTED,
                        request.getEmail(), null, httpRequest, false, "All MFA methods exhausted");
            } else if (Boolean.TRUE.equals(response.getRequiresMfa())) {
                AuditAction action = response.getFailedMethod() != null
                        ? AuditAction.LOGIN_2FA_FAILED : AuditAction.LOGIN_2FA_REQUESTED;
                auditLogService.logAuth(action, request.getEmail(), null, httpRequest,
                        response.getFailedMethod() == null, response.getFailedMethod());
            } else if (response.getAccessToken() != null) {
                auditLogService.logAuth(AuditAction.LOGIN_SUCCESS,
                        request.getEmail(), null, httpRequest, true, null);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            auditLogService.logAuth(AuditAction.LOGIN_FAILED,
                    request.getEmail(), null, httpRequest, false,
                    e.getMessage() != null ? e.getMessage() : "Invalid credentials");
            throw e;
        }
    }

    @PostMapping("/login/2fa")
    public ResponseEntity<AuthResponse> loginWith2FA(
            @Valid @RequestBody LoginWith2FARequest request,
            HttpServletRequest httpRequest) {
        log.info("[AUTH] POST /login/2fa — {}", request.getEmail());
        try {
            AuthResponse response = authService.loginWith2FA(request);
            auditLogService.logAuth(AuditAction.LOGIN_2FA_SUCCESS,
                    request.getEmail(), null, httpRequest, true, null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            auditLogService.logAuth(AuditAction.LOGIN_2FA_FAILED,
                    request.getEmail(), null, httpRequest, false, e.getMessage());
            throw e;
        }
    }

    // ── Registration ──────────────────────────────────────────────────────────

    @PostMapping("/register/patient")
    public ResponseEntity<AuthResponse> registerPatient(
            @Valid @RequestBody CreatePatientRequest request,
            HttpServletRequest httpRequest) {
        log.info("[AUTH] POST /register/patient — {}", request.getEmail());
        try {
            recaptchaService.verify(request.getRecaptchaToken());
            AuthResponse response = authService.registerPatient(request);
            auditLogService.logAuth(AuditAction.SIGNUP_PATIENT,
                    request.getEmail(), null, httpRequest, true, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            auditLogService.logAuth(AuditAction.SIGNUP_PATIENT,
                    request.getEmail(), null, httpRequest, false, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/register/doctor")
    public ResponseEntity<AuthResponse> registerDoctor(
            @Valid @RequestBody CreateDoctorRequest request,
            HttpServletRequest httpRequest) {
        log.info("[AUTH] POST /register/doctor — {}", request.getEmail());
        try {
            recaptchaService.verify(request.getRecaptchaToken());
            AuthResponse response = authService.registerDoctor(request);
            auditLogService.logAuth(AuditAction.SIGNUP_DOCTOR,
                    request.getEmail(), null, httpRequest, true, null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            auditLogService.logAuth(AuditAction.SIGNUP_DOCTOR,
                    request.getEmail(), null, httpRequest, false, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest) {
        log.info("[AUTH] POST /verify-email — {}", request.getEmail());
        try {
            AuthResponse response = authService.verifyEmail(request);
            auditLogService.logAuth(AuditAction.EMAIL_VERIFICATION_SUCCESS,
                    request.getEmail(), null, httpRequest, true, null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            auditLogService.logAuth(AuditAction.EMAIL_VERIFICATION_FAILED,
                    request.getEmail(), null, httpRequest, false, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request,
            HttpServletRequest httpRequest) {
        log.info("[AUTH] POST /resend-verification — {}", request.getEmail());
        AuthResponse response = authService.resendVerification(request);
        auditLogService.logAuth(AuditAction.EMAIL_VERIFICATION_SENT,
                request.getEmail(), null, httpRequest, true, null);
        return ResponseEntity.ok(response);
    }

    // ── Password Reset (unauthenticated) ─────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        log.info("[AUTH] POST /forgot-password — {}", request.getEmail());
        passwordResetService.initiateReset(request);
        auditLogService.logAuth(AuditAction.PASSWORD_RESET_REQUESTED,
                request.getEmail(), null, httpRequest, true, null);
        return ResponseEntity.ok(Map.of(
                "message", "Si l'adresse e-mail existe, un code de réinitialisation a été envoyé."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpRequest) {
        log.info("[AUTH] POST /reset-password — {}", request.getEmail());
        AuthResponse response = passwordResetService.confirmReset(request);
        auditLogService.logAuth(AuditAction.PASSWORD_RESET_SUCCESS,
                request.getEmail(), null, httpRequest, true, null);
        return ResponseEntity.ok(response);
    }

    // ── Password Change (authenticated — two-step) ────────────────────────────

    /**
     * Step 1a: Send a 6-digit verification code to the authenticated user's email.
     * Email is extracted from the JWT — the user does NOT type it.
     *
     * POST /api/auth/password-change/send-code
     * Authorization: Bearer {user_token}
     */
    @PostMapping("/password-change/send-code")
    public ResponseEntity<Map<String, String>> sendPasswordChangeCode(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        if (jwt == null) {
            log.debug("[AUTH] POST /password-change/send-code — DEV MODE stub");
            return ResponseEntity.ok(Map.of(
                    "message", "[DEV] Code envoyé (stub — Keycloak désactivé)",
                    "maskedEmail", "de***@example.com"
            ));
        }

        String email     = jwt.getClaim("email");
        String keycloakId = jwt.getSubject();
        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalide — email manquant.");
        }

        authService.sendPasswordChangeCode(email, httpRequest);
        auditLogService.logAuth(AuditAction.PASSWORD_CHANGE_CODE_SENT,
                email, keycloakId, httpRequest, true, null);

        return ResponseEntity.ok(Map.of(
                "message",      "Code de vérification envoyé à votre adresse e-mail",
                "maskedEmail",  maskEmail(email)
        ));
    }

    /**
     * Step 1b: Verify the code and receive a short-lived verification token.
     *
     * POST /api/auth/password-change/verify-code
     * Body: { "code": "482917" }
     */
    @PostMapping("/password-change/verify-code")
    public ResponseEntity<Map<String, Object>> verifyPasswordChangeCode(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        if (jwt == null) {
            log.debug("[AUTH] POST /password-change/verify-code — DEV MODE stub");
            return ResponseEntity.ok(Map.of(
                    "verificationToken", "dev-stub-token",
                    "expiresIn",         300
            ));
        }

        String email     = jwt.getClaim("email");
        String keycloakId = jwt.getSubject();
        String code      = body.get("code");

        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalide — email manquant.");
        }
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code est requis.");
        }

        String verificationToken = authService.verifyPasswordChangeCode(email, code);
        auditLogService.logAuth(AuditAction.PASSWORD_CHANGE_CODE_VERIFIED,
                email, keycloakId, httpRequest, true, null);

        return ResponseEntity.ok(Map.of(
                "verificationToken", verificationToken,
                "expiresIn",         300
        ));
    }

    /**
     * Step 2: Change the password using the verification token from step 1.
     *
     * POST /api/auth/change-password
     * Body: { verificationToken, currentPassword, newPassword, confirmPassword }
     */
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        if (jwt == null) {
            log.debug("[AUTH] POST /change-password — DEV MODE stub");
            return ResponseEntity.ok(Map.of("message", "[DEV] Mot de passe modifié (stub — Keycloak désactivé)"));
        }

        String email     = jwt.getClaim("email");
        String keycloakId = jwt.getSubject();

        if (email == null || keycloakId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalide — email ou subject manquant.");
        }

        try {
            authService.changePassword(email, keycloakId, request);
            auditLogService.logAuth(AuditAction.PASSWORD_CHANGE_SUCCESS,
                    email, keycloakId, httpRequest, true, null);
        } catch (Exception e) {
            auditLogService.logAuth(AuditAction.PASSWORD_CHANGE_FAILED,
                    email, keycloakId, httpRequest, false, e.getMessage());
            throw e;
        }

        return ResponseEntity.ok(Map.of("message", "Mot de passe modifié avec succès"));
    }

    // ── Session Management ────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {

        if (jwt != null) {
            String email     = jwt.getClaim("email");
            String keycloakId = jwt.getSubject();
            authService.logout(keycloakId);
            auditLogService.logAuth(AuditAction.LOGOUT, email, keycloakId, httpRequest, true, null);
        } else {
            log.info("[AUTH] POST /logout — DEV MODE, no JWT to invalidate");
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        log.info("[AUTH] POST /refresh");
        return ResponseEntity.ok(authService.refreshToken(request.getRefreshToken()));
    }

    // ── Private helpers ───────────────────────────────────────────────────────────

    /** Mask email for display: "amine@gmail.com" → "am****@gmail.com" */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "****";
        String[] parts = email.split("@", 2);
        String local  = parts[0];
        String domain = parts[1];
        if (local.length() <= 2) return local.charAt(0) + "****@" + domain;
        return local.substring(0, 2) + "****@" + domain;
    }
}
