// src/main/java/skylinkers/tn/mediconnectbackend/dto/request/LoginRequest.java
package skylinkers.tn.mediconnectbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}