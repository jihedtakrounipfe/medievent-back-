package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    /**
     * Short-lived UUID token issued by POST /api/auth/password-change/verify-code.
     * Proves that the user completed email verification before changing the password.
     * Required in production; ignored in DEV mode (when JWT is null).
     */
    @NotBlank(message = "Le token de vérification est requis")
    private String verificationToken;

    @NotBlank(message = "Le mot de passe actuel est requis")
    private String currentPassword;

    @NotBlank(message = "Le nouveau mot de passe est requis")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
            message = "Le mot de passe doit contenir au moins une majuscule, une minuscule et un chiffre"
    )
    private String newPassword;

    @NotBlank(message = "La confirmation est requise")
    private String confirmPassword;
}
