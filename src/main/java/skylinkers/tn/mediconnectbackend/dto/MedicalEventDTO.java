package skylinkers.tn.mediconnectbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import skylinkers.tn.mediconnectbackend.entities.enums.EventAudience;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalEventDTO {
    private Long id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String location;
    private EventAudience targetAudience;
    private skylinkers.tn.mediconnectbackend.entities.enums.Specialization specialization;
    private String speakerName;
    private String speakerBio;
    private String agenda;
    private String bannerUrl;
    private EventStatus status;
    private Long organizerId;
    private String organizerName;
    private String organizerEmail;
    private String rejectionReason;
    private Integer maxParticipants;
    private Long confirmedCount;
    private Long waitingListCount;
}
