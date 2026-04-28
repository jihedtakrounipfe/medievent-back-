package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import java.time.LocalDateTime;

@Entity
@Table(name = "forum_post_reactions")
public class PostReaction {

    @EmbeddedId
    private PostReactionId id = new PostReactionId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("postId")
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @Column(nullable = false, length = 16)
    private String emoji;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public PostReactionId getId() { return id; }
    public void setId(PostReactionId id) { this.id = id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public ForumPost getPost() { return post; }
    public void setPost(ForumPost post) { this.post = post; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
