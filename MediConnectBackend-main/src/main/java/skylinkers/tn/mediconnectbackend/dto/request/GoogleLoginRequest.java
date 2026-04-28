package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank(message = "Le token Google est requis")
    private String idToken;
}
