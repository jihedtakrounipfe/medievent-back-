package skylinkers.tn.mediconnectbackend.mapper;

import skylinkers.tn.mediconnectbackend.dto.request.CreateDoctorRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.DoctorResponse;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public Doctor toEntity(CreateDoctorRequest req) {
        Doctor doctor = new Doctor();
        doctor.setEmail(req.getEmail());
        doctor.setKeycloakId(req.getKeycloakId());
        doctor.setFirstName(req.getFirstName());
        doctor.setLastName(req.getLastName());
        doctor.setPhone(req.getPhone());
        doctor.setDateOfBirth(req.getDateOfBirth());
        doctor.setGender(req.getGender());
        doctor.setAddress(req.getAddress());
        doctor.setRppsNumber(req.getRppsNumber());
        doctor.setSpecialization(req.getSpecialization());
        doctor.setLicenseNumber(req.getLicenseNumber());
        doctor.setOfficeAddress(req.getOfficeAddress());
        doctor.setConsultationFee(req.getConsultationFee());
        doctor.setProfilePicture(req.getProfilePicture());
        doctor.setConsultationDuration(req.getConsultationDuration());
        doctor.setVerificationStatus(VerificationStatus.PENDING);
        doctor.setIsVerified(Boolean.FALSE);
        return doctor;
    }

    public DoctorResponse toResponse(Doctor d) {
        EmergencyContactParts ec = parseEmergencyContact(d.getEmergencyContact());
        return DoctorResponse.builder()
                .id(d.getId())
                .userType(UserType.DOCTOR)
                .email(d.getEmail())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .phone(d.getPhone())
                .dateOfBirth(d.getDateOfBirth())
                .gender(d.getGender())
                .address(d.getAddress())
                .emergencyContactName(ec.name())
                .emergencyContactPhone(ec.phone())
                .rppsNumber(d.getRppsNumber())
                .specialization(d.getSpecialization())
                .licenseNumber(d.getLicenseNumber())
                .consultationDuration(d.getConsultationDuration())
                .officeAddress(d.getOfficeAddress())
                .verificationStatus(d.getVerificationStatus())
                .googleCalendarLinked(d.isGoogleCalendarLinked())
                .clinicId(d.getClinicId())
                .consultationFee(d.getConsultationFee())
                .rating(d.getRating())
                .isVerified(d.getIsVerified())
                .isActive(d.isActive())
                .profilePicture(d.getProfilePicture())
                .createdAt(d.getCreatedAt())
                .interests(d.getInterests())
                .build();
    }

    public void applyUpdate(UpdateProfileRequest req, Doctor doctor) {
        if (req.getFirstName()     != null) doctor.setFirstName(req.getFirstName());
        if (req.getLastName()      != null) doctor.setLastName(req.getLastName());
        if (req.getPhone()         != null) doctor.setPhone(req.getPhone());
        if (req.getProfilePicture()!= null) doctor.setProfilePicture(req.getProfilePicture());
        if (req.getAddress()       != null) doctor.setAddress(req.getAddress());
        if (req.getDateOfBirth()   != null) doctor.setDateOfBirth(req.getDateOfBirth());
        if (req.getGender()        != null) doctor.setGender(req.getGender());

        if (req.getSpecialization()       != null) doctor.setSpecialization(req.getSpecialization());
        if (req.getLicenseNumber()        != null) doctor.setLicenseNumber(req.getLicenseNumber());
        if (req.getConsultationDuration() != null) doctor.setConsultationDuration(req.getConsultationDuration());
        if (req.getConsultationFee()      != null) doctor.setConsultationFee(req.getConsultationFee());
        if (req.getOfficeAddress()        != null) doctor.setOfficeAddress(req.getOfficeAddress());

        if (req.getEmergencyContact() != null) {
            doctor.setEmergencyContact(req.getEmergencyContact());
        } else if (req.getEmergencyContactName() != null || req.getEmergencyContactPhone() != null) {
            EmergencyContactParts current = parseEmergencyContact(doctor.getEmergencyContact());
            String name = req.getEmergencyContactName() == null ? current.name() : req.getEmergencyContactName();
            String phone = req.getEmergencyContactPhone() == null ? current.phone() : req.getEmergencyContactPhone();
            doctor.setEmergencyContact(composeEmergencyContact(name, phone));
        }
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
