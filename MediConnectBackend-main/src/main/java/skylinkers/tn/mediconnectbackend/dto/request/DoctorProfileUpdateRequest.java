package skylinkers.tn.mediconnectbackend.dto.request;

import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DoctorProfileUpdateRequest {
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String phone;

    @Size(max = 500)
    private String address;

    @Size(max = 500)
    private String profilePicture;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 20)
    private String emergencyContactPhone;

    private Specialization specialization;

    @Size(max = 50)
    private String licenseNumber;

    private Integer consultationDuration;

    private BigDecimal consultationFee;

    @Size(max = 500)
    private String officeAddress;
}
