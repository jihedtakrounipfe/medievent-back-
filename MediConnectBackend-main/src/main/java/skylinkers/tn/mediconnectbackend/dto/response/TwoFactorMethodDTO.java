package skylinkers.tn.mediconnectbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorMethodDTO {
    private String type;        // "TOTP", "EMAIL", "SMS"
    private String label;       // French display label
    private boolean configured; // whether the user has this method set up
    private boolean available;  // whether this method is enabled in the system
}
