package skylinkers.tn.mediconnectbackend.entities;


import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idLab;


    private String labName;
    private String address;
    private Number phone;

    @OneToMany(mappedBy = "laboratory")
    private List<Appointment> appointments ;



}







