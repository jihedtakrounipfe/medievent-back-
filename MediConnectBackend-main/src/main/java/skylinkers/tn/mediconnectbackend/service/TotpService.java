package skylinkers.tn.mediconnectbackend.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.RecoveryCode;
import skylinkers.tn.mediconnectbackend.entities.enums.AuditAction;
import skylinkers.tn.mediconnectbackend.repository.RecoveryCodeRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TotpService {

    private static final int RECOVERY_CODE_COUNT = 8;
    private static final int RECOVERY_CODE_LENGTH = 10;
    private static final String ISSUER = "MediConnect";
    private static final String RECOVERY_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final AppUserRepository appUserRepository;
    private final RecoveryCodeRepository recoveryCodeRepository;
    private final AuditLogService auditLogService;

    private final DefaultSecretGenerator secretGenerator = new DefaultSecretGenerator(32);
    private final BCryptPasswordEncoder bcrypt = new BCryptPasswordEncoder();

    /** Generate a fresh Base32 TOTP shared secret. */
    public String generateSecret() {
        return secretGenerator.generate();
    }

    /**
     * Build a QR code PNG for the given secret + email and return it as a
     * data URI (data:image/png;base64,...) ready for an <img> tag.
     */
    public String generateQrCodeBase64(String secret, String userEmail) {
        QrData qrData = new QrData.Builder()
                .label(userEmail)
                .secret(secret)
                .issuer(ISSUER)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(qrData.getUri(), BarcodeFormat.QR_CODE, 300, 300);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException e) {
            log.error("[TOTP] QR generation failed for {}: {}", userEmail, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Impossible de générer le QR code.", e);
        }
    }

    /** Verify a 6-digit TOTP code with ±1 period (30 s) tolerance. */
    public boolean verifyCode(String secret, String code) {
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(
                new DefaultCodeGenerator(HashingAlgorithm.SHA1, 6),
                new SystemTimeProvider()
        );
        verifier.setTimePeriod(30);
        verifier.setAllowedTimePeriodDiscrepancy(1);
        return verifier.isValidCode(secret, code);
    }

    /**
     * Step 1: Generate a fresh secret, persist it as a pending (not-yet-enabled) secret,
     * and return the QR code data URI + raw secret for manual entry.
     *
     * The secret is saved to totpSecret but totpEnabled/totpEnrolled remain false until
     * confirmTotpSetup() succeeds.
     */
    @Transactional
    public TotpSetupData initiateTotpSetup(AppUser user) {
        if (!user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Activez d'abord la verification par e-mail avant de configurer TOTP.");
        }
        String secret = generateSecret();
        user.setTotpSecret(secret);
        appUserRepository.save(user);
        String qrCodeDataUri = generateQrCodeBase64(secret, user.getEmail());
        auditLogService.log(user, AuditAction.TOTP_SETUP_INITIATED.name(), null, null, true, null);
        return new TotpSetupData(secret, qrCodeDataUri);
    }

    public record TotpSetupData(String secret, String qrCodeDataUri) {}

    /**
     * Step 2: Verify the user's first TOTP code against the pending secret,
     * enable the method, and return 8 single-use recovery codes (shown once).
     *
     * @throws ResponseStatusException 400 if no pending secret or code is invalid
     */
    @Transactional
    public List<String> confirmTotpSetup(AppUser user, String verificationCode) {
        if (!user.isTwoFactorEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La verification par e-mail doit rester active pour utiliser TOTP.");
        }
        String pendingSecret = user.getTotpSecret();
        if (pendingSecret == null || user.isTotpEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Aucune configuration TOTP en attente.");
        }
        if (!verifyCode(pendingSecret, verificationCode)) {
            auditLogService.log(user, AuditAction.TOTP_SETUP_CANCELLED.name(), null, null, false,
                    "Verification code mismatch during setup");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Code de vérification invalide. Veuillez réessayer.");
        }
        user.setTotpEnabled(true);
        user.setTotpEnrolled(true);
        appUserRepository.save(user);

        recoveryCodeRepository.deleteAllByUserId(user.getId());
        List<String> plainCodes = generateAndSaveRecoveryCodes(user);
        auditLogService.log(user, AuditAction.TOTP_SETUP_COMPLETED.name(), null, null, true, null);
        log.info("[TOTP] Setup completed for userId={}", user.getId());
        return plainCodes;
    }

    /** Cancel an in-progress setup by clearing the pending secret. */
    @Transactional
    public void cancelTotpSetup(AppUser user) {
        if (!user.isTotpEnabled()) {
            user.setTotpSecret(null);
            appUserRepository.save(user);
        }
    }

    /** Disable TOTP: wipe secret + flags + all recovery codes. */
    @Transactional
    public void disableTotp(AppUser user) {
        user.setTotpSecret(null);
        user.setTotpEnabled(false);
        user.setTotpEnrolled(false);
        appUserRepository.save(user);
        recoveryCodeRepository.deleteAllByUserId(user.getId());
        auditLogService.log(user, AuditAction.TOTP_DISABLED.name(), null, null, true, null);
        log.info("[TOTP] Disabled for userId={}", user.getId());
    }

    /**
     * Match a raw recovery code against unused BCrypt hashes.
     * Marks the matching code as used on success.
     *
     * @return true if a matching unused code was found
     */
    @Transactional
    public boolean verifyRecoveryCode(AppUser user, String rawCode) {
        List<RecoveryCode> unused = recoveryCodeRepository.findByUserIdAndUsedFalse(user.getId());
        for (RecoveryCode rc : unused) {
            if (bcrypt.matches(rawCode, rc.getCodeHash())) {
                rc.setUsed(true);
                recoveryCodeRepository.save(rc);
                auditLogService.log(user, AuditAction.RECOVERY_CODE_USED.name(), null, null, true, null);
                return true;
            }
        }
        auditLogService.log(user, AuditAction.RECOVERY_CODE_FAILED.name(), null, null, false, null);
        return false;
    }

    /**
     * Invalidate all existing recovery codes and generate 8 fresh ones.
     *
     * @return plaintext recovery codes
     */
    @Transactional
    public List<String> regenerateRecoveryCodes(AppUser user) {
        recoveryCodeRepository.deleteAllByUserId(user.getId());
        List<String> plainCodes = generateAndSaveRecoveryCodes(user);
        auditLogService.log(user, AuditAction.RECOVERY_CODES_REGENERATED.name(), null, null, true, null);
        return plainCodes;
    }

    /** Count how many single-use recovery codes the user has not yet consumed. */
    public long countRemainingRecoveryCodes(AppUser user) {
        return recoveryCodeRepository.countByUserIdAndUsedFalse(user.getId());
    }

    // ── private helpers ─────────────────────────────────────────────────────

    private List<String> generateAndSaveRecoveryCodes(AppUser user) {
        SecureRandom rng = new SecureRandom();
        List<String> plainCodes = new ArrayList<>(RECOVERY_CODE_COUNT);
        List<RecoveryCode> entities = new ArrayList<>(RECOVERY_CODE_COUNT);

        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            String plain = randomCode(rng);
            plainCodes.add(plain);
            entities.add(RecoveryCode.builder()
                    .user(user)
                    .codeHash(bcrypt.encode(plain))
                    .used(false)
                    .build());
        }
        recoveryCodeRepository.saveAll(entities);
        return plainCodes;
    }

    private String randomCode(SecureRandom rng) {
        StringBuilder sb = new StringBuilder(RECOVERY_CODE_LENGTH);
        for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
            sb.append(RECOVERY_CHARS.charAt(rng.nextInt(RECOVERY_CHARS.length())));
        }
        return sb.toString();
    }
}
