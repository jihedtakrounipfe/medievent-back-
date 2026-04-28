package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import skylinkers.tn.mediconnectbackend.entities.Appointment;
import skylinkers.tn.mediconnectbackend.entities.Consultation;

import java.util.List;

public interface IConsultationService {
    Consultation ajouterConsultation(Consultation cons);
    List<Consultation> AfficherConsultations();
    Consultation modifierConsultation(Consultation cons);
    void supprimerConsultation(long idcons);
}
