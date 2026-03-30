package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DoctorStatusReasonRequest {
    @NotBlank
    private String reason;
}

