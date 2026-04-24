package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import skylinkers.tn.mediconnectbackend.entities.enums.EventAudience;
import skylinkers.tn.mediconnectbackend.entities.enums.EventStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Entity
@Table(name = "medical_events")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventAudience targetAudience;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true, length = 50)
    private skylinkers.tn.mediconnectbackend.entities.enums.Specialization specialization;

    @Column(length = 255)
    private String speakerName;

    @Column(columnDefinition = "TEXT")
    private String speakerBio;

    @Column(columnDefinition = "TEXT")
    private String agenda;

    @Column(length = 1000)
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @lombok.Builder.Default
    private EventStatus status = EventStatus.APPROVED;

    @Column(name = "max_participants")
    @lombok.Builder.Default
    private Integer maxParticipants = 50; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Doctor organizer;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventParticipant> participants = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "event_speakers",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "doctor_id")
    )
    @Builder.Default
    private List<Doctor> speakers = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "event_moderators",
        joinColumns = @JoinColumn(name = "event_id"),
        inverseJoinColumns = @JoinColumn(name = "doctor_id")
    )
    @Builder.Default
    private List<Doctor> moderators = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
