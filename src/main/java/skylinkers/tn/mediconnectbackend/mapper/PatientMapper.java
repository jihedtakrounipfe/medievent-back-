package skylinkers.tn.mediconnectbackend.mapper;

import skylinkers.tn.mediconnectbackend.dto.request.CreatePatientRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.PatientResponse;
import skylinkers.tn.mediconnectbackend.entities.Patient;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
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
        patient.setBloodType(req.getBloodType());
        patient.setAddress(req.getAddress());
        patient.setAllergies(req.getAllergies());
        patient.setEmergencyContact(composeEmergencyContact(req.getEmergencyContactName(), req.getEmergencyContactPhone()));
        patient.setProfilePicture(req.getProfilePicture());
        return patient;
    }

    /** Maps to a safe response — socialSecurityNum is intentionally excluded. */
    public PatientResponse toResponse(Patient p) {
        EmergencyContactParts ec = parseEmergencyContact(p.getEmergencyContact());
        return PatientResponse.builder()
                .id(p.getId())
                .userType(UserType.PATIENT)
                .email(p.getEmail())
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .phone(p.getPhone())
                .dateOfBirth(p.getDateOfBirth())
                .gender(p.getGender())
                .bloodType(p.getBloodType())
                .allergies(p.getAllergies())
                .address(p.getAddress())
                .emergencyContactName(ec.name())
                .emergencyContactPhone(ec.phone())
                .profilePicture(p.getProfilePicture())
                .biometricEnrolled(p.isBiometricEnrolled())
                .googleCalendarLinked(p.isGoogleCalendarLinked())
                .noShowScore(p.getNoShowScore())
                .isActive(p.isActive())
                .isVerified(p.getIsVerified())
                .createdAt(p.getCreatedAt())
                .build();
    }

    /** Applies only non-null fields from the update request. */
    public void applyUpdate(UpdateProfileRequest req, Patient patient) {
        if (req.getFirstName()       != null) patient.setFirstName(req.getFirstName());
        if (req.getLastName()        != null) patient.setLastName(req.getLastName());
        if (req.getPhone()           != null) patient.setPhone(req.getPhone());
        if (req.getAddress()         != null) patient.setAddress(req.getAddress());
        if (req.getDateOfBirth()     != null) patient.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender()          != null) patient.setGender(req.getGender());
        if (req.getBloodType()       != null) patient.setBloodType(req.getBloodType());
        if (req.getAllergies()       != null) patient.setAllergies(req.getAllergies());
        if (req.getEmergencyContact() != null) {
            patient.setEmergencyContact(req.getEmergencyContact());
        } else if (req.getEmergencyContactName() != null || req.getEmergencyContactPhone() != null) {
            EmergencyContactParts current = parseEmergencyContact(patient.getEmergencyContact());
            String name = req.getEmergencyContactName() == null ? current.name() : req.getEmergencyContactName();
            String phone = req.getEmergencyContactPhone() == null ? current.phone() : req.getEmergencyContactPhone();
            patient.setEmergencyContact(composeEmergencyContact(name, phone));
        }
        if (req.getProfilePicture()  != null) patient.setProfilePicture(req.getProfilePicture());
    }

    private record EmergencyContactParts(String name, String phone) {}

    private EmergencyContactParts parseEmergencyContact(String raw) {
        if (raw == null) return new EmergencyContactParts(null, null);
        String v = raw.trim();
        if (v.isBlank()) return new EmergencyContactParts(null, null);
        String[] parts = v.split("\\|", 2);
        if (parts.length == 2) {
            String name = parts[0].trim();
            String phone = parts[1].trim();
            return new EmergencyContactParts(name.isBlank() ? null : name, phone.isBlank() ? null : phone);
        }
        return new EmergencyContactParts(v, null);
    }

    private String composeEmergencyContact(String name, String phone) {
        String n = name == null ? "" : name.trim();
        String p = phone == null ? "" : phone.trim();
        if (n.isBlank() && p.isBlank()) return null;
        return n + "|" + p;
    }
}
