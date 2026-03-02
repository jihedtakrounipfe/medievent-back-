package skylinkers.tn.mediconnectbackend.mapper;

import skylinkers.tn.mediconnectbackend.dto.request.CreatePatientRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.PatientResponse;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import org.springframework.stereotype.Component;

/**
 * Manual mapper — keeps the mapping logic explicit and testable without
 * pulling in MapStruct or ModelMapper, which can silently map sensitive
 * fields (e.g. socialSecurityNum) into response DTOs.
 *
 * SOLID — SRP: Only mapping logic lives here. No business rules.
 */
@Component
public class PatientMapper {

    public Patient toEntity(CreatePatientRequest req) {
        Patient patient = new Patient();
        patient.setEmail(req.getEmail());
        patient.setKeycloakId(req.getKeycloakId());
        patient.setFirstName(req.getFirstName());
        patient.setLastName(req.getLastName());
        patient.setPhone(req.getPhone());
        patient.setDateOfBirth(req.getDateOfBirth());
        patient.setGender(req.getGender());
        patient.setSocialSecurityNum(req.getSocialSecurityNum()); // AES-256 applied by converter
        patient.setBloodType(req.getBloodType());
        patient.setAddress(req.getAddress());
        patient.setEmergencyContact(req.getEmergencyContact());
        return patient;
    }

    /** Maps to a safe response — socialSecurityNum is intentionally excluded. */
    public PatientResponse toResponse(Patient p) {
        return PatientResponse.builder()
                .id(p.getId())
                .email(p.getEmail())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .phone(p.getPhone())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .bloodType(p.getBloodType())
                .allergies(p.getAllergies())
                .emergencyContact(p.getEmergencyContact())
                .address(p.getAddress())
                .profilePicture(p.getProfilePicture())
                .biometricEnrolled(p.isBiometricEnrolled())
                .googleCalendarLinked(p.isGoogleCalendarLinked())
                .noShowScore(p.getNoShowScore())
                .isActive(p.isActive())
                .createdAt(p.getCreatedAt())
                .build();
    }

    /** Applies only non-null fields from the update request. */
    public void applyUpdate(UpdateProfileRequest req, Patient patient) {
        if (req.getFirstName()       != null) patient.setFirstName(req.getFirstName());
        if (req.getLastName()        != null) patient.setLastName(req.getLastName());
        if (req.getPhone()           != null) patient.setPhone(req.getPhone());
        if (req.getAddress()         != null) patient.setAddress(req.getAddress());
        if (req.getEmergencyContact()!= null) patient.setEmergencyContact(req.getEmergencyContact());
        if (req.getProfilePicture()  != null) patient.setProfilePicture(req.getProfilePicture());
    }
}
