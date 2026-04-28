package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLinkConfirmRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String currentPassword;
    @NotBlank
    private String googleId;
}
