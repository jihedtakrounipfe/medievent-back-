package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.entities.Reminder;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.ConsultationRepository;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.ReminderRepository;

import java.util.List;



@Service
@AllArgsConstructor
public class ReminderServiceImpl implements IReminderService{

    @Autowired
    private ReminderRepository reminderRepository;


    @Override
    public Reminder ajouterReminder(Reminder reminder) {
        return reminderRepository.save(reminder);
    }

    @Override
    public List<Reminder> AfficherReminders() {
        return reminderRepository.findAll();
    }

    @Override
    public Reminder modifierReminder(Reminder reminder) {
        return reminderRepository.save(reminder);
    }

    @Override
    public void supprimerReminder(long idreminder) {
        reminderRepository.deleteById(idreminder);
    }
}
