/*
package skylinkers.tn.mediconnectbackend.Services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PatientServiceImpl implements PatientService {

    private final PatientRepository    patientRepository;
    private final AuditLogRepository   auditLogRepository;
    private final AuditLogService      auditLogService;
    private final PatientMapper        patientMapper;

    @Override
    @Transactional
    public PatientResponse createPatient(CreatePatientRequest request) {
        if (patientRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("Email already registered: " + request.getEmail());
        }
        Patient patient = patientMapper.toEntity(request);
        Patient saved   = patientRepository.save(patient);
        log.info("Patient created: id={}", saved.getId());
        auditLogService.log(saved, "ACCOUNT_CREATED", null, null, true, null);
        return patientMapper.toResponse(saved);
    }

    @Override
    public PatientResponse getPatientById(Long id) {
        return patientMapper.toResponse(findPatientOrThrow(id));
    }

    @Override
    public PatientResponse getPatientByKeycloakId(String keycloakId) {
        Patient patient = patientRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found for Keycloak ID: " + keycloakId));
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional
    public PatientResponse updateProfile(Long id, UpdateProfileRequest request) {
        Patient patient = findPatientOrThrow(id);
        patientMapper.applyUpdate(request, patient);
        Patient saved = patientRepository.save(patient);
        auditLogService.log(saved, "PROFILE_UPDATE", null, null, true, null);
        return patientMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void deactivatePatient(Long id) {
        Patient patient = findPatientOrThrow(id);
        patient.setActive(false);
        patientRepository.save(patient);
        auditLogService.log(patient, "ACCOUNT_DEACTIVATED", null, null, true, null);
        log.warn("Patient deactivated: id={}", id);
    }

    @Override
    @Transactional
    public void erasePatientData(Long id) {
        Patient patient = findPatientOrThrow(id);
        // Anonymise personal fields — keeps the row for referential integrity
        patient.setFirstName("[ERASED]");
        patient.setLastName("[ERASED]");
        patient.setEmail("erased+" + id + "@mediconnect.invalid");
        patient.setPhone(null);
        patient.setAddress(null);
        patient.setEmergencyContact(null);
        patient.setSocialSecurityNum(null);
        patient.setProfilePicture(null);
        patient.setActive(false);
        patientRepository.save(patient);
        // Anonymise audit trail (RGPD compliant — IPs removed)
        auditLogRepository.anonymiseByUserId(id);
        log.warn("Patient data erased (RGPD): id={}", id);
    }

    @Override
    public Object exportPatientData(Long id) {
        Patient patient = findPatientOrThrow(id);
        // Returns a structured map — caller serialises to JSON/PDF
        return Map.of(
                "profile",   patientMapper.toResponse(patient),
                "auditLogs", auditLogRepository.findByUserIdOrderByTimestampDesc(id, Pageable.ofSize(1000)).getContent()
        );
    }

    @Override
    public Page<PatientResponse> getAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable).map(patientMapper::toResponse);
    }

    @Override
    @Transactional
    public void updateNoShowScore(Long id, Double score) {
        Patient patient = findPatientOrThrow(id);
        patient.setNoShowScore(score);
        patientRepository.save(patient);
    }

    // ── Private helpers ──────────────────────────────────────────────

    private Patient findPatientOrThrow(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
    }
}

 */