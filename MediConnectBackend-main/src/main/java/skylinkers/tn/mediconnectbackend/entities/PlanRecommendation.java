package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "plan_recommendations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(columnDefinition = "LONGTEXT")
    private String userResponses; // JSON string of responses

    @Column(nullable = false)
    private String recommendedPlan; // BASIC, PLUS, PREMIUM, STUDENT

    @Column(columnDefinition = "TEXT")
    private String reasoning; // Why this plan was recommended

    @Column
    private Double confidenceScore; // 0.0 - 1.0

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}