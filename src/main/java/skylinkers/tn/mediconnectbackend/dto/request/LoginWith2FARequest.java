package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class LoginWith2FARequest extends LoginRequest {

    @NotBlank
    private String totpCode;
}