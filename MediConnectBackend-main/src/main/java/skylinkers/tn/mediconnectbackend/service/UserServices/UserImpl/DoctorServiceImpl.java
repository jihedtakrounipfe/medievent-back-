package skylinkers.tn.mediconnectbackend.service.UserServices.UserImpl;

import skylinkers.tn.mediconnectbackend.dto.request.CreateDoctorRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.DoctorResponse;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import skylinkers.tn.mediconnectbackend.exception.DuplicateEmailException;
import skylinkers.tn.mediconnectbackend.exception.ResourceNotFoundException;
import skylinkers.tn.mediconnectbackend.mapper.DoctorMapper;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.DoctorRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.DoctorService;
import skylinkers.tn.mediconnectbackend.utils.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final AuditLogService  auditLogService;
    private final DoctorMapper     doctorMapper;
    private final EmailService     emailService;

    @Override
    @Transactional
    public DoctorResponse createDoctor(CreateDoctorRequest request) {
        if (doctorRepository.existsByRppsNumber(request.getRppsNumber())) {
            throw new DuplicateEmailException("RPPS already registered: " + request.getRppsNumber());
        }
        Doctor doctor = doctorMapper.toEntity(request);
        Doctor saved  = doctorRepository.save(doctor);
        auditLogService.log(saved, "ACCOUNT_CREATED", null, null, true,
                "RPPS pending admin verification");
        log.info("Doctor created (PENDING): id={}", saved.getId());
        return doctorMapper.toResponse(saved);
    }

    @Override
    public DoctorResponse getDoctorById(Long id) {
        return doctorMapper.toResponse(findDoctorOrThrow(id));
    }

    @Override
    public DoctorResponse getDoctorByKeycloakId(String keycloakId) {
        Doctor doctor = doctorRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + keycloakId));
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse updateProfile(Long id, UpdateProfileRequest request) {
        Doctor doctor = findDoctorOrThrow(id);
        String before = snapshotProfile(doctor);
        doctorMapper.applyUpdate(request, doctor);
        Doctor saved = doctorRepository.save(doctor);
        String after = snapshotProfile(saved);
        if (!Objects.equals(before, after)) {
            auditLogService.logProfileChange(
                    skylinkers.tn.mediconnectbackend.entities.enums.AuditAction.PROFILE_UPDATED,
                    saved,
                    before,
                    after,
                    null
            );
        }
        return doctorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DoctorResponse updateVerificationStatus(Long id, VerificationStatus status) {
        return setVerificationStatus(id, status, null);
    }

    @Override
    @Transactional
    public DoctorResponse setVerificationStatus(Long id, VerificationStatus status, String reason) {
        Doctor doctor = findDoctorOrThrow(id);
        VerificationStatus oldStatus = doctor.getVerificationStatus();

        if (Objects.equals(oldStatus, status)) {
            return doctorMapper.toResponse(doctor);
        }

        doctor.setVerificationStatus(status);
        Doctor saved = doctorRepository.save(doctor);
        auditLogService.log(saved, "DOCTOR_VERIFICATION_" + status.name(), null, null, true, reason);
        log.info("Doctor verification updated: id={}, oldStatus={}, newStatus={}", id, oldStatus, status);

        String doctorName = (saved.getFirstName() + " " + saved.getLastName()).trim();
        sendDoctorStatusEmail(saved.getEmail(), doctorName, oldStatus, status, reason);

        return doctorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DoctorResponse approveDoctor(Long id) {
        return setVerificationStatus(id, VerificationStatus.APPROVED, null);
    }

    @Override
    @Transactional
    public DoctorResponse rejectDoctor(Long id, String reason) {
        return setVerificationStatus(id, VerificationStatus.REJECTED, reason);
    }

    @Override
    @Transactional
    public DoctorResponse suspendDoctor(Long id, String reason) {
        return setVerificationStatus(id, VerificationStatus.SUSPENDED, reason);
    }

    @Override
    public List<DoctorResponse> getDoctorsForAdmin(VerificationStatus status) {
        if (status == null) {
            return doctorRepository.findAll().stream().map(doctorMapper::toResponse).toList();
        }
        return doctorRepository.findByVerificationStatus(status).stream().map(doctorMapper::toResponse).toList();
    }

    @Override
    public Page<DoctorResponse> getDoctorsBySpecialization(Specialization specialization, Pageable pageable) {
        return doctorRepository
                .findBySpecializationAndVerificationStatusAndIsActiveTrue(
                        specialization, VerificationStatus.APPROVED, pageable)
                .map(doctorMapper::toResponse);
    }

    @Override
    public List<DoctorResponse> getPendingVerification() {
        return doctorRepository.findByVerificationStatus(VerificationStatus.PENDING)
                .stream().map(doctorMapper::toResponse).toList();
    }

    @Override
    public List<DoctorResponse> getDoctorsByClinic(Long clinicId) {
        return doctorRepository.findByClinicId(clinicId)
                .stream().map(doctorMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void deactivateDoctor(Long id) {
        Doctor doctor = findDoctorOrThrow(id);
        doctor.setActive(false);
        doctorRepository.save(doctor);
        auditLogService.log(doctor, "ACCOUNT_DEACTIVATED", null, null, true, null);
    }

    @Override
    @Transactional
    public void activateDoctor(Long id) {
        Doctor doctor = findDoctorOrThrow(id);
        doctor.setActive(true);
        doctorRepository.save(doctor);
        auditLogService.log(doctor, "ACCOUNT_ACTIVATED", null, null, true, null);
    }

    @Override
    public Page<DoctorResponse> getAllDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable).map(doctorMapper::toResponse);
    }

    private Doctor findDoctorOrThrow(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
    }

    private void sendDoctorStatusEmail(String to, String doctorName, VerificationStatus oldStatus, VerificationStatus newStatus, String reason) {
        if (newStatus == VerificationStatus.APPROVED) {
            emailService.sendDoctorApprovedEmail(to, doctorName);
            return;
        }
        if (oldStatus == VerificationStatus.PENDING && newStatus == VerificationStatus.REJECTED) {
            emailService.sendDoctorRejectedEmail(to, doctorName, reason == null ? "" : reason);
            return;
        }
        emailService.sendDoctorStatusChangeEmail(
                to,
                doctorName,
                oldStatus == null ? "" : oldStatus.name(),
                newStatus == null ? "" : newStatus.name(),
                reason
        );
    }

    private String snapshotProfile(Doctor doctor) {
        return "{"
                + "\"firstName\":\"" + safe(doctor.getFirstName()) + "\","
                + "\"lastName\":\"" + safe(doctor.getLastName()) + "\","
                + "\"phone\":\"" + safe(doctor.getPhone()) + "\","
                + "\"address\":\"" + safe(doctor.getAddress()) + "\","
                + "\"dateOfBirth\":\"" + String.valueOf(doctor.getDateOfBirth()) + "\","
                + "\"gender\":\"" + String.valueOf(doctor.getGender()) + "\","
                + "\"specialization\":\"" + String.valueOf(doctor.getSpecialization()) + "\","
                + "\"licenseNumber\":\"" + safe(doctor.getLicenseNumber()) + "\","
                + "\"consultationDuration\":\"" + String.valueOf(doctor.getConsultationDuration()) + "\","
                + "\"consultationFee\":\"" + String.valueOf(doctor.getConsultationFee()) + "\","
                + "\"officeAddress\":\"" + safe(doctor.getOfficeAddress()) + "\","
                + "\"emergencyContact\":\"" + safe(doctor.getEmergencyContact()) + "\","
                + "\"profilePicture\":\"" + safe(doctor.getProfilePicture()) + "\""
                + "}";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "\\\"");
    }
}
