package skylinkers.tn.mediconnectbackend.service.AppointmentServices;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.entities.Consultation;
import skylinkers.tn.mediconnectbackend.repository.AppointmentRepository.*;

import java.util.List;




@Service
@AllArgsConstructor
public class ConsultationServiceImpl implements IConsultationService{

    @Autowired
    private ConsultationRepository consultationRepository;



    @Override
    public Consultation ajouterConsultation(Consultation cons) {
        return consultationRepository.save(cons);
    }

    @Override
    public List<Consultation> AfficherConsultations() {
        return consultationRepository.findAll();
    }

    @Override
    public Consultation modifierConsultation(Consultation cons) {
        return consultationRepository.save(cons);
    }

    @Override
    public void supprimerConsultation(long idcons) {
        consultationRepository.deleteById(idcons);
    }
}
