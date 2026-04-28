package skylinkers.tn.mediconnectbackend.utils;


import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import skylinkers.tn.mediconnectbackend.entities.Appointment;

import java.time.LocalTime;

@Getter
public class AppointmentRescheduledEvent extends ApplicationEvent {

    private final Appointment appointment;
    private final LocalTime ancienneHeure;
    private final LocalTime nouvelleHeure;

    public AppointmentRescheduledEvent(
            Object source,
            Appointment appointment,
            LocalTime ancienneHeure,
            LocalTime nouvelleHeure
    ) {
        super(source);
        this.appointment = appointment;
        this.ancienneHeure = ancienneHeure;
        this.nouvelleHeure = nouvelleHeure;
    }
}
