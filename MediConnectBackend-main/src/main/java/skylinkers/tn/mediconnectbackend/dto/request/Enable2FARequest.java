package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class Enable2FARequest {

    @NotBlank(message = "La méthode 2FA est requise")
    private String method; // "TOTP" | "EMAIL"
}
