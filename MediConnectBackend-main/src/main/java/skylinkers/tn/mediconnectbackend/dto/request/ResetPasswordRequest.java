package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String code;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit comporter au moins 8 caractères")
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
