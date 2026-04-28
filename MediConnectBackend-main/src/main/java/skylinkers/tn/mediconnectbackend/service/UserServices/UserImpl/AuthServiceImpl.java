package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.dto.request.*;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import skylinkers.tn.mediconnectbackend.dto.response.AuthResponse;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.entities.VerificationCode;
import skylinkers.tn.mediconnectbackend.entities.enums.AuditAction;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.exception.InvalidPasswordException;
import skylinkers.tn.mediconnectbackend.exception.PasswordMismatchException;
import skylinkers.tn.mediconnectbackend.exception.TooManyAttemptsException;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.repository.VerificationCodeRepository;
import skylinkers.tn.mediconnectbackend.service.FaceRecognitionService;
import skylinkers.tn.mediconnectbackend.service.LoginAttemptService;
import skylinkers.tn.mediconnectbackend.service.MfaSessionService;
import skylinkers.tn.mediconnectbackend.service.TotpService;
import skylinkers.tn.mediconnectbackend.service.TwoFactorService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.DoctorService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.IAuthService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.PatientService;
import skylinkers.tn.mediconnectbackend.service.VerificationCodeService;
import skylinkers.tn.mediconnectbackend.service.risk.LoginContext;
import skylinkers.tn.mediconnectbackend.service.risk.TwoFaDecision;
import skylinkers.tn.mediconnectbackend.service.risk.TwoFactorDecisionService;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakAdminClient;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient.KeycloakTokenResponse;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final SecureRandom RNG = new SecureRandom();

    private final PatientService patientService;
    private final DoctorService doctorService;
    private final AppUserRepository appUserRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final KeycloakTokenClient keycloakTokenClient;
    private final KeycloakAdminClient keycloakAdminClient;
    private final EmailService emailService;
    private final VerificationCodeService verificationCodeService;
    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;
    private final FaceRecognitionService faceRecognitionService;
    private final MfaSessionService mfaSessionService;
    private final TotpService totpService;
    private final TwoFactorService twoFactorService;
    private final TwoFactorDecisionService twoFactorDecisionService;

    @Override
    public AuthResponse login(LoginRequest request) {
        if (request.getMfaSessionToken() != null && !request.getMfaSessionToken().isBlank()) {
            return handleMfaStep(request);
        }
        return handleCredentialStep(request);
    }

    // ── Step 1: credential verification ──────────────────────────────────────

    private AuthResponse handleCredentialStep(LoginRequest request) {
        log.info("[AUTH] Credential step for {}", request.getEmail());

        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException(
                        "User not found in local DB. Please complete registration first."));

        if (loginAttemptService.isBlocked(request.getEmail())) {
            long remaining = loginAttemptService.getRemainingBlockSeconds(request.getEmail());
            throw new TooManyAttemptsException(remaining);
        }
        if (!user.isActive()) {
            loginAttemptService.loginFailed(request.getEmail());
            throw new RuntimeException("User account is disabled");
        }

        KeycloakTokenResponse tokens = authenticateWithKeycloak(request.getEmail(), request.getPassword());

        // ── Risk scoring ──────────────────────────────────────────────────────
        LoginContext ctx = new LoginContext(
                String.valueOf(user.getId()),
                extractUserAgent(),
                extractIp(),
                java.time.LocalDateTime.now(),
                user
        );
        TwoFaDecision riskDecision = twoFactorDecisionService.evaluate(ctx);
        log.info("[RISK] user={} score={} decision={}", user.getEmail(),
                riskDecision.riskScore(), riskDecision.decision());

        // Build the ordered list of MFA methods this user has explicitly configured.
        // Password reset and dependency cleanup can clear this set entirely; in that
        // case we let the user in and let them reconfigure MFA from profile settings.
        List<String> enabledMethods = twoFactorService.getEnabledMethods(user);

        // SKIPPED: low risk — skip MFA unless the admin enforced it for this user
        if ("SKIPPED".equals(riskDecision.decision()) && !user.isTwoFactorEnforced()) {
            loginAttemptService.resetAttempts(request.getEmail());
            twoFactorDecisionService.recordOutcome(riskDecision.auditLogId(), "SUCCESS");
            return buildTokenResponse(tokens, user);
        }

        // FORCED: high risk — require MFA regardless; inject EMAIL if user has no methods
        if ("FORCED".equals(riskDecision.decision()) && enabledMethods.isEmpty()) {
            enabledMethods = List.of("EMAIL");
            log.warn("[RISK] FORCED MFA for user={} with no configured methods — injecting EMAIL fallback",
                    user.getEmail());
        }

        // No MFA methods at all (OPTIONAL or SKIPPED+enforced edge case) — issue tokens
        if (enabledMethods.isEmpty()) {
            loginAttemptService.resetAttempts(request.getEmail());
            twoFactorDecisionService.recordOutcome(riskDecision.auditLogId(), "SUCCESS");
            return buildTokenResponse(tokens, user);
        }

        // Determine primary method (highest priority with attempts available)
        String primaryMethod = enabledMethods.get(0);

        // If EMAIL is primary, send the code immediately to avoid extra round-trip
        boolean emailOtpSent = false;
        if ("EMAIL".equals(primaryMethod)) {
            String code = randomSixDigits();
            save2FACode(user.getEmail(), code);
            emailService.send2FACode(user.getEmail(), user.getFirstName(), code);
            emailOtpSent = true;
        }

        String sessionToken = mfaSessionService.create(
                user.getEmail(), enabledMethods, emailOtpSent,
                tokens.accessToken(), tokens.refreshToken(), tokens.tokenType(), tokens.expiresIn(),
                riskDecision.auditLogId());

        MfaSessionService.MfaSessionData session = mfaSessionService.get(sessionToken).orElseThrow();

        auditLogService.log(user, AuditAction.LOGIN_2FA_REQUESTED.name(),
                extractIp(), extractUserAgent(), true,
                "methods=" + enabledMethods + " riskScore=" + riskDecision.riskScore()
                        + " riskDecision=" + riskDecision.decision());

        return AuthResponse.builder()
                .requiresMfa(true)
                .mfaSessionToken(sessionToken)
                .enabledMethods(enabledMethods)
                .primaryMethod(primaryMethod)
                .attemptsRemaining(session.attemptsRemaining())
                .message(emailOtpSent ? "Un code de vérification a été envoyé à votre adresse e-mail." : null)
                .build();
    }

    // ── Step 2: MFA verification ──────────────────────────────────────────────

    private AuthResponse handleMfaStep(LoginRequest request) {
        String sessionToken = request.getMfaSessionToken();
        MfaSessionService.MfaSessionData session = mfaSessionService.get(sessionToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Session MFA expirée. Veuillez vous reconnecter."));

        AppUser user = appUserRepository.findByEmail(session.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Utilisateur introuvable."));

        String method = request.getMfaMethod();
        if (method == null || method.isBlank()) {
            method = session.nextAvailableMethod()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                            "Toutes les méthodes MFA ont été épuisées."));
        }

        if (!session.enabledMethods().contains(method) && !"RECOVERY".equals(method)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Méthode MFA non disponible: " + method);
        }

        // Recovery code: special bypass for TOTP users — no attempt decrement
        if ("RECOVERY".equals(method)) {
            String rawCode = request.getRecoveryCode();
            if (rawCode == null || rawCode.isBlank()) {
                return buildMfaWaitResponse(sessionToken, session, "RECOVERY", null, null);
            }
            if (!totpService.verifyRecoveryCode(user, rawCode.trim())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Code de récupération invalide.");
            }
            return completeMfaSuccess(sessionToken, session, user, "RECOVERY");
        }

        int attemptsLeft = session.attemptsRemaining().getOrDefault(method, 0);
        if (attemptsLeft <= 0) {
            String next = session.nextAvailableMethod().orElse(null);
            if (next == null) {
                if (session.riskAuditLogId() != null) {
                    twoFactorDecisionService.recordOutcome(session.riskAuditLogId(), "FAILED");
                }
                mfaSessionService.consume(sessionToken);
                handleMfaExhaustion(user);
                return AuthResponse.builder()
                        .allMethodsExhausted(true)
                        .message("Toutes les tentatives de vérification ont été épuisées.")
                        .build();
            }
            return buildMfaWaitResponse(sessionToken, session, next,
                    method + " épuisé", null);
        }

        return switch (method) {
            case "EMAIL" -> handleEmailMfa(sessionToken, session, user, request);
            case "TOTP"  -> handleTotpMfa(sessionToken, session, user, request);
            case "FACE"  -> handleFaceMfa(sessionToken, session, user, request);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Méthode MFA inconnue: " + method);
        };
    }

    private AuthResponse handleEmailMfa(String sessionToken, MfaSessionService.MfaSessionData session,
            AppUser user, LoginRequest request) {
        String code = request.getMfaCode() != null ? request.getMfaCode() : request.getOtpCode();
        if (code == null || code.isBlank()) {
            // Send the code if not already sent
            if (!session.emailOtpSent()) {
                String otp = randomSixDigits();
                save2FACode(user.getEmail(), otp);
                emailService.send2FACode(user.getEmail(), user.getFirstName(), otp);
                mfaSessionService.markEmailOtpSent(sessionToken);
            }
            return buildMfaWaitResponse(sessionToken, session, "EMAIL",
                    null, "Un code de vérification a été envoyé à votre adresse e-mail.");
        }
        if (!verify2FACode(user.getEmail(), code.trim())) {
            return handleMfaFailure(sessionToken, session, user, "EMAIL",
                    "Code invalide ou expiré.");
        }
        return completeMfaSuccess(sessionToken, session, user, "EMAIL");
    }

    private AuthResponse handleTotpMfa(String sessionToken, MfaSessionService.MfaSessionData session,
            AppUser user, LoginRequest request) {
        String code = request.getMfaCode();
        if (code == null || code.isBlank()) {
            return buildMfaWaitResponse(sessionToken, session, "TOTP", null, null);
        }
        String secret = user.getTotpSecret();
        if (secret == null || !user.isTotpEnrolled()) {
            // TOTP lost but was in enabled list — degrade to next method
            MfaSessionService.MfaSessionData updated =
                    mfaSessionService.decrementAttempt(sessionToken, "TOTP").orElse(session);
            String next = updated.nextAvailableMethod().orElse(null);
            if (next == null) {
                if (updated.riskAuditLogId() != null) {
                    twoFactorDecisionService.recordOutcome(updated.riskAuditLogId(), "FAILED");
                }
                mfaSessionService.consume(sessionToken);
                handleMfaExhaustion(user);
                return AuthResponse.builder()
                        .allMethodsExhausted(true)
                        .message("Toutes les tentatives de vérification ont été épuisées.")
                        .build();
            }
            return buildMfaWaitResponse(sessionToken, updated, next, null, null);
        }
        if (!totpService.verifyCode(secret, code.trim())) {
            return handleMfaFailure(sessionToken, session, user, "TOTP",
                    "Code TOTP invalide.");
        }
        return completeMfaSuccess(sessionToken, session, user, "TOTP");
    }

    private AuthResponse handleFaceMfa(String sessionToken, MfaSessionService.MfaSessionData session,
            AppUser user, LoginRequest request) {
        String faceImage = request.getFaceImage() != null ? request.getFaceImage() : request.getMfaCode();
        if (faceImage == null || faceImage.isBlank()) {
            return buildMfaWaitResponse(sessionToken, session, "FACE", null, null);
        }
        try {
            FaceRecognitionService.FaceVerificationResult result =
                    faceRecognitionService.verifyFace(user, decodeBase64Image(faceImage));
            if (!result.matched()) {
                return handleMfaFailure(sessionToken, session, user, "FACE",
                        faceVerificationMessage(result.message()));
            }
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
                log.warn("[AUTH] Face service unavailable for {}", user.getEmail());
                return buildMfaWaitResponse(sessionToken, session, "FACE", "FACE",
                        "Le service de reconnaissance faciale est temporairement indisponible.");
            }
            return handleMfaFailure(sessionToken, session, user, "FACE",
                    ex.getReason() != null ? ex.getReason() : "Erreur lors de la vérification faciale.");
        }
        return completeMfaSuccess(sessionToken, session, user, "FACE");
    }

    // ── MFA helpers ───────────────────────────────────────────────────────────

    private AuthResponse handleMfaFailure(String sessionToken,
            MfaSessionService.MfaSessionData session, AppUser user,
            String method, String reason) {
        MfaSessionService.MfaSessionData updated =
                mfaSessionService.decrementAttempt(sessionToken, method).orElse(session);
        auditLogService.log(user, AuditAction.MFA_METHOD_SWITCHED.name(),
                extractIp(), extractUserAgent(), false, "method=" + method + " reason=" + reason);

        if (updated.allExhausted()) {
            if (updated.riskAuditLogId() != null) {
                twoFactorDecisionService.recordOutcome(updated.riskAuditLogId(), "FAILED");
            }
            mfaSessionService.consume(sessionToken);
            handleMfaExhaustion(user);
            return AuthResponse.builder()
                    .allMethodsExhausted(true)
                    .message("Toutes les tentatives de vérification ont été épuisées.")
                    .build();
        }

        int remaining = updated.attemptsRemaining().getOrDefault(method, 0);
        if (remaining <= 0) {
            auditLogService.log(user, AuditAction.MFA_METHOD_EXHAUSTED.name(),
                    extractIp(), extractUserAgent(), false, "method=" + method + " exhausted");
        }

        String nextBest = updated.nextAvailableMethod().orElse(method);
        return AuthResponse.builder()
                .requiresMfa(true)
                .mfaSessionToken(sessionToken)
                .enabledMethods(updated.enabledMethods())
                .primaryMethod(nextBest)
                .attemptsRemaining(updated.attemptsRemaining())
                .failedMethod(method)
                .message(reason + (remaining > 0 ? " " + remaining + " tentative(s) restante(s)." : ""))
                .build();
    }

    private AuthResponse completeMfaSuccess(String sessionToken,
            MfaSessionService.MfaSessionData session, AppUser user, String method) {
        mfaSessionService.consume(sessionToken);
        loginAttemptService.resetAttempts(user.getEmail());
        auditLogService.log(user, AuditAction.TOTP_VERIFIED.name(),
                extractIp(), extractUserAgent(), true, "method=" + method);
        if (session.riskAuditLogId() != null) {
            twoFactorDecisionService.recordOutcome(session.riskAuditLogId(), "SUCCESS");
        }
        return buildTokenResponse(
                new KeycloakTokenResponse(session.accessToken(), session.refreshToken(),
                        session.tokenType(), session.expiresIn()),
                user);
    }

    private AuthResponse buildMfaWaitResponse(String sessionToken,
            MfaSessionService.MfaSessionData session,
            String primaryMethod, String failedMethod, String message) {
        return AuthResponse.builder()
                .requiresMfa(true)
                .mfaSessionToken(sessionToken)
                .enabledMethods(session.enabledMethods())
                .primaryMethod(primaryMethod)
                .attemptsRemaining(session.attemptsRemaining())
                .failedMethod(failedMethod)
                .message(message)
                .build();
    }

    private void handleMfaExhaustion(AppUser user) {
        log.warn("[AUTH] All MFA methods exhausted for userId={}", user.getId());
        auditLogService.log(user, AuditAction.MFA_ALL_EXHAUSTED.name(),
                extractIp(), extractUserAgent(), false, null);
        twoFactorService.disableAllMfa(user);
        
        // Best-effort remote cleanup — failures are logged, not re-thrown
        
        
        auditLogService.log(user, AuditAction.MFA_EXHAUSTED_RESET_SUCCESS.name(),
                extractIp(), extractUserAgent(), true, null);
    }

    private KeycloakTokenResponse authenticateWithKeycloak(String email, String password) {
        try {
            return keycloakTokenClient.passwordGrant(email, password);
        } catch (HttpClientErrorException.BadRequest e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("unauthorized_client")
                    && body.contains("direct access grants")) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Keycloak client 'angular-spa' is not allowed for Direct Access Grants.", e);
            }
            loginAttemptService.loginFailed(email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Email ou mot de passe incorrect.", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            loginAttemptService.loginFailed(email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Email ou mot de passe incorrect.", e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Keycloak est inaccessible.", e);
        }
    }

    private AuthResponse buildTokenResponse(KeycloakTokenResponse tokens, AppUser user) {
        return AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType(tokens.tokenType())
                .expiresIn(tokens.expiresIn())
                .user(mapToUserResponse(user))
                .build();
    }

    @Override
    public AuthResponse loginWith2FA(LoginWith2FARequest request) {
        throw new UnsupportedOperationException("Use the standard /login endpoint with otpCode field for 2FA.");
    }

    @Override
    public AuthResponse registerPatient(CreatePatientRequest request) {
        log.info("[AUTH] Patient registration: {}", request.getEmail());

        String keycloakId;
        try {
            keycloakId = keycloakAdminClient.createPatientUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName()
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak admin authentication failed. Verify KEYCLOAK_ADMIN_USERNAME/KEYCLOAK_ADMIN_PASSWORD and that admin-cli direct access grants are enabled.",
                    e
            );
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak is unreachable. Verify keycloak.server-url and that Keycloak is running.",
                    e
            );
        }
        request.setKeycloakId(keycloakId);

        var patient = patientService.createPatient(request);
        verificationCodeService.generateAndSend(request.getEmail(), request.getFirstName());

        return AuthResponse.builder()
                .message("Verification code sent to your email.")
                .user(patient)
                .build();
    }

    @Override
    public AuthResponse registerDoctor(CreateDoctorRequest request) {
        log.info("[AUTH] Doctor registration: {} (RPPS: {})", request.getEmail(), request.getRppsNumber());

        String keycloakId;
        try {
            keycloakId = keycloakAdminClient.createDoctorUser(
                    request.getEmail(),
                    request.getPassword(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getRppsNumber()
            );
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak admin authentication failed. Verify KEYCLOAK_ADMIN_USERNAME/KEYCLOAK_ADMIN_PASSWORD and that admin-cli direct access grants are enabled.",
                    e
            );
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak is unreachable. Verify keycloak.server-url and that Keycloak is running.",
                    e
            );
        }
        request.setKeycloakId(keycloakId);

        var doctor = doctorService.createDoctor(request);
        verificationCodeService.generateAndSend(request.getEmail(), request.getFirstName());

        return AuthResponse.builder()
                .message("Verification code sent to your email.")
                .user(doctor)
                .build();
    }

    @Override
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        boolean ok = verificationCodeService.verifyCode(request.getEmail(), request.getCode());
        if (!ok) {
            throw new IllegalArgumentException("Invalid or expired verification code.");
        }

        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        user.setIsVerified(Boolean.TRUE);
        appUserRepository.save(user);

        keycloakAdminClient.setEmailVerifiedByEmail(request.getEmail(), true);
        emailService.sendWelcomeEmail(request.getEmail(), user.getFirstName());

        return AuthResponse.builder()
                .message("Email verified successfully.")
                .build();
    }

    @Override
    public AuthResponse resendVerification(ResendVerificationRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        verificationCodeService.generateAndSend(user.getEmail(), user.getFirstName());
        return AuthResponse.builder()
                .message("Verification code sent.")
                .build();
    }

    @Override
    public void sendPasswordChangeCode(String email, HttpServletRequest httpRequest) {
        long recentSends = verificationCodeRepository.countByEmailAndPurposeAndCreatedAtAfter(
                email.toLowerCase().trim(), "PWD_CHANGE_CODE", LocalDateTime.now().minusHours(1));
        if (recentSends >= 3) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Trop de demandes. Veuillez reessayer dans une heure.");
        }

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));

        List<VerificationCode> old = verificationCodeRepository.findByEmailAndPurposeAndUsedFalse(
                email.toLowerCase().trim(), "PWD_CHANGE_CODE");
        old.forEach(c -> c.setUsed(true));
        if (!old.isEmpty()) {
            verificationCodeRepository.saveAll(old);
        }

        String code = randomSixDigits();
        VerificationCode vc = new VerificationCode();
        vc.setEmail(email.toLowerCase().trim());
        vc.setCode(code);
        vc.setPurpose("PWD_CHANGE_CODE");
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        vc.setUsed(false);
        verificationCodeRepository.save(vc);

        emailService.sendPasswordChangeCode(email, user.getFirstName(), code);
        log.info("[AUTH] Password change code sent to {}", email);
    }

    @Override
    public String verifyPasswordChangeCode(String email, String code) {
        VerificationCode vc = verificationCodeRepository
                .findByEmailAndCodeAndPurposeAndUsedFalseAndExpiresAtAfter(
                        email.toLowerCase().trim(),
                        code.trim(),
                        "PWD_CHANGE_CODE",
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Code invalide ou expire."));

        vc.setUsed(true);
        verificationCodeRepository.save(vc);

        String token = UUID.randomUUID().toString();
        VerificationCode tokenVc = new VerificationCode();
        tokenVc.setEmail(email.toLowerCase().trim());
        tokenVc.setCode(token);
        tokenVc.setPurpose("PASSWORD_CHANGE_TOKEN");
        tokenVc.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        tokenVc.setUsed(false);
        verificationCodeRepository.save(tokenVc);

        log.info("[AUTH] Password change code verified for {} - token issued", email);
        return token;
    }

    @Override
    public void changePassword(String userEmail, String keycloakId, ChangePasswordRequest request) {
        log.info("[AUTH] changePassword for {}", userEmail);

        VerificationCode tokenVc = verificationCodeRepository
                .findByEmailAndCodeAndPurposeAndUsedFalseAndExpiresAtAfter(
                        userEmail.toLowerCase().trim(),
                        request.getVerificationToken().trim(),
                        "PASSWORD_CHANGE_TOKEN",
                        LocalDateTime.now()
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Token de verification invalide ou expire. Recommencez la procedure."));
        tokenVc.setUsed(true);
        verificationCodeRepository.save(tokenVc);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new PasswordMismatchException("Les mots de passe ne correspondent pas");
        }

        if (!verifyPasswordWithKeycloak(userEmail, request.getCurrentPassword())) {
            throw new InvalidPasswordException("Le mot de passe actuel est incorrect");
        }

        try {
            keycloakAdminClient.resetPasswordById(keycloakId, request.getNewPassword());
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak est inaccessible. Impossible de modifier le mot de passe.",
                    e
            );
        }

        AppUser pwdUser = appUserRepository.findByEmail(userEmail).orElse(null);
        if (pwdUser != null && pwdUser.getGoogleId() != null) {
            pwdUser.setGoogleInternalPassword(request.getNewPassword());
            appUserRepository.save(pwdUser);
        }

        log.info("[AUTH] Password changed successfully for {}", userEmail);
    }

    @Override
    public void logout(String keycloakId) {
        log.info("[AUTH] Logout for keycloakId={}", keycloakId);
        keycloakAdminClient.logoutUserSessions(keycloakId);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        log.info("[AUTH] Token refresh requested");

        KeycloakTokenResponse tokens;
        try {
            tokens = keycloakTokenClient.refresh(refreshToken);
        } catch (HttpClientErrorException.BadRequest e) {
            String body = e.getResponseBodyAsString();
            if (body != null && body.contains("invalid_grant") && body.contains("Token is not active")) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token expired or revoked. Please log in again.",
                        e
                );
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.", e);
        } catch (HttpClientErrorException.Unauthorized e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token.", e);
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak is unreachable. Verify keycloak.server-url and that Keycloak is running.",
                    e
            );
        }

        return AuthResponse.builder()
                .accessToken(tokens.accessToken())
                .refreshToken(tokens.refreshToken())
                .tokenType(tokens.tokenType())
                .expiresIn(tokens.expiresIn())
                .build();
    }

    private boolean verifyPasswordWithKeycloak(String email, String password) {
        try {
            keycloakTokenClient.passwordGrant(email, password);
            return true;
        } catch (HttpClientErrorException.Unauthorized e) {
            return false;
        } catch (HttpClientErrorException.BadRequest e) {
            return false;
        } catch (Exception e) {
            log.error("[AUTH] Error verifying current password for {}: {}", email, e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Erreur lors de la verification du mot de passe. Veuillez reessayer.",
                    e
            );
        }
    }

    void save2FACode(String email, String code) {
        List<VerificationCode> old = verificationCodeRepository.findByEmailAndPurposeAndUsedFalse(email, "2FA_LOGIN");
        old.forEach(c -> c.setUsed(true));
        if (!old.isEmpty()) {
            verificationCodeRepository.saveAll(old);
        }

        VerificationCode vc = new VerificationCode();
        vc.setEmail(email.toLowerCase().trim());
        vc.setCode(code);
        vc.setPurpose("2FA_LOGIN");
        vc.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        vc.setUsed(false);
        verificationCodeRepository.save(vc);
    }

    boolean verify2FACode(String email, String code) {
        return verificationCodeRepository
                .findByEmailAndCodeAndPurposeAndUsedFalseAndExpiresAtAfter(
                        email.toLowerCase().trim(),
                        code.trim(),
                        "2FA_LOGIN",
                        LocalDateTime.now()
                )
                .map(vc -> {
                    vc.setUsed(true);
                    verificationCodeRepository.save(vc);
                    return true;
                })
                .orElse(false);
    }

    private String extractIp() {
        try {
            var req = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String forwarded = req.getHeader("X-Forwarded-For");
            return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private String extractUserAgent() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest().getHeader("User-Agent");
        } catch (Exception e) {
            return null;
        }
    }

    private String randomSixDigits() {
        int n = RNG.nextInt(1_000_000);
        return String.format("%06d", n);
    }

    private String faceVerificationMessage(String code) {
        if (code == null || code.isBlank()) {
            return "Visage non reconnu.";
        }

        return switch (code) {
            case "no_face_detected" -> "Aucun visage détecté. Regardez la caméra et réessayez.";
            case "multiple_faces_detected" -> "Plusieurs visages ont été détectés. Gardez une seule personne dans le cadre.";
            case "low_quality" -> "Image trop floue ou trop lointaine. Rapprochez-vous et réessayez.";
            case "not_enrolled" -> "Aucun visage n'est enregistré pour ce compte. Reconfigurez la reconnaissance faciale depuis votre profil.";
            case "invalid_image" -> "L'image capturée est invalide. Veuillez réessayer.";
            case "service_error" -> "Le service de reconnaissance faciale a rencontré une erreur. Veuillez réessayer.";
            default -> "Visage non reconnu.";
        };
    }

    private Object mapToUserResponse(AppUser user) {
        if (user instanceof Patient) {
            return patientService.getPatientById(user.getId());
        }
        if (user instanceof Doctor) {
            return doctorService.getDoctorById(user.getId());
        }
        UserType type = user.getUserType();
        return AppUserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .userType(type)
                .isActive(user.isActive())
                .isVerified(user.getIsVerified())
                .profilePicture(user.getProfilePicture())
                .createdAt(user.getCreatedAt())
                .twoFactorEnabled(user.isTwoFactorEnabled())
                .faceEnabled(user.isFaceEnabled())
                .faceEnrolled(user.isFaceEnrolled())
                .build();
    }

    private byte[] decodeBase64Image(String faceImage) {
        String normalized = faceImage;
        int prefixIndex = normalized.indexOf("base64,");
        if (prefixIndex >= 0) {
            normalized = normalized.substring(prefixIndex + 7);
        }
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image faciale invalide.", ex);
        }
    }
}
