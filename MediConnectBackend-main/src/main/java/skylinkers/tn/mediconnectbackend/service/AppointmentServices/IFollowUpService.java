package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import skylinkers.tn.mediconnectbackend.entities.Consultation;
import skylinkers.tn.mediconnectbackend.entities.FollowUp;

import java.util.List;

public interface IFollowUpService {
    FollowUp ajouterFollowUp(FollowUp fu);
    List<FollowUp> AfficherFollowUps();
    FollowUp modifierFollowUp(FollowUp followUp);
    void supprimerFollowUp(long idfollowup);



}
