package skylinkers.tn.mediconnectbackend.controller.UserController;

import skylinkers.tn.mediconnectbackend.dto.request.CreatePatientRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.AuditLogResponse;
import skylinkers.tn.mediconnectbackend.dto.response.PatientResponse;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.PatientService;
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

/**
 * SOLID — SRP: Only patient CRUD + RGPD endpoints.
 *              Appointment booking is in the consultation module.
 *
 * RBAC:
 *   ROLE_PATIENT  — own profile only
 *   ROLE_ADMIN    — all patients
 *   ROLE_DOCTOR_* — read-only on their assigned patients (handled in service)
 */
@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService  patientService;
    private final AuditLogService auditLogService;

    // ── Registration (called by Keycloak webhook after user confirms email) ──

    @PostMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.createPatient(request));
    }

    // ── Own profile (patient self-service) ──────────────────────────────────

    @GetMapping("/me")
//    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientResponse> getMyProfile(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId = jwt.getSubject();
        return ResponseEntity.ok(patientService.getPatientByKeycloakId(keycloakId));
    }

    @PatchMapping("/me")
//    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientResponse> updateMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest request) {
        PatientResponse profile = patientService.getPatientByKeycloakId(jwt.getSubject());
        return ResponseEntity.ok(patientService.updateProfile(profile.getId(), request));
    }

    /** RGPD Art. 17 — right to erasure. */
    @DeleteMapping("/me")
//    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Void> eraseMyData(@AuthenticationPrincipal Jwt jwt) {
        PatientResponse profile = patientService.getPatientByKeycloakId(jwt.getSubject());
        patientService.erasePatientData(profile.getId());
        return ResponseEntity.noContent().build();
    }

    /** RGPD Art. 20 — right to data portability. */
    @GetMapping("/me/export")
//    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Object> exportMyData(@AuthenticationPrincipal Jwt jwt) {
        PatientResponse profile = patientService.getPatientByKeycloakId(jwt.getSubject());
        return ResponseEntity.ok(patientService.exportPatientData(profile.getId()));
    }

    @GetMapping("/me/audit-logs")
//    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<Page<AuditLogResponse>> getMyAuditLogs(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {
        PatientResponse profile = patientService.getPatientByKeycloakId(jwt.getSubject());
        return ResponseEntity.ok(auditLogService.getLogsForUser(profile.getId(), pageable));
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────

    @GetMapping
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<PatientResponse>> getAllPatients(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(patientService.getAllPatients(pageable));
    }

    @GetMapping("/{id}")
//    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR_GP', 'DOCTOR_SPECIALIST')")
    public ResponseEntity<PatientResponse> getPatientById(@PathVariable Long id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PatchMapping("/{id}/deactivate")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivatePatient(@PathVariable Long id) {
        patientService.deactivatePatient(id);
        return ResponseEntity.noContent().build();
    }
}
