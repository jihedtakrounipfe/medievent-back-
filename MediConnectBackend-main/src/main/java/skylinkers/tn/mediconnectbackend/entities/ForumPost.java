package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "forum_posts")
public class ForumPost {

    @Id
    private String id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    private PostCategory category;

    private int viewCount;

    private int commentCount;

    private int saveCount = 0;  // ✅ AJOUTÉ

    private boolean isVerifiedByDoctor;

    private LocalDateTime createdAt;

    private boolean pinned = false;

    @ElementCollection
    @CollectionTable(name = "forum_post_tags", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ForumAttachment> attachments = new ArrayList<>();

    public enum PostCategory {
        QUESTION, ADVICE, AWARENESS, DOCUMENT
    }

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public PostCategory getCategory() { return category; }
    public void setCategory(PostCategory category) { this.category = category; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getSaveCount() { return saveCount; }  // ✅ AJOUTÉ
    public void setSaveCount(int saveCount) { this.saveCount = saveCount; }  // ✅ AJOUTÉ

    public boolean isVerifiedByDoctor() { return isVerifiedByDoctor; }
    public void setVerifiedByDoctor(boolean verifiedByDoctor) { isVerifiedByDoctor = verifiedByDoctor; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public AppUser getAuthor() { return author; }
    public void setAuthor(AppUser author) { this.author = author; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<ForumAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<ForumAttachment> attachments) { this.attachments = attachments; }
}
