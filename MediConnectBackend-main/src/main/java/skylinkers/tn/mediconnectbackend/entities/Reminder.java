package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;

import lombok.*;
import skylinkers.tn.mediconnectbackend.entities.enums.ReminderChannel;
import skylinkers.tn.mediconnectbackend.entities.enums.ReminderStatus;

import java.util.Date;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReminder;

    @Enumerated(EnumType.STRING)
    private ReminderChannel channel;

    @Enumerated(EnumType.STRING)
    private ReminderStatus status;


    private Date scheduledAt;

    @ManyToOne
    private Appointment appointment;


}
