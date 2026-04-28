package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.entities.FollowUp;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.ConsultationRepository;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.FollowUpRepository;

import java.util.List;



@Service
@AllArgsConstructor
public class FollowUpServiceImpl implements IFollowUpService{

    @Autowired
    private FollowUpRepository followUpRepository;



    @Override
    public FollowUp ajouterFollowUp(FollowUp fu) {
        return followUpRepository.save(fu);
    }

    @Override
    public List<FollowUp> AfficherFollowUps() {
        return followUpRepository.findAll();
    }

    @Override
    public FollowUp modifierFollowUp(FollowUp followUp) {
        return followUpRepository.save(followUp);
    }

    @Override
    public void supprimerFollowUp(long idfollowup) {
        followUpRepository.deleteById(idfollowup);
    }
}
