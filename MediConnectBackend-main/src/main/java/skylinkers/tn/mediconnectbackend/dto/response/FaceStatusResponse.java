package skylinkers.tn.mediconnectbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FaceStatusResponse {
    private boolean faceEnabled;
    private boolean faceEnrolled;
}
