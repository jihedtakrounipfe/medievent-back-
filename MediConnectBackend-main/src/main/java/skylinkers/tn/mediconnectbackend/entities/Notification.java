package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String message;
    
    @ManyToOne
    @JoinColumn(name = "post_id")
    private ForumPost post;

    @ManyToOne
    @JoinColumn(name = "comment_id")
    private ForumComment comment;

    @Column(name = "is_read")
    private boolean read = false;
    
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private AppUser actor;

    public enum NotificationType {
        COMMENT, UPVOTE, DOCTOR_RESPONSE, PINNED, BADGE
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public ForumPost getPost() { return post; }
    public void setPost(ForumPost post) { this.post = post; }

    public ForumComment getComment() { return comment; }
    public void setComment(ForumComment comment) { this.comment = comment; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public AppUser getActor() { return actor; }
    public void setActor(AppUser actor) { this.actor = actor; }
}
