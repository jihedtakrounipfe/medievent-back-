package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

import skylinkers.tn.mediconnectbackend.dto.request.CreatePatientRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * SOLID — ISP: This interface exposes only patient-relevant operations.
 * No admin, doctor, or biometric methods bleed into this contract.
 *
 * SOLID — DIP: Controllers depend on this interface, not on PatientServiceImpl.
 */
public interface PatientService {

    PatientResponse createPatient(CreatePatientRequest request);

    PatientResponse getPatientById(Long id);

    PatientResponse getPatientByKeycloakId(String keycloakId);

    PatientResponse updateProfile(Long id, UpdateProfileRequest request);

    void deactivatePatient(Long id);

    void activatePatient(Long id);

    /** RGPD right-to-erasure: anonymises personal fields & audit logs. */
    void erasePatientData(Long id);

    /** RGPD right-to-portability: exports all patient data as a structured map. */
    Object exportPatientData(Long id);

    Page<PatientResponse> getAllPatients(Pageable pageable);

    void updateNoShowScore(Long id, Double score);
}

