package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

import skylinkers.tn.mediconnectbackend.dto.request.LoginRequest;
import skylinkers.tn.mediconnectbackend.dto.request.LoginWith2FARequest;
import skylinkers.tn.mediconnectbackend.dto.request.ResendVerificationRequest;
import skylinkers.tn.mediconnectbackend.dto.request.VerifyEmailRequest;
import skylinkers.tn.mediconnectbackend.dto.response.AuthResponse;
import skylinkers.tn.mediconnectbackend.dto.response.PatientResponse;
import skylinkers.tn.mediconnectbackend.dto.response.DoctorResponse;
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

    /** Invalidate the Keycloak session + revoke Google refresh token if linked. */
    void logout(String keycloakId);

    /** Exchange a Keycloak refresh token for a new access token. */
    AuthResponse refreshToken(String refreshToken);
}
