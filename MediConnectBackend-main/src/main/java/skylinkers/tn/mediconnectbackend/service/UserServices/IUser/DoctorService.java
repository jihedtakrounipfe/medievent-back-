package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

import skylinkers.tn.mediconnectbackend.dto.request.CreateDoctorRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.DoctorResponse;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DoctorService {

    DoctorResponse createDoctor(CreateDoctorRequest request);

    DoctorResponse getDoctorById(Long id);

    DoctorResponse getDoctorByKeycloakId(String keycloakId);

    DoctorResponse updateProfile(Long id, UpdateProfileRequest request);

    /** Admin: approve or reject RPPS verification. */
    DoctorResponse updateVerificationStatus(Long id, VerificationStatus status);

    DoctorResponse setVerificationStatus(Long id, VerificationStatus status, String reason);

    DoctorResponse approveDoctor(Long id);

    DoctorResponse rejectDoctor(Long id, String reason);

    DoctorResponse suspendDoctor(Long id, String reason);

    List<DoctorResponse> getDoctorsForAdmin(VerificationStatus status);

    Page<DoctorResponse> getDoctorsBySpecialization(Specialization specialization, Pageable pageable);

    List<DoctorResponse> getPendingVerification();

    List<DoctorResponse> getDoctorsByClinic(Long clinicId);

    void deactivateDoctor(Long id);

    void activateDoctor(Long id);

    Page<DoctorResponse> getAllDoctors(Pageable pageable);
}

