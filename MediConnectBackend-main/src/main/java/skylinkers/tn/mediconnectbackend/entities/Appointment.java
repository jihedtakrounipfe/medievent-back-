package skylinkers.tn.mediconnectbackend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import skylinkers.tn.mediconnectbackend.entities.enums.*;
 import skylinkers.tn.mediconnectbackend.entities.enums.UrgencyLevel;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentStatus;
import skylinkers.tn.mediconnectbackend.entities.enums.AppointmentType;
import skylinkers.tn.mediconnectbackend.entities.enums.UrgencyLevel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAppointment;

    // Type de RDV : DOCTOR ou LAB
    @Enumerated(EnumType.STRING)
    private AppointmentType typeRdv;

    // Informations médecin
    private String specialite;
    private String medecin;

    // Nom du laboratoire (simple string pour éviter conflit)
    private String laboratoire;

    // Date et heure
    private LocalDate date;
    private LocalTime heure;

    // Motif du rendez-vous
    private String motif;

    // Status : PENDING / CONFIRMED / CANCELLED / DONE
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;

    // Google Calendar ID (optionnel)
    private String googleCalEventId;

    // Relation consultation
    @OneToOne
    private Consultation consultation;

    // Rappels
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Reminder> reminders;

    @ManyToOne
    private Laboratory laboratory;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = true)
    @Builder.Default
    private UrgencyLevel urgencyLevel = UrgencyLevel.NORMAL;

        @ManyToOne
    @JoinColumn(name = "patient_id")
    @JsonIgnoreProperties({
            "oauthToken",
            "biometricData",
            "auditLogs",
            "socialSecurityNum",
            "hibernateLazyInitializer",
            "handler"
    })
    private Patient patient;


    //normalement tetfasakh
    public void setUrgent(boolean value) {
        this.urgencyLevel = value ? UrgencyLevel.URGENT : UrgencyLevel.NORMAL;
    }


    @PrePersist
    public void prePersist() {
        if (this.urgencyLevel == null) {
            this.urgencyLevel = UrgencyLevel.NORMAL;
        }
    }



}