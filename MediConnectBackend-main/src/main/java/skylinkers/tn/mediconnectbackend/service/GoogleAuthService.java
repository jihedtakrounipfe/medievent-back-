package skylinkers.tn.mediconnectbackend.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.dto.request.Google2FAVerifyRequest;
import skylinkers.tn.mediconnectbackend.dto.request.GoogleFaceVerifyRequest;
import skylinkers.tn.mediconnectbackend.dto.request.GoogleLinkConfirmRequest;
import skylinkers.tn.mediconnectbackend.dto.request.GoogleRegisterRequest;
import skylinkers.tn.mediconnectbackend.dto.response.GoogleLoginResponse;
import skylinkers.tn.mediconnectbackend.dto.response.GoogleUserInfo;
import skylinkers.tn.mediconnectbackend.service.FaceRecognitionService;
import skylinkers.tn.mediconnectbackend.service.MfaSessionService;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.entities.VerificationCode;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import skylinkers.tn.mediconnectbackend.exception.DuplicateEmailException;
import skylinkers.tn.mediconnectbackend.exception.InvalidGoogleTokenException;
import skylinkers.tn.mediconnectbackend.exception.InvalidPasswordException;
import skylinkers.tn.mediconnectbackend.exception.ResourceNotFoundException;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.repository.VerificationCodeRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakAdminClient;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient;
import skylinkers.tn.mediconnectbackend.service.security.KeycloakTokenClient.KeycloakTokenResponse;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleAuthService {

    private static final SecureRandom RNG = new SecureRandom();

    @Value("${google.client-id}")
    private String googleClientId;

    private final AppUserRepository         appUserRepository;
    private final VerificationCodeRepository verificationCodeRepository;
    private final KeycloakTokenClient       keycloakTokenClient;
    private final KeycloakAdminClient       keycloakAdminClient;
    private final EmailService              emailService;
    private final AuditLogService           auditLogService;
    private final FaceRecognitionService    faceRecognitionService;
    private final MfaSessionService         mfaSessionService;
    private final TwoFactorService          twoFactorService;

    // ── Token Verification ────────────────────────────────────────────────────

    public GoogleUserInfo verifyGoogleToken(String idToken) {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken googleIdToken;
        try {
            googleIdToken = verifier.verify(idToken);
        } catch (Exception e) {
            throw new InvalidGoogleTokenException("Erreur lors de la vérification du token Google");
        }

        if (googleIdToken == null) {
            throw new InvalidGoogleTokenException("Token Google invalide ou expiré");
        }

        GoogleIdToken.Payload payload = googleIdToken.getPayload();

        if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
            throw new InvalidGoogleTokenException("L'adresse e-mail Google n'est pas vérifiée");
        }

        return GoogleUserInfo.builder()
                .email(payload.getEmail())
                .firstName((String) payload.get("given_name"))
                .lastName((String) payload.get("family_name"))
                .pictureUrl((String) payload.get("picture"))
                .googleId(payload.getSubject())
                .build();
    }

    // ── Main Login Entry Point ────────────────────────────────────────────────

    @Transactional
    public GoogleLoginResponse handleGoogleLogin(String idToken, HttpServletRequest request) {
        GoogleUserInfo googleUser = verifyGoogleToken(idToken);
        String email = googleUser.getEmail();

        log.info("[GOOGLE] Login attempt for {}", email);

        Optional<AppUser> existingUser = appUserRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            AppUser user = existingUser.get();

            if (!user.isActive()) {
                auditLogService.log(user, "GOOGLE_LOGIN_FAILED", getIp(request), getAgent(request), false,
                        "Account deactivated");
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Votre compte a été désactivé. Contactez le support.");
            }

            if (user.getGoogleId() == null) {
                // Existing email+password account — ask to link
                auditLogService.log(user, "GOOGLE_LINK_REQUESTED", getIp(request), getAgent(request), false,
                        "Google login on non-linked account");
                return GoogleLoginResponse.requiresLinking(email, googleUser);
            }

            return processExistingUserLogin(user, request);
        }

        // New user
        log.info("[GOOGLE] New user — redirecting to registration: {}", email);
        return GoogleLoginResponse.newUser(googleUser);
    }

    // ── Existing User Login ───────────────────────────────────────────────────

    private GoogleLoginResponse processExistingUserLogin(AppUser user, HttpServletRequest request) {
        // Build ordered list of enabled MFA methods
        List<String> enabledMethods = twoFactorService.getEnabledMethods(user);

        if (enabledMethods.isEmpty()) {
            KeycloakTokenResponse tokens = getTokensForUser(user);
            auditLogService.log(user, "GOOGLE_LOGIN_SUCCESS", getIp(request), getAgent(request), true, null);
            log.info("[GOOGLE] Login success for {}", user.getEmail());
            return GoogleLoginResponse.success(tokens, user);
        }

        // Get tokens now (stored in session for MFA step)
        KeycloakTokenResponse tokens = getTokensForUser(user);

        String primaryMethod = enabledMethods.get(0);
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
                null); // Google OAuth login bypasses risk scoring

        MfaSessionService.MfaSessionData session = mfaSessionService.get(sessionToken).orElseThrow();

        auditLogService.log(user, "GOOGLE_LOGIN_MFA_REQUIRED",
                getIp(request), getAgent(request), true, "methods=" + enabledMethods);
        log.info("[GOOGLE] MFA required for {}, methods={}", user.getEmail(), enabledMethods);

        return GoogleLoginResponse.builder()
                .success(false)
                .requiresMfa(true)
                .mfaSessionToken(sessionToken)
                .enabledMethods(enabledMethods)
                .primaryMethod(primaryMethod)
                .attemptsRemaining(session.attemptsRemaining())
                .email(user.getEmail())
                .message(emailOtpSent
                        ? "Un code de vérification a été envoyé à votre adresse e-mail."
                        : null)
                .build();
    }

    // ── Face Verification ─────────────────────────────────────────────────────

    @Transactional
    public GoogleLoginResponse verifyFace(GoogleFaceVerifyRequest req, HttpServletRequest request) {
        AppUser user = appUserRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (Boolean.TRUE.equals(req.getUseEmailFallback())) {
            String code = randomSixDigits();
            save2FACode(user.getEmail(), code);
            emailService.send2FACode(user.getEmail(), user.getFirstName(), code);
            auditLogService.log(user, "GOOGLE_LOGIN_FACE_FALLBACK", getIp(request), getAgent(request), true, "User requested email fallback");
            return GoogleLoginResponse.faceFallback(user.getEmail());
        }

        if (req.getFaceImage() == null || req.getFaceImage().isBlank()) {
            return GoogleLoginResponse.requiresFace(user.getEmail());
        }

        try {
            byte[] imageBytes = decodeFaceImage(req.getFaceImage());
            FaceRecognitionService.FaceVerificationResult result =
                    faceRecognitionService.verifyFace(user, imageBytes);

            if (!result.matched()) {
                auditLogService.log(user, "GOOGLE_LOGIN_FACE_FAILED", getIp(request), getAgent(request), false, result.message());
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Visage non reconnu. Veuillez réessayer.");
            }

            auditLogService.log(user, "GOOGLE_LOGIN_FACE_SUCCESS", getIp(request), getAgent(request), true, null);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("[GOOGLE] Face service unavailable for {}: {}", user.getEmail(), ex.getMessage());
            String code = randomSixDigits();
            save2FACode(user.getEmail(), code);
            emailService.send2FACode(user.getEmail(), user.getFirstName(), code);
            auditLogService.log(user, "GOOGLE_LOGIN_FACE_FALLBACK", getIp(request), getAgent(request), true,
                    "Face service unavailable: " + ex.getMessage());
            return GoogleLoginResponse.faceFallback(user.getEmail());
        }

        KeycloakTokenResponse tokens = getTokensForUser(user);
        auditLogService.log(user, "GOOGLE_LOGIN_SUCCESS", getIp(request), getAgent(request), true, "via face");
        return GoogleLoginResponse.success(tokens, user);
    }

    // ── 2FA Verification ──────────────────────────────────────────────────────

    @Transactional
    public GoogleLoginResponse verify2FA(Google2FAVerifyRequest req, HttpServletRequest request) {
        AppUser user = appUserRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        if (!verify2FACode(req.getEmail(), req.getOtpCode())) {
            auditLogService.log(user, "GOOGLE_LOGIN_2FA_FAILED", getIp(request), getAgent(request), false,
                    "Invalid OTP");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Code de vérification invalide ou expiré.");
        }

        KeycloakTokenResponse tokens = getTokensForUser(user);
        auditLogService.log(user, "GOOGLE_LOGIN_2FA_SUCCESS", getIp(request), getAgent(request), true, null);
        auditLogService.log(user, "GOOGLE_LOGIN_SUCCESS", getIp(request), getAgent(request), true, "via 2FA");
        return GoogleLoginResponse.success(tokens, user);
    }

    // ── Account Linking ───────────────────────────────────────────────────────

    @Transactional
    public GoogleLoginResponse confirmLink(GoogleLinkConfirmRequest req, HttpServletRequest request) {
        AppUser user = appUserRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Verify current password
        try {
            keycloakTokenClient.passwordGrant(req.getEmail(), req.getCurrentPassword());
        } catch (Exception e) {
            auditLogService.log(user, "GOOGLE_LINK_FAILED", getIp(request), getAgent(request), false,
                    "Wrong password");
            throw new InvalidPasswordException("Mot de passe incorrect");
        }

        // Generate internal password and update Keycloak
        String internalPassword = generateInternalPassword();
        keycloakAdminClient.resetPasswordById(user.getKeycloakId(), internalPassword);
        user.setGoogleId(req.getGoogleId());
        user.setGoogleInternalPassword(internalPassword);
        appUserRepository.save(user);

        auditLogService.log(user, "GOOGLE_LINK_SUCCESS", getIp(request), getAgent(request), true, null);
        log.info("[GOOGLE] Account linked for {}", req.getEmail());

        return processExistingUserLogin(user, request);
    }

    // ── Google Registration ───────────────────────────────────────────────────

    @Transactional
    public GoogleLoginResponse registerGoogleUser(GoogleRegisterRequest req, HttpServletRequest request) {
        String email = req.getEmail();

        if (appUserRepository.existsByEmail(email)) {
            throw new DuplicateEmailException("Un compte avec cet e-mail existe déjà");
        }

        // Password: use provided one or generate internal random password
        String internalPassword = (req.getPassword() != null && !req.getPassword().isBlank())
                ? req.getPassword()
                : generateInternalPassword();

        String roleName = "DOCTOR".equals(req.getRole()) ? "ROLE_DOCTOR_GP" : "ROLE_PATIENT";
        Map<String, List<String>> attributes = Map.of("user_type", List.of(req.getRole()));

        String keycloakId = keycloakAdminClient.createGoogleUser(
                email, internalPassword,
                req.getFirstName(), req.getLastName(),
                roleName, attributes
        );

        AppUser user;
        if ("DOCTOR".equals(req.getRole())) {
            Doctor doctor = Doctor.builder()
                    .keycloakId(keycloakId)
                    .email(email)
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .phone(req.getPhone())
                    .googleId(req.getGoogleId())
                    .googleInternalPassword(internalPassword)
                    .profilePicture(req.getPictureUrl())
                    .isActive(true)
                    .isVerified(true)
                    .twoFactorEnabled(false)
                    .rppsNumber(req.getRppsNumber())
                    .specialization(req.getSpecialization())
                    .consultationDuration(req.getConsultationDuration())
                    .consultationFee(req.getConsultationFee())
                    .officeAddress(req.getOfficeAddress())
                    .licenseNumber(req.getLicenseNumber())
                    .verificationStatus(VerificationStatus.PENDING)
                    .build();
            user = appUserRepository.save(doctor);
        } else {
            Patient patient = Patient.builder()
                    .keycloakId(keycloakId)
                    .email(email)
                    .firstName(req.getFirstName())
                    .lastName(req.getLastName())
                    .phone(req.getPhone())
                    .googleId(req.getGoogleId())
                    .googleInternalPassword(internalPassword)
                    .profilePicture(req.getPictureUrl())
                    .isActive(true)
                    .isVerified(true)
                    .twoFactorEnabled(false)
                    .dateOfBirth(req.getDateOfBirth())
                    .gender(req.getGender())
                    .address(req.getAddress())
                    .build();
            user = appUserRepository.save(patient);
        }

        emailService.sendWelcomeEmail(email, req.getFirstName());

        String auditAction = "DOCTOR".equals(req.getRole()) ? "GOOGLE_SIGNUP_DOCTOR" : "GOOGLE_SIGNUP_PATIENT";
        auditLogService.log(user, auditAction, getIp(request), getAgent(request), true, "Registered via Google");

        log.info("[GOOGLE] New {} registered: {}", req.getRole(), email);

        // Doctors go to pending approval — don't issue tokens yet
        if ("DOCTOR".equals(req.getRole())) {
            return GoogleLoginResponse.builder()
                    .success(true)
                    .isNewUser(false)
                    .email(email)
                    .userType("DOCTOR")
                    .message("Votre compte médecin est en attente de vérification.")
                    .build();
        }

        KeycloakTokenResponse tokens = getTokensForUser(user);
        return GoogleLoginResponse.success(tokens, user);
    }

    // ── Token Helper ──────────────────────────────────────────────────────────

    KeycloakTokenResponse getTokensForUser(AppUser user) {
        String pwd = user.getGoogleInternalPassword();
        if (pwd == null || pwd.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de générer des tokens — mot de passe interne manquant pour " + user.getEmail());
        }
        return keycloakTokenClient.passwordGrant(user.getEmail(), pwd);
    }

    // ── 2FA Code Helpers ──────────────────────────────────────────────────────

    void save2FACode(String email, String code) {
        List<VerificationCode> old = verificationCodeRepository
                .findByEmailAndPurposeAndUsedFalse(email, "2FA_LOGIN");
        old.forEach(c -> c.setUsed(true));
        if (!old.isEmpty()) verificationCodeRepository.saveAll(old);

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

    // ── Private Utilities ─────────────────────────────────────────────────────

    private String generateInternalPassword() {
        byte[] bytes = new byte[48];
        RNG.nextBytes(bytes);
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String randomSixDigits() {
        return String.format("%06d", RNG.nextInt(1_000_000));
    }

    private byte[] decodeFaceImage(String faceImage) {
        String normalized = faceImage;
        int idx = normalized.indexOf("base64,");
        if (idx >= 0) normalized = normalized.substring(idx + 7);
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image faciale invalide.", ex);
        }
    }

    private String getIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        return (forwarded != null && !forwarded.isBlank()) ? forwarded.split(",")[0].trim() : req.getRemoteAddr();
    }

    private String getAgent(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }
}
