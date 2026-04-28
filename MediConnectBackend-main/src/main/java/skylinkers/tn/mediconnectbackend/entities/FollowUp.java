package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import skylinkers.tn.mediconnectbackend.entities.enums.SenderTypeFollowup;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFollowUp;

    private String message;

    private SenderTypeFollowup senderType;

    private Boolean isRead;
    private LocalDateTime sentAt;

    @ManyToOne(optional = false)
    private Consultation consultation;
}