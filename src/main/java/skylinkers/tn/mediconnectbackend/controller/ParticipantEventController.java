package skylinkers.tn.mediconnectbackend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.dto.GuestInviteRequest;
import skylinkers.tn.mediconnectbackend.dto.MedicalEventDTO;
import skylinkers.tn.mediconnectbackend.dto.ParticipantDTO;
import skylinkers.tn.mediconnectbackend.entities.MedicalEvent;
import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.repository.MedicalEventRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.DoctorRepository;
import skylinkers.tn.mediconnectbackend.service.MedicalEventService;
import skylinkers.tn.mediconnectbackend.utils.EmailService;
import skylinkers.tn.mediconnectbackend.exception.ResourceNotFoundException;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events/participation")
@RequiredArgsConstructor
public class ParticipantEventController {

    private final MedicalEventService eventService;
    private final EmailService emailService;
    private final MedicalEventRepository eventRepository;
    private final DoctorRepository doctorRepository;

    @Value("${mediconnect.app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @PostMapping("/{eventId}/join")
    public ResponseEntity<ParticipantDTO> joinEvent(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.joinEvent(eventId, jwt.getClaimAsString("email")));
    }

    @PostMapping("/{eventId}/invite/{userId}")
    public ResponseEntity<ParticipantDTO> inviteUser(@PathVariable Long eventId, @PathVariable Long userId, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.inviteUser(eventId, userId, jwt.getClaimAsString("email")));
    }

    /**
     * Invite an external guest by email — no need for them to have an account.
     * Sends them a direct join link for the live session.
     */
    @PostMapping("/{eventId}/invite-email")
    public ResponseEntity<Map<String, String>> inviteGuestByEmail(
            @PathVariable Long eventId,
            @RequestBody GuestInviteRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        MedicalEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        String doctorEmail = jwt.getClaimAsString("email");
        Doctor doctor = doctorRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found"));

        String doctorName = "Dr. " + doctor.getFirstName() + " " + doctor.getLastName();
        String eventTitle = event.getTitle();
        String eventDate  = event.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm"));
        String joinUrl    = frontendBaseUrl + "/events/" + eventId + "/room";

        String guestName = (request.getGuestName() != null && !request.getGuestName().isBlank())
                ? request.getGuestName() : "Cher invité";

        emailService.sendGuestInvitationEmail(
                request.getGuestEmail(),
                guestName,
                doctorName,
                eventTitle,
                eventDate,
                joinUrl
        );

        return ResponseEntity.ok(Map.of("message", "Invitation envoyée à " + request.getGuestEmail()));
    }

    @PostMapping("/{eventId}/respond")
    public ResponseEntity<ParticipantDTO> respondToInvite(@PathVariable Long eventId, @RequestParam boolean accept, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.respondToInvite(eventId, jwt.getClaimAsString("email"), accept));
    }

    @GetMapping("/{eventId}/participants")
    public ResponseEntity<List<ParticipantDTO>> getEventParticipants(@PathVariable Long eventId) {
        return ResponseEntity.ok(eventService.getEventParticipants(eventId));
    }

    @GetMapping("/invitations/my")
    public ResponseEntity<List<MedicalEventDTO>> getMyInvitations(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.getMyInvitations(jwt.getClaimAsString("email")));
    }

    @GetMapping("/participations/my")
    public ResponseEntity<List<skylinkers.tn.mediconnectbackend.dto.MyParticipationDTO>> getMyParticipations(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.getMyParticipations(jwt.getClaimAsString("email")));
    }

    @GetMapping("/recommended")
    public ResponseEntity<List<MedicalEventDTO>> getRecommendedEvents(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(eventService.getRecommendedEvents(jwt.getClaimAsString("email")));
    }

    @DeleteMapping("/{eventId}/cancel")
    public ResponseEntity<Void> cancelParticipation(@PathVariable Long eventId, @AuthenticationPrincipal Jwt jwt) {
        eventService.cancelParticipation(eventId, jwt.getClaimAsString("email"));
        return ResponseEntity.noContent().build();
    }
}

