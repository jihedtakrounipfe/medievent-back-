package skylinkers.tn.mediconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationEvent {
    private Long eventId;
    private Long userId;
    private String type; // e.g., "CANCELLED", "JOINED"
}
