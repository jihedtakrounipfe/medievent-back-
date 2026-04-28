package skylinkers.tn.mediconnectbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoogleUserInfo {
    private String email;
    private String firstName;
    private String lastName;
    private String pictureUrl;
    private String googleId;
}
