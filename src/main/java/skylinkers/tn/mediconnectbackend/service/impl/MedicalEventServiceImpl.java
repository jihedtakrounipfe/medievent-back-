package skylinkers.tn.mediconnectbackend.service.impl;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import skylinkers.tn.mediconnectbackend.dto.MedicalEventDTO;
import skylinkers.tn.mediconnectbackend.entities.MedicalEvent;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;
import skylinkers.tn.mediconnectbackend.exception.ResourceNotFoundException;
import skylinkers.tn.mediconnectbackend.repository.MedicalEventRepository;
import skylinkers.tn.mediconnectbackend.repository.EventParticipantRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.DoctorRepository;
import skylinkers.tn.mediconnectbackend.service.MedicalEventService;
import skylinkers.tn.mediconnectbackend.utils.EmailService;
import skylinkers.tn.mediconnectbackend.config.KafkaConfig;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.EventParticipant;
import skylinkers.tn.mediconnectbackend.dto.ParticipantDTO;
import skylinkers.tn.mediconnectbackend.entities.enums.ParticipantRole;
import skylinkers.tn.mediconnectbackend.entities.enums.ParticipantStatus;
import skylinkers.tn.mediconnectbackend.dto.AppNotificationDTO;
import skylinkers.tn.mediconnectbackend.entities.AppNotification;
import skylinkers.tn.mediconnectbackend.entities.EventSubscription;
import skylinkers.tn.mediconnectbackend.repository.AppNotificationRepository;
import skylinkers.tn.mediconnectbackend.repository.EventSubscriptionRepository;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalEventServiceImpl implements MedicalEventService {
    private final MedicalEventRepository eventRepository;
    private final DoctorRepository doctorRepository;
    private final AppUserRepository appUserRepository;
    private final EventParticipantRepository participantRepository;
    private final EventSubscriptionRepository subscriptionRepository;
    private final AppNotificationRepository notificationRepository;
    private final EmailService emailService;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = KafkaConfig.EVENT_PARTICIPATION_TOPIC;

    @org.springframework.beans.factory.annotation.Value("${mediconnect.app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    private static final java.time.format.DateTimeFormatter DATE_FMT =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private String formatDate(java.time.LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "Date à confirmer";
    }
    @Override
    @Transactional
    public MedicalEventDTO createEvent(MedicalEventDTO dto, String doctorEmail) {
        log.info("[CREATE-EVENT] Doctor: {}, DTO: {}", doctorEmail, dto);
        Doctor doctor = doctorRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        // Overlap Check (± 1 hour)
        java.time.LocalDateTime startWindow = dto.getEventDate().minusHours(1);
        java.time.LocalDateTime endWindow = dto.getEventDate().plusHours(1);
        long existing = eventRepository.countByOrganizerAndEventDateBetween(doctor, startWindow, endWindow);
        if (existing > 0) {
            throw new IllegalArgumentException("Vous avez déjà une conférence prévue à ce créneau horaire (intervalle de 1h)");
        }

        // Truncate location if it exceeds 255 characters (limit of typical DB column)
        String location = dto.getLocation();
        if (location != null && location.length() > 255) {
            location = location.substring(0, 252) + "...";
        }

        MedicalEvent event = MedicalEvent.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .location(location)
                .targetAudience(dto.getTargetAudience())
                .specialization(dto.getSpecialization())
                .speakerName(dto.getSpeakerName())
                .speakerBio(dto.getSpeakerBio())
                .agenda(dto.getAgenda())
                .bannerUrl(dto.getBannerUrl())
                .status(EventStatus.APPROVED)
                .organizer(doctor)
                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 50)
                .speakers(dto.getSpeakers() != null ? dto.getSpeakers().stream()
                        .map(s -> doctorRepository.findById(s.getId()).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()) : new java.util.ArrayList<>())
                .moderators(dto.getModerators() != null ? dto.getModerators().stream()
                        .map(m -> doctorRepository.findById(m.getId()).orElse(null))
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList()) : new java.util.ArrayList<>())
                .build();
        MedicalEvent saved = eventRepository.save(event);
        
        // Notify co-presenters (speakers)
        if (saved.getSpeakers() != null) {
            String organizerName = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
            String dateStr = saved.getEventDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String joinUrl = frontendBaseUrl + "/events/" + saved.getId() + "/room";
            
            for (Doctor speaker : saved.getSpeakers()) {
                emailService.sendGuestInvitationEmail(
                    speaker.getEmail(),
                    "Dr. " + speaker.getFirstName() + " " + speaker.getLastName(),
                    organizerName,
                    saved.getTitle(),
                    dateStr,
                    joinUrl
                );
            }
        }
        
        return mapToDTO(saved);
    }
    @Override
    @Transactional
    public MedicalEventDTO updateEvent(Long id, MedicalEventDTO dto, String doctorEmail) {
        MedicalEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (!event.getOrganizer().getEmail().equalsIgnoreCase(doctorEmail)) {
            throw new RuntimeException("Unauthorized: You are not the organizer");
        }
        // Guard: a COMPLETED event is archived and cannot be modified
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new ResponseStatusException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "Cet événement est terminé et ne peut plus être modifié."
            );
        }
        // Truncate location if it exceeds 255 characters
        String newLoc = dto.getLocation();
        if (newLoc != null && newLoc.length() > 255) {
            newLoc = newLoc.substring(0, 252) + "...";
        }

        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setLocation(newLoc);
        event.setTargetAudience(dto.getTargetAudience());
        event.setSpecialization(dto.getSpecialization());
        event.setSpeakerName(dto.getSpeakerName());
        event.setSpeakerBio(dto.getSpeakerBio());
        event.setAgenda(dto.getAgenda());
        event.setBannerUrl(dto.getBannerUrl());
        if (dto.getMaxParticipants() != null) event.setMaxParticipants(dto.getMaxParticipants());
        if (dto.getSpeakers() != null) {
            event.setSpeakers(dto.getSpeakers().stream()
                    .map(s -> doctorRepository.findById(s.getId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        if (dto.getModerators() != null) {
            event.setModerators(dto.getModerators().stream()
                    .map(m -> doctorRepository.findById(m.getId()).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toList()));
        }
        // CRITICAL FIX: Never downgrade a COMPLETED event back to APPROVED.
        // Only set APPROVED if the event wasn't already completed.
        if (event.getStatus() != EventStatus.COMPLETED) {
            event.setStatus(EventStatus.APPROVED);
        }

        MedicalEvent saved = eventRepository.save(event);

        // MODIFICATION: Notify subscribers and participants of the update
        String updateMsg = "L'événement \"" + saved.getTitle() + "\" a été mis à jour.";
        
        log.info("[NOTIF-TRACE] Starting notification process for Event ID: {}", saved.getId());

        // 1. Notify the Organizer (Self-notification for testing)
        notificationRepository.save(AppNotification.builder()
            .user(saved.getOrganizer())
            .eventId(saved.getId())
            .title("Mise à jour (Vous)")
            .message("Vous avez mis à jour l'événement: " + saved.getTitle())
            .type("info")
            .createdAt(LocalDateTime.now())
            .read(false)
            .build());

        // 2. Notify subscribers
        List<EventSubscription> subs = subscriptionRepository.findByEvent(saved);
        log.info("[NOTIF-TRACE] Found {} subscribers for this event", subs.size());
        
        subs.forEach(s -> {
            log.info("[NOTIF-TRACE] Notifying subscriber: {}", s.getUser().getEmail());
            notificationRepository.save(AppNotification.builder()
                .user(s.getUser())
                .eventId(saved.getId())
                .title("Mise à jour d'événement")
                .message(updateMsg)
                .type("info")
                .createdAt(LocalDateTime.now())
                .read(false)
                .build());
            
            emailService.sendEventUpdatedEmail(
                s.getUser().getEmail(),
                s.getUser().getFirstName() + " " + s.getUser().getLastName(),
                saved.getTitle(),
                formatDate(saved.getEventDate()),
                frontendBaseUrl + "/events/" + saved.getId()
            );
        });

        // 3. Notify confirmed participants
        List<EventParticipant> participants = participantRepository.findByEvent(saved).stream()
            .filter(p -> p.getStatus() == ParticipantStatus.CONFIRMED)
            .collect(Collectors.toList());
        log.info("[NOTIF-TRACE] Found {} confirmed participants for this event", participants.size());

        participants.forEach(p -> {
                log.info("[NOTIF-TRACE] Notifying participant: {}", p.getUser().getEmail());
                notificationRepository.save(AppNotification.builder()
                    .user(p.getUser())
                    .eventId(saved.getId())
                    .title("Mise à jour d'événement")
                    .message(updateMsg)
                    .type("info")
                    .createdAt(LocalDateTime.now())
                    .read(false)
                    .build());

                emailService.sendEventUpdatedEmail(
                    p.getUser().getEmail(),
                    p.getUser().getFirstName() + " " + p.getUser().getLastName(),
                    saved.getTitle(),
                    formatDate(saved.getEventDate()),
                    frontendBaseUrl + "/events/" + saved.getId()
                );
            });

        return mapToDTO(saved);
    }
    @Override
    @Transactional
    public void deleteEvent(Long id, String requesterEmail) {
        MedicalEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        // Check if requester is organizer OR a real admin user in the database
        boolean isOrganizer = event.getOrganizer().getEmail().equalsIgnoreCase(requesterEmail);
        boolean isAdmin = appUserRepository.findByEmail(requesterEmail)
                .map(u -> "ADMIN".equalsIgnoreCase(u.getUserType() != null ? u.getUserType().name() : ""))
                .orElse(false);
        if (!isOrganizer && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized: You are not the organizer");
        }

        // Notify all enrolled participants BEFORE deleting
        String dateStr = formatDate(event.getEventDate());
        participantRepository.findByEvent(event).stream()
                .filter(p -> p.getStatus() == ParticipantStatus.CONFIRMED
                          || p.getStatus() == ParticipantStatus.WAITING_LIST)
                .forEach(p -> {
                    String userName = p.getUser().getFirstName() + " " + p.getUser().getLastName();
                    emailService.sendEventCancelledEmail(
                            p.getUser().getEmail(),
                            userName,
                            event.getTitle(),
                            dateStr
                    );
                });

        eventRepository.delete(event);
    }

    @Override
    @Transactional
    public MedicalEventDTO addSpeaker(Long eventId, Long doctorId, String organizerEmail) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        if (!event.getOrganizer().getEmail().equalsIgnoreCase(organizerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can add speakers");
        }
        
        // MODIFICATION: Block adding speakers to COMPLETED events
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'ajouter un intervenant à un événement terminé.");
        }
        
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
                
        if (!event.getSpeakers().contains(doctor)) {
            event.getSpeakers().add(doctor);
            // Notify co-presenter via email
            sendInvitationEmail(doctor, event);
        }
        
        return mapToDTO(eventRepository.save(event));
    }
    
    @Override
    @Transactional
    public void completeEvent(Long id, String organizerEmail, Integer participantCount) {
        MedicalEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        if (!event.getOrganizer().getEmail().equalsIgnoreCase(organizerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can complete the event");
        }

        event.setStatus(EventStatus.COMPLETED);
        event.setFinalParticipantCount(participantCount);
        eventRepository.save(event);

        // Mark the flag FIRST (before sending emails) to prevent double-sending on crash
        if (!event.isStartedNotificationSent()) {
            event.setStartedNotificationSent(true);
            eventRepository.save(event);

            String joinUrl = frontendBaseUrl + "/events/" + event.getId() + "/room";
            participantRepository.findByEvent(event).stream()
                    .filter(p -> p.getStatus() == ParticipantStatus.CONFIRMED)
                    .forEach(p -> {
                        String userName = p.getUser().getFirstName() + " " + p.getUser().getLastName();
                        emailService.sendEventStartedEmail(
                                p.getUser().getEmail(),
                                userName,
                                event.getTitle(),
                                joinUrl
                        );
                    });
        }
    }

    @Override
    @Transactional
    public void removeSpeaker(Long eventId, Long doctorId, String organizerEmail) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        if (!event.getOrganizer().getEmail().equalsIgnoreCase(organizerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the organizer can remove speakers");
        }
        
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
                
        event.getSpeakers().remove(doctor);
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalEventDTO getEventById(Long id) {
        MedicalEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        return mapToDTO(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicalEventDTO> getMyEvents(String doctorEmail) {
        Doctor doctor = doctorRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));
        return eventRepository.findAllByOrganizerId(doctor.getId())
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<MedicalEventDTO> getEventsByStatus(EventStatus status) {
        return eventRepository.findAllByStatus(status)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<MedicalEventDTO> getAllEventsForAdmin() {
        return eventRepository.findAll()
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }
    @Override
    @Transactional
    public MedicalEventDTO approveEvent(Long id) {
        // Keep for API compatibility but status is already APPROVED
        MedicalEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        event.setStatus(EventStatus.APPROVED);
        return mapToDTO(eventRepository.save(event));
    }
    @Override
    @Transactional
    public MedicalEventDTO rejectEvent(Long id, String reason) {
        MedicalEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        event.setStatus(EventStatus.REJECTED);
        event.setRejectionReason(reason);
        return mapToDTO(eventRepository.save(event));
    }
    @Override
    @Transactional
    public ParticipantDTO joinEvent(Long eventId, String userEmail) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        if (participantRepository.existsByEventAndUser(event, user)) {
            throw new IllegalArgumentException("User already participating");
        }
        long activeCount = participantRepository.countByEventAndStatusAndRole(event, ParticipantStatus.CONFIRMED, ParticipantRole.PARTICIPANT);
        ParticipantStatus targetStatus = (activeCount < event.getMaxParticipants()) 
                ? ParticipantStatus.CONFIRMED 
                : ParticipantStatus.WAITING_LIST;
        
        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(user)
                .role(ParticipantRole.PARTICIPANT)
                .status(targetStatus)
                .build();
                
        EventParticipant saved = participantRepository.save(participant);

        // Notify user via email
        String userName = user.getFirstName() + " " + user.getLastName();
        String eventDateStr = formatDate(event.getEventDate());
        String eventUrl = frontendBaseUrl + "/events/" + event.getId();

        if (targetStatus == ParticipantStatus.CONFIRMED) {
            emailService.sendParticipationConfirmedEmail(
                    user.getEmail(),
                    userName,
                    event.getTitle(),
                    eventDateStr,
                    event.getLocation(),
                    eventUrl
            );
        } else if (targetStatus == ParticipantStatus.WAITING_LIST) {
            emailService.sendParticipationWaitlistEmail(
                    user.getEmail(),
                    userName,
                    event.getTitle(),
                    eventDateStr,
                    eventUrl
            );
        }
        
        return mapToParticipantDTO(saved);
    }
    @Override
    @Transactional
    public void cancelParticipation(Long eventId, String userEmail) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        EventParticipant participant = participantRepository.findByEventAndUser(event, user)
                .orElseThrow(() -> new ResourceNotFoundException("Participation not found"));
        ParticipantStatus oldStatus = participant.getStatus();
        participantRepository.delete(participant);
        if (oldStatus == ParticipantStatus.CONFIRMED) {
            // Promote the first person on the waiting list to CONFIRMED
            participantRepository.findFirstByEventAndStatusOrderByCreatedAtAsc(event, ParticipantStatus.WAITING_LIST)
                    .ifPresent(next -> {
                        next.setStatus(ParticipantStatus.CONFIRMED);
                        participantRepository.save(next);
                        // Notify them by email
                        String userName = next.getUser().getFirstName() + " " + next.getUser().getLastName();
                        emailService.sendParticipationConfirmedEmail(
                                next.getUser().getEmail(),
                                userName,
                                event.getTitle(),
                                formatDate(event.getEventDate()),
                                event.getLocation(),
                                frontendBaseUrl + "/events/" + event.getId()
                        );
                    });
            kafkaTemplate.send(TOPIC, new skylinkers.tn.mediconnectbackend.dto.ParticipationEvent(eventId, user.getId(), "CANCELLED"));
        }
    }
    @Override
    @Transactional
    public ParticipantDTO inviteUser(Long eventId, Long userIdToInvite, String doctorEmail) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
                
        // Only organizer can invite (or adapt if you want any doctor to invite)
        if (!event.getOrganizer().getEmail().equalsIgnoreCase(doctorEmail)) {
            throw new IllegalArgumentException("Unauthorized: Only organizer can invite");
        }

        // MODIFICATION: Block inviting to COMPLETED events
        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Impossible d'inviter des participants à un événement terminé.");
        }
        
        AppUser userToInvite = appUserRepository.findById(userIdToInvite)
                .orElseThrow(() -> new ResourceNotFoundException("User to invite not found"));
                
        if (participantRepository.existsByEventAndUser(event, userToInvite)) {
            throw new IllegalArgumentException("User already participating or invited");
        }
        
        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(userToInvite)
                .role(ParticipantRole.GUEST)
                .status(ParticipantStatus.PENDING_INVITE)
                .build();
                
        ParticipantDTO saved = mapToParticipantDTO(participantRepository.save(participant));
        
        // Notify guest doctor via email
        sendInvitationEmail(userToInvite, event);
        
        return saved;
    }
    @Override
    @Transactional
    public ParticipantDTO respondToInvite(Long eventId, String userEmail, boolean accepted) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        EventParticipant participant = participantRepository.findByEventAndUser(event, user)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));
                
        if (participant.getStatus() != ParticipantStatus.PENDING_INVITE) {
            throw new IllegalArgumentException("Already responded or not invited");
        }
        
        participant.setStatus(accepted ? ParticipantStatus.CONFIRMED : ParticipantStatus.DECLINED);
        EventParticipant savedParticipant = participantRepository.save(participant);

        // MODIFICATION: Notify host (organizer) of guest response
        String responseStr = accepted ? "accepté" : "décliné";
        notificationRepository.save(AppNotification.builder()
            .user(event.getOrganizer())
            .eventId(event.getId())
            .title("Réponse à l'invitation")
            .message(user.getFirstName() + " " + user.getLastName() + " a " + responseStr + " votre invitation pour \"" + event.getTitle() + "\".")
            .type("info")
            .createdAt(LocalDateTime.now())
            .read(false)
            .build());

        return mapToParticipantDTO(savedParticipant);
    }
    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDTO> getEventParticipants(Long eventId) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
                
        return participantRepository.findByEvent(event).stream()
                .filter(p -> p.getRole() == ParticipantRole.PARTICIPANT && p.getStatus() == ParticipantStatus.CONFIRMED)
                .map(this::mapToParticipantDTO)
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<MedicalEventDTO> getMyInvitations(String userEmail) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        return participantRepository.findByUserAndStatus(user, ParticipantStatus.PENDING_INVITE)
                .stream()
                .map(p -> mapToDTO(p.getEvent()))
                .collect(Collectors.toList());
    }
    @Override
    @Transactional(readOnly = true)
    public List<skylinkers.tn.mediconnectbackend.dto.MyParticipationDTO> getMyParticipations(String userEmail) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        return participantRepository.findByUser(user)
                .stream()
                .filter(p -> p.getStatus() == ParticipantStatus.CONFIRMED || p.getStatus() == ParticipantStatus.WAITING_LIST)
                .map(p -> {
                    Long waitPos = null;
                    if (p.getStatus() == ParticipantStatus.WAITING_LIST) {
                        waitPos = participantRepository.findByEvent(p.getEvent()).stream()
                            .filter(other -> other.getStatus() == ParticipantStatus.WAITING_LIST)
                            .filter(other -> other.getCreatedAt().isBefore(p.getCreatedAt()))
                            .count() + 1;
                    }
                    return skylinkers.tn.mediconnectbackend.dto.MyParticipationDTO.builder()
                        .event(mapToDTO(p.getEvent()))
                        .status(p.getStatus())
                        .waitingListPosition(waitPos)
                        .build();
                })
                .collect(Collectors.toList());
    }
    private void sendInvitationEmail(AppUser recipient, MedicalEvent event) {
        String organizerName = "Dr. " + event.getOrganizer().getFirstName() + " " + event.getOrganizer().getLastName();
        String recipientName = "Dr. " + recipient.getFirstName() + " " + recipient.getLastName();
        String dateStr = event.getEventDate() != null ? 
            event.getEventDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "À venir";
        String joinUrl = frontendBaseUrl + "/events/" + event.getId(); 
        
        emailService.sendGuestInvitationEmail(
            recipient.getEmail(),
            recipientName,
            organizerName,
            event.getTitle(),
            dateStr,
            joinUrl
        );
    }

    private ParticipantDTO mapToParticipantDTO(EventParticipant participant) {
        return ParticipantDTO.builder()
                .id(participant.getId())
                .userId(participant.getUser().getId())
                .userName(participant.getUser().getFirstName() + " " + participant.getUser().getLastName())
                .userEmail(participant.getUser().getEmail())
                .role(participant.getRole())
                .status(participant.getStatus())
                .build();
    }
    private MedicalEventDTO mapToDTO(MedicalEvent event) {
        return MedicalEventDTO.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .eventDate(event.getEventDate())
                .location(event.getLocation())
                .targetAudience(event.getTargetAudience())
                .specialization(event.getSpecialization())
                .speakerName(event.getSpeakerName())
                .speakerBio(event.getSpeakerBio())
                .agenda(event.getAgenda())
                .bannerUrl(event.getBannerUrl())
                .status(event.getStatus())
                .organizerId(event.getOrganizer().getId())
                .organizerName(event.getOrganizer().getFirstName() + " " + event.getOrganizer().getLastName())
                .organizerEmail(event.getOrganizer().getEmail())
                .rejectionReason(event.getRejectionReason())
                .finalParticipantCount(event.getFinalParticipantCount())
                .maxParticipants(event.getMaxParticipants())
                .confirmedCount(participantRepository.countByEventAndStatusAndRole(event, ParticipantStatus.CONFIRMED, ParticipantRole.PARTICIPANT))
                .waitingListCount(participantRepository.countByEventAndStatusAndRole(event, ParticipantStatus.WAITING_LIST, ParticipantRole.PARTICIPANT))
                .speakers(event.getSpeakers() == null ? null : event.getSpeakers().stream()
                        .map(s -> MedicalEventDTO.SpeakerDTO.builder()
                                .id(s.getId())
                                .fullName(s.getFirstName() + " " + s.getLastName())
                                .email(s.getEmail())
                                .specialization(s.getSpecialization() != null ? s.getSpecialization().name() : null)
                                .profilePicture(s.getProfilePicture())
                                .build())
                        .collect(Collectors.toList()))
                .moderators(event.getModerators() == null ? null : event.getModerators().stream()
                        .map(m -> MedicalEventDTO.SpeakerDTO.builder()
                                .id(m.getId())
                                .fullName(m.getFirstName() + " " + m.getLastName())
                                .email(m.getEmail())
                                .specialization(m.getSpecialization() != null ? m.getSpecialization().name() : null)
                                .profilePicture(m.getProfilePicture())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public void subscribeToEvent(Long eventId, String userEmail) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!subscriptionRepository.existsByUserAndEvent(user, event)) {
            subscriptionRepository.save(EventSubscription.builder().user(user).event(event).build());
        }
    }

    @Override
    @Transactional
    public void unsubscribeFromEvent(Long eventId, String userEmail) {
        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        subscriptionRepository.findByUserAndEvent(user, event)
                .ifPresent(subscriptionRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSubscribed(Long eventId, String userEmail) {
        MedicalEvent event = eventRepository.findById(eventId).orElse(null);
        AppUser user = appUserRepository.findByEmail(userEmail).orElse(null);
        if (event == null || user == null) return false;
        return subscriptionRepository.existsByUserAndEvent(user, event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppNotificationDTO> getMyNotifications(String userEmail) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return notificationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::mapToNotificationDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Long notificationId, String userEmail) {
        AppNotification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notif.getUser().getEmail().equals(userEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        notif.setRead(true);
    }

    @Override
    @Transactional
    public void clearNotifications(String userEmail) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<AppNotification> list = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        notificationRepository.deleteAll(list);
    }

    @Override
    @Transactional
    public void notifySubscribers(Long eventId) {
        if (this.hasAlreadyNotified(eventId)) return;

        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        
        List<EventSubscription> subs = subscriptionRepository.findByEvent(event);
        for (EventSubscription sub : subs) {
            sendLiveNotification(sub.getUser(), event);
        }

        // MODIFICATION: Also notify guests (speakers and moderators)
        if (event.getSpeakers() != null) {
            for (Doctor speaker : event.getSpeakers()) {
                sendLiveNotification(speaker, event);
            }
        }
        if (event.getModerators() != null) {
            for (Doctor moderator : event.getModerators()) {
                sendLiveNotification(moderator, event);
            }
        }
    }

    private void sendLiveNotification(AppUser user, MedicalEvent event) {
        notificationRepository.save(AppNotification.builder()
                .user(user)
                .eventId(event.getId())
                .title("🔴 Direct commencé !")
                .message("\"" + event.getTitle() + "\" est en direct maintenant. Cliquez pour rejoindre.")
                .type("live_started")
                .createdAt(LocalDateTime.now())
                .read(false)
                .build());
    }

    @Override
    public boolean hasAlreadyNotified(Long eventId) {
        return notificationRepository.existsByEventIdAndType(eventId, "live_started");
    }

    private AppNotificationDTO mapToNotificationDTO(AppNotification n) {
        return AppNotificationDTO.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .eventId(n.getEventId())
                .type(n.getType())
                .timestamp(n.getCreatedAt())
                .isRead(n.isRead())
                .build();
    }
}
