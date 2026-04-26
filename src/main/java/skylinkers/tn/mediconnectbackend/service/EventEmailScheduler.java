package skylinkers.tn.mediconnectbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.EventParticipant;
import skylinkers.tn.mediconnectbackend.entities.MedicalEvent;
import skylinkers.tn.mediconnectbackend.entities.enums.ParticipantStatus;
import skylinkers.tn.mediconnectbackend.repository.EventParticipantRepository;
import skylinkers.tn.mediconnectbackend.repository.MedicalEventRepository;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Runs every minute and dispatches two types of scheduled emails:
 *  1. 30-minute pre-event reminder to all confirmed participants
 *  2. "Event has started" notification (fires within a 2-min window around event start time)
 *
 * Both use tracking flags on MedicalEvent to avoid sending duplicate emails.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventEmailScheduler {

    private final MedicalEventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${mediconnect.app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    /**
     * Fires every 60 seconds.
     * Sends a reminder email 30 min before the event starts (window: 29–31 min from now).
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sendPreEventReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.plusMinutes(29);
        LocalDateTime windowEnd   = now.plusMinutes(31);

        List<MedicalEvent> events = eventRepository.findEventsNeedingReminder(windowStart, windowEnd);
        if (events.isEmpty()) return;

        log.info("[SCHEDULER] Found {} event(s) needing 30-min reminder", events.size());

        for (MedicalEvent event : events) {
            String dateStr = formatDate(event.getEventDate());
            String eventUrl = frontendBaseUrl + "/events/" + event.getId() + "/room";

            participantRepository.findByEvent(event).stream()
                    .filter(p -> p.getStatus() == ParticipantStatus.CONFIRMED)
                    .forEach(p -> {
                        String userName = p.getUser().getFirstName() + " " + p.getUser().getLastName();
                        emailService.sendEventReminderEmail(
                                p.getUser().getEmail(),
                                userName,
                                event.getTitle(),
                                dateStr,
                                eventUrl
                        );
                        log.info("[SCHEDULER] Reminder sent to {} for event '{}'",
                                p.getUser().getEmail(), event.getTitle());
                    });

            event.setReminderSent(true);
            eventRepository.save(event);
        }
    }

    /**
     * Fires every 60 seconds.
     * Sends a "the event has started" email within a ±2-min window around the event time.
     * Only triggers if the organizer hasn't manually sent it via completeEvent().
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void sendEventStartedNotifications() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusMinutes(2);
        LocalDateTime windowEnd   = now.plusMinutes(2);

        List<MedicalEvent> events = eventRepository.findEventsNeedingStartNotification(windowStart, windowEnd);
        if (events.isEmpty()) return;

        log.info("[SCHEDULER] Found {} event(s) to send start notification", events.size());

        for (MedicalEvent event : events) {
            String eventUrl = frontendBaseUrl + "/events/" + event.getId() + "/room";

            participantRepository.findByEvent(event).stream()
                    .filter(p -> p.getStatus() == ParticipantStatus.CONFIRMED)
                    .forEach(p -> {
                        String userName = p.getUser().getFirstName() + " " + p.getUser().getLastName();
                        emailService.sendEventStartedEmail(
                                p.getUser().getEmail(),
                                userName,
                                event.getTitle(),
                                eventUrl
                        );
                        log.info("[SCHEDULER] Start notification sent to {} for event '{}'",
                                p.getUser().getEmail(), event.getTitle());
                    });

            event.setStartedNotificationSent(true);
            eventRepository.save(event);
        }
    }

    private String formatDate(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "Date à confirmer";
    }
}
