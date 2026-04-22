package skylinkers.tn.mediconnectbackend.dto;

import lombok.Builder;
import lombok.Data;
import skylinkers.tn.mediconnectbackend.entities.enums.ParticipantRole;
import skylinkers.tn.mediconnectbackend.entities.enums.ParticipantStatus;

@Data
@Builder
public class ParticipantDTO {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private ParticipantRole role;
    private ParticipantStatus status;
}
