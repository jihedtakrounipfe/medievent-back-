package skylinkers.tn.mediconnectbackend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TwoFactorStatusResponse {
    private boolean enabled;
    private List<TwoFactorMethodDTO> methods;
}
