package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleFaceVerifyRequest {

    @NotBlank @Email
    private String email;

    /** Base64-encoded face image. Null/blank triggers email OTP fallback. */
    private String faceImage;

    /** Set to true when user explicitly requests the email OTP fallback. */
    private Boolean useEmailFallback;
}
