package skylinkers.tn.mediconnectbackend.controller.UserController;

import skylinkers.tn.mediconnectbackend.dto.request.CreateDoctorRequest;
import skylinkers.tn.mediconnectbackend.dto.request.DoctorProfileUpdateRequest;
import skylinkers.tn.mediconnectbackend.dto.request.DoctorStatusReasonRequest;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/doctors", "/api/doctors"})
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

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('DOCTOR_GP', 'DOCTOR_SPECIALIST')")
    public ResponseEntity<DoctorResponse> updateMyProfileById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @Valid @RequestBody DoctorProfileUpdateRequest request
    ) {
        DoctorResponse me = doctorService.getDoctorByKeycloakId(jwt.getSubject());
        if (!me.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé");
        }
        UpdateProfileRequest payload = new UpdateProfileRequest();
        payload.setFirstName(request.getFirstName());
        payload.setLastName(request.getLastName());
        payload.setPhone(request.getPhone());
        payload.setAddress(request.getAddress());
        payload.setProfilePicture(request.getProfilePicture());
        payload.setEmergencyContactName(request.getEmergencyContactName());
        payload.setEmergencyContactPhone(request.getEmergencyContactPhone());
        payload.setSpecialization(request.getSpecialization());
        payload.setLicenseNumber(request.getLicenseNumber());
        payload.setConsultationDuration(request.getConsultationDuration());
        payload.setConsultationFee(request.getConsultationFee());
        payload.setOfficeAddress(request.getOfficeAddress());
        return ResponseEntity.ok(doctorService.updateProfile(id, payload));
    }

    // ── Public discovery (patients searching for doctors) ───────────────────

    @GetMapping(params = "specialization")
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
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<DoctorResponse>> getPendingVerification() {
        return ResponseEntity.ok(doctorService.getPendingVerification());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<DoctorResponse>> getPending() {
        return ResponseEntity.ok(doctorService.getPendingVerification());
    }

    @GetMapping(params = {"!specialization"})
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<DoctorResponse>> getAllDoctorsForAdmin(
            @RequestParam(required = false) VerificationStatus status
    ) {
        return ResponseEntity.ok(doctorService.getDoctorsForAdmin(status));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<DoctorResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.approveDoctor(id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<DoctorResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody DoctorStatusReasonRequest request
    ) {
        return ResponseEntity.ok(doctorService.rejectDoctor(id, request.getReason()));
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<DoctorResponse> suspend(
            @PathVariable Long id,
            @Valid @RequestBody DoctorStatusReasonRequest request
    ) {
        return ResponseEntity.ok(doctorService.suspendDoctor(id, request.getReason()));
    }

    @PutMapping("/{id}/reactivate")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<DoctorResponse> reactivate(@PathVariable Long id) {
        return ResponseEntity.ok(doctorService.approveDoctor(id));
    }

    @PatchMapping("/{id}/verification")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<DoctorResponse> updateVerification(
            @PathVariable Long id,
            @RequestParam VerificationStatus status) {
        return ResponseEntity.ok(doctorService.updateVerificationStatus(id, status));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> deactivateDoctor(@PathVariable Long id) {
        doctorService.deactivateDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> activateDoctor(@PathVariable Long id) {
        doctorService.activateDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<DoctorResponse>> getAllDoctors(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(doctorService.getAllDoctors(pageable));
    }

    @GetMapping("/clinic/{clinicId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_GP', 'DOCTOR_SPECIALIST')")
    public ResponseEntity<List<DoctorResponse>> getDoctorsByClinic(@PathVariable Long clinicId) {
        return ResponseEntity.ok(doctorService.getDoctorsByClinic(clinicId));
    }
}
