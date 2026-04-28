package skylinkers.tn.mediconnectbackend.controller.UserController;

import skylinkers.tn.mediconnectbackend.dto.request.Google2FAVerifyRequest;
import skylinkers.tn.mediconnectbackend.dto.request.GoogleFaceVerifyRequest;
import skylinkers.tn.mediconnectbackend.dto.request.GoogleLinkConfirmRequest;
import skylinkers.tn.mediconnectbackend.dto.request.GoogleLoginRequest;
import skylinkers.tn.mediconnectbackend.dto.request.GoogleRegisterRequest;
import skylinkers.tn.mediconnectbackend.dto.response.GoogleLoginResponse;
import skylinkers.tn.mediconnectbackend.service.GoogleAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Google Sign-In endpoints — all public (no Bearer JWT required).
 * The Google ID token itself is the credential; the backend verifies it
 * server-side with Google's official library.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    /** Step 1: Verify Google ID token → returns success, isNewUser, requires2FA, or requiresLinking */
    @PostMapping("/google-login")
    public ResponseEntity<GoogleLoginResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("[GOOGLE] POST /google-login");
        return ResponseEntity.ok(googleAuthService.handleGoogleLogin(request.getIdToken(), httpRequest));
    }

    /** Step 2a: New user completes role selection + registration form */
    @PostMapping("/google-register")
    public ResponseEntity<GoogleLoginResponse> googleRegister(
            @Valid @RequestBody GoogleRegisterRequest request,
            HttpServletRequest httpRequest) {
        log.info("[GOOGLE] POST /google-register — {}", request.getEmail());
        return ResponseEntity.ok(googleAuthService.registerGoogleUser(request, httpRequest));
    }

    /** Step 2b: Existing user confirms account linking with current password */
    @PostMapping("/google-link")
    public ResponseEntity<GoogleLoginResponse> googleLink(
            @Valid @RequestBody GoogleLinkConfirmRequest request,
            HttpServletRequest httpRequest) {
        log.info("[GOOGLE] POST /google-link — {}", request.getEmail());
        return ResponseEntity.ok(googleAuthService.confirmLink(request, httpRequest));
    }

    /** Step 2c: 2FA code verification for Google login */
    @PostMapping("/google-2fa-verify")
    public ResponseEntity<GoogleLoginResponse> google2FAVerify(
            @Valid @RequestBody Google2FAVerifyRequest request,
            HttpServletRequest httpRequest) {
        log.info("[GOOGLE] POST /google-2fa-verify — {}", request.getEmail());
        return ResponseEntity.ok(googleAuthService.verify2FA(request, httpRequest));
    }

    /** Step 2d: Face verification for Google login (when requiresFace=true) */
    @PostMapping("/google-face-verify")
    public ResponseEntity<GoogleLoginResponse> googleFaceVerify(
            @Valid @RequestBody GoogleFaceVerifyRequest request,
            HttpServletRequest httpRequest) {
        log.info("[GOOGLE] POST /google-face-verify — {}", request.getEmail());
        return ResponseEntity.ok(googleAuthService.verifyFace(request, httpRequest));
    }
}
