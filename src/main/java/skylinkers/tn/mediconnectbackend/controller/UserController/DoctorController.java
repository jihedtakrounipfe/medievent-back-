package skylinkers.tn.mediconnectbackend.controller.UserController;

import skylinkers.tn.mediconnectbackend.dto.request.CreateDoctorRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.DoctorResponse;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    // ── Self-registration (account status: PENDING until admin approves) ────

    @PostMapping
    public ResponseEntity<DoctorResponse> registerDoctor(
            @Valid @RequestBody CreateDoctorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.createDoctor(request));
    }

    // ── Own profile ──────────────────────────────────────────────────────────

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('DOCTOR_GP', 'DOCTOR_SPECIALIST')")
    public ResponseEntity<DoctorResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(doctorService.getDoctorByKeycloakId(jwt.getSubject()));
    }

    @PatchMapping("/me")
    @PreAuthorize("hasAnyRole('DOCTOR_GP', 'DOCTOR_SPECIALIST')")
    public ResponseEntity<DoctorResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        DoctorResponse profile = doctorService.getDoctorByKeycloakId(jwt.getSubject());
        return ResponseEntity.ok(doctorService.updateProfile(profile.getId(), request));
    }

    // ── Public discovery (patients searching for doctors) ───────────────────

    @GetMapping
    public ResponseEntity<Page<DoctorResponse>> getDoctorsBySpecialization(
            @RequestParam Specialization specialization,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(doctorService.getDoctorsBySpecialization(specialization, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.getDoctorById(id));
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────

    @GetMapping("/pending-verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<DoctorResponse>> getPendingVerification() {
        return ResponseEntity.ok(doctorService.getPendingVerification());
    }

    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DoctorResponse> updateVerification(
            @PathVariable Long id,
            @RequestParam VerificationStatus status) {
        return ResponseEntity.ok(doctorService.updateVerificationStatus(id, status));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateDoctor(@PathVariable Long id) {
        doctorService.deactivateDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clinic/{clinicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_GP', 'DOCTOR_SPECIALIST')")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByClinic(@PathVariable Long clinicId) {
        return ResponseEntity.ok(doctorService.getDoctorsByClinic(clinicId));
    }
}