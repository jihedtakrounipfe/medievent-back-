package skylinkers.tn.mediconnectbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;       // always "Bearer"
    private Long   expiresIn;       // seconds
    private Object user;            // PatientResponse | DoctorResponse — polymorphic
    private String message;         // used for registration responses
    private Boolean requires2FA;    // true → frontend must send TOTP next
}