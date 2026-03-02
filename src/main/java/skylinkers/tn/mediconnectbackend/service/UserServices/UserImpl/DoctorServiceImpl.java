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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final AuditLogService  auditLogService;
    private final DoctorMapper     doctorMapper;

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
        doctorMapper.applyUpdate(request, doctor);
        Doctor saved = doctorRepository.save(doctor);
        auditLogService.log(saved, "PROFILE_UPDATE", null, null, true, null);
        return doctorMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DoctorResponse updateVerificationStatus(Long id, VerificationStatus status) {
        Doctor doctor = findDoctorOrThrow(id);
        boolean approved = status == VerificationStatus.APPROVED;
        doctorRepository.updateVerificationStatus(id, status, approved);
        doctor.setVerificationStatus(status);
        doctor.setVerified(approved);
        auditLogService.log(doctor, "DOCTOR_VERIFICATION_" + status.name(), null, null, true, null);
        log.info("Doctor verification updated: id={}, status={}", id, status);
        return doctorMapper.toResponse(doctor);
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

    private Doctor findDoctorOrThrow(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
    }
}
