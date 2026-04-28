package skylinkers.tn.mediconnectbackend.entities;


import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idConsultation;

    private String clinicalNotes;

    @Column( nullable = true)
    private Date followUpDate;

    private Date dateCons;

    private String description;

    @OneToMany(mappedBy = "consultation", cascade = CascadeType.ALL)
    private List<FollowUp> followUps;


    // @ManyToOne
//    private MedicalRecord medicalRecord;



}
