package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_saved_posts")
public class UserSavedPost {

    @EmbeddedId
    private UserSavedPostId id = new UserSavedPostId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UserSavedPostId getId() { return id; }
    public void setId(UserSavedPostId id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public ForumPost getPost() { return post; }
    public void setPost(ForumPost post) { this.post = post; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
