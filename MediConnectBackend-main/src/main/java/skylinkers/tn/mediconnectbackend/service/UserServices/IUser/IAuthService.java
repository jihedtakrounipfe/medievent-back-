package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

import jakarta.servlet.http.HttpServletRequest;
import skylinkers.tn.mediconnectbackend.dto.request.ChangePasswordRequest;
import skylinkers.tn.mediconnectbackend.dto.request.LoginRequest;
import skylinkers.tn.mediconnectbackend.dto.request.LoginWith2FARequest;
import skylinkers.tn.mediconnectbackend.dto.request.ResendVerificationRequest;
import skylinkers.tn.mediconnectbackend.dto.request.VerifyEmailRequest;
import skylinkers.tn.mediconnectbackend.dto.response.AuthResponse;
import skylinkers.tn.mediconnectbackend.dto.request.CreatePatientRequest;
import skylinkers.tn.mediconnectbackend.dto.request.CreateDoctorRequest;

/**
 * SOLID — OCP: new auth strategies (Google, biometric) extend this
 *               without modifying existing implementations.
 */
public interface IAuthService {

    /** Authenticate with email + password. Returns tokens or requires2FA flag. */
    AuthResponse login(LoginRequest request);

    /** Second step when 2FA is enabled — validates TOTP + issues tokens. */
    AuthResponse loginWith2FA(LoginWith2FARequest request);

    /** Register a new patient — triggers Keycloak account creation + email verification. */
    AuthResponse registerPatient(CreatePatientRequest request);

    /** Register a new doctor — account set to PENDING until admin approves. */
    AuthResponse registerDoctor(CreateDoctorRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    AuthResponse resendVerification(ResendVerificationRequest request);

    // ── Two-step password change ──────────────────────────────────────────────────

    /**
     * Step 1a: Generate and email a 6-digit verification code to the user.
     * Rate-limited to 3 sends per hour per email.
     *
     * @param email       extracted from JWT (user does NOT type this)
     * @param httpRequest for rate-limiting and audit context
     */
    void sendPasswordChangeCode(String email, HttpServletRequest httpRequest);

    /**
     * Step 1b: Verify the 6-digit code and return a short-lived UUID token (valid 5 min).
     * The token is stored in the verification_codes table with purpose=PASSWORD_CHANGE_TOKEN.
     *
     * @param email extracted from JWT
     * @param code  the 6-digit code entered by the user
     * @return      a UUID verification token to be passed to changePassword()
     * @throws org.springframework.web.server.ResponseStatusException 400 if code invalid/expired
     */
    String verifyPasswordChangeCode(String email, String code);

    /**
     * Step 2: Change the authenticated user's password.
     * Validates the verification token from step 1b, then verifies the current
     * password via Keycloak, then resets via Admin API.
     *
     * @param userEmail         the current user's email (from JWT)
     * @param keycloakId        the current user's Keycloak UUID (from JWT)
     * @param request           the password change request (verificationToken + current + new + confirm)
     */
    void changePassword(String userEmail, String keycloakId, ChangePasswordRequest request);

    /** Invalidate the Keycloak session + revoke Google refresh token if linked. */
    void logout(String keycloakId);

    /** Exchange a Keycloak refresh token for a new access token. */
    AuthResponse refreshToken(String refreshToken);
}
