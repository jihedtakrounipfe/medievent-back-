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
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class MedicalEventServiceImpl implements MedicalEventService {
    private final MedicalEventRepository eventRepository;
    private final DoctorRepository doctorRepository;
    private final AppUserRepository appUserRepository;
    private final EventParticipantRepository participantRepository;
    private final EmailService emailService;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = KafkaConfig.EVENT_PARTICIPATION_TOPIC;

    @org.springframework.beans.factory.annotation.Value("${mediconnect.app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;
    @Override
    @Transactional
    public MedicalEventDTO createEvent(MedicalEventDTO dto, String doctorEmail) {
        Doctor doctor = doctorRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        // Overlap Check (± 1 hour)
        java.time.LocalDateTime startWindow = dto.getEventDate().minusHours(1);
        java.time.LocalDateTime endWindow = dto.getEventDate().plusHours(1);
        long existing = eventRepository.countByOrganizerAndEventDateBetween(doctor, startWindow, endWindow);
        if (existing > 0) {
            throw new IllegalArgumentException("Vous avez déjà une conférence prévue à ce créneau horaire (intervalle de 1h)");
        }

        MedicalEvent event = MedicalEvent.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .eventDate(dto.getEventDate())
                .location(dto.getLocation())
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
        event.setTitle(dto.getTitle());
        event.setDescription(dto.getDescription());
        event.setEventDate(dto.getEventDate());
        event.setLocation(dto.getLocation());
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
        event.setStatus(EventStatus.APPROVED); // Auto-approved on update
        return mapToDTO(eventRepository.save(event));
    }
    @Override
    @Transactional
    public void deleteEvent(Long id, String requesterEmail) {
        MedicalEvent event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        // Only organizer or admin can delete an event
        if (!event.getOrganizer().getEmail().equalsIgnoreCase(requesterEmail) && !"admin".equals(requesterEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized: You are not the organizer");
        }
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
        String eventDateStr = event.getEventDate() != null ? event.getEventDate().toString() : "À venir";
        
        if (targetStatus == ParticipantStatus.CONFIRMED) {
            emailService.sendParticipationConfirmedEmail(
                    user.getEmail(), 
                    userName, 
                    event.getTitle(), 
                    eventDateStr, 
                    event.getLocation()
            );
        } else if (targetStatus == ParticipantStatus.WAITING_LIST) {
            emailService.sendParticipationWaitlistEmail(
                    user.getEmail(), 
                    userName, 
                    event.getTitle(), 
                    eventDateStr
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
            // Only confirmed users leaving trigger an opening in the main list
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
        return mapToParticipantDTO(participantRepository.save(participant));
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
}
