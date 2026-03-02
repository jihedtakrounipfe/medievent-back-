package skylinkers.tn.mediconnectbackend.mapper;

import skylinkers.tn.mediconnectbackend.dto.request.CreateDoctorRequest;
import skylinkers.tn.mediconnectbackend.dto.request.UpdateProfileRequest;
import skylinkers.tn.mediconnectbackend.dto.response.DoctorResponse;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
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
        doctor.setRppsNumber(req.getRppsNumber());
        doctor.setSpecialization(req.getSpecialization());
        doctor.setLicenseNumber(req.getLicenseNumber());
        doctor.setOfficeAddress(req.getOfficeAddress());
        doctor.setConsultationFee(req.getConsultationFee());
        if (req.getConsultationDuration() != null) {
            doctor.setConsultationDuration(req.getConsultationDuration());
        }
        return doctor;
    }

    public DoctorResponse toResponse(Doctor d) {
        return DoctorResponse.builder()
                .id(d.getId())
                .email(d.getEmail())
                .firstName(d.getFirstName())
                .lastName(d.getLastName())
                .phone(d.getPhone())
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
                .isVerified(d.isVerified())
                .isActive(d.isActive())
                .profilePicture(d.getProfilePicture())
                .createdAt(d.getCreatedAt())
                .build();
    }

    public void applyUpdate(UpdateProfileRequest req, Doctor doctor) {
        if (req.getFirstName()     != null) doctor.setFirstName(req.getFirstName());
        if (req.getLastName()      != null) doctor.setLastName(req.getLastName());
        if (req.getPhone()         != null) doctor.setPhone(req.getPhone());
        if (req.getProfilePicture()!= null) doctor.setProfilePicture(req.getProfilePicture());
    }
}
