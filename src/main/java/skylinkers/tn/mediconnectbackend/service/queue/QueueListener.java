package skylinkers.tn.mediconnectbackend.service.queue;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.config.KafkaConfig;
import skylinkers.tn.mediconnectbackend.dto.ParticipationEvent;
import skylinkers.tn.mediconnectbackend.entities.EventParticipant;
import skylinkers.tn.mediconnectbackend.entities.MedicalEvent;
import skylinkers.tn.mediconnectbackend.entities.enums.ParticipantStatus;
import skylinkers.tn.mediconnectbackend.repository.EventParticipantRepository;
import skylinkers.tn.mediconnectbackend.repository.MedicalEventRepository;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueListener {

    private final EventParticipantRepository participantRepository;
    private final MedicalEventRepository eventRepository;
    private final EmailService emailService;

    @KafkaListener(topics = KafkaConfig.EVENT_PARTICIPATION_TOPIC, groupId = "mediconnect-group")
    @Transactional
    public void processParticipationEvent(ParticipationEvent event) {
        log.info("Processing Kafka event: {}", event);
        if ("CANCELLED".equals(event.getType())) {
            promoteFromWaitingList(event.getEventId());
        }
    }

    private void promoteFromWaitingList(Long eventId) {
        MedicalEvent medicalEvent = eventRepository.findById(eventId).orElse(null);
        if (medicalEvent == null) return;
        
        long confirmedCount = participantRepository.countByEventAndStatus(medicalEvent, ParticipantStatus.CONFIRMED);
        
        if (confirmedCount < medicalEvent.getMaxParticipants()) {
            Optional<EventParticipant> nextInLine = participantRepository.findFirstByEventAndStatusOrderByCreatedAtAsc(
                    medicalEvent, ParticipantStatus.WAITING_LIST);
            
            nextInLine.ifPresent(participant -> {
                participant.setStatus(ParticipantStatus.CONFIRMED);
                participantRepository.save(participant);
                
                log.info("User {} promoted from waiting list for event {}", participant.getUser().getEmail(), eventId);
                
                // Notify user via premium email
                String userName = participant.getUser().getFirstName() + " " + participant.getUser().getLastName();
                String eventDateStr = medicalEvent.getEventDate() != null ? medicalEvent.getEventDate().toString() : "À venir";
                
                emailService.sendParticipationPromotedEmail(
                        participant.getUser().getEmail(),
                        userName,
                        medicalEvent.getTitle(),
                        eventDateStr,
                        medicalEvent.getLocation()
                );
            });
        }
    }
}
