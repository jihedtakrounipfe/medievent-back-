package skylinkers.tn.mediconnectbackend.service;

import skylinkers.tn.mediconnectbackend.dto.MedicalEventDTO;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;

import java.util.List;
import skylinkers.tn.mediconnectbackend.dto.ParticipantDTO;

public interface MedicalEventService {
    MedicalEventDTO createEvent(MedicalEventDTO dto, String doctorEmail);
    MedicalEventDTO updateEvent(Long id, MedicalEventDTO dto, String doctorEmail);
    void deleteEvent(Long id, String requesterEmail);
    MedicalEventDTO addSpeaker(Long eventId, Long doctorId, String organizerEmail);
    void removeSpeaker(Long eventId, Long doctorId, String organizerEmail);
    MedicalEventDTO getEventById(Long id);
    
    List<MedicalEventDTO> getMyEvents(String doctorEmail);
    List<MedicalEventDTO> getEventsByStatus(EventStatus status);
    List<MedicalEventDTO> getAllEventsForAdmin();
    
    MedicalEventDTO approveEvent(Long id);
    MedicalEventDTO rejectEvent(Long id, String reason);
    
    // Participation
    ParticipantDTO joinEvent(Long eventId, String userEmail);
    ParticipantDTO inviteUser(Long eventId, Long userIdToInvite, String doctorEmail);
    ParticipantDTO respondToInvite(Long eventId, String userEmail, boolean accepted);
    List<ParticipantDTO> getEventParticipants(Long eventId);
    List<MedicalEventDTO> getMyInvitations(String userEmail);
    List<skylinkers.tn.mediconnectbackend.dto.MyParticipationDTO> getMyParticipations(String userEmail);
    void cancelParticipation(Long eventId, String userEmail);
}
