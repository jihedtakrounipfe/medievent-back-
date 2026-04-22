package skylinkers.tn.mediconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skylinkers.tn.mediconnectbackend.entities.enums.ParticipantStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyParticipationDTO {
    private MedicalEventDTO event;
    private ParticipantStatus status;
    private Long waitingListPosition;
}
