package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import skylinkers.tn.mediconnectbackend.entities.FollowUp;
import skylinkers.tn.mediconnectbackend.entities.Reminder;

import java.util.List;

public interface IReminderService {

    Reminder ajouterReminder(Reminder reminder);
    List<Reminder> AfficherReminders();
    Reminder modifierReminder(Reminder reminder);
    void supprimerReminder(long idreminder);


}
