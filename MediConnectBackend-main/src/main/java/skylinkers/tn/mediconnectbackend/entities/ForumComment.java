package skylinkers.tn.mediconnectbackend.entities;

import jakarta.persistence.*;
import skylinkers.tn.mediconnectbackend.entities.AppUser;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "forum_comments")
public class ForumComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;

    @ManyToOne
    @JoinColumn(name = "author_id", nullable = false)
    private AppUser author;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "upvotes", nullable = false)
    private int upvotes = 0;

    // ✅ AJOUTER CE CHAMP
    @Column(name = "downvotes", nullable = false)
    private int downvotes = 0;

    @Column(name = "is_answer", nullable = false)
    private boolean isAnswer = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // 1️⃣ Threading Support
    @Column(name = "reply_to_id")
    private String replyToId;

    // 2️⃣ & 3️⃣ Utility Marking
    @Column(name = "helpful_count", nullable = false)
    private int helpfulCount = 0;

    @Column(name = "solution_count", nullable = false)
    private int solutionCount = 0;

    @Column(name = "irrelevant_count", nullable = false)
    private int irrelevantCount = 0;

    // 4️⃣ Reputation
    @Column(name = "author_reputation", nullable = false)
    private int authorReputation = 0;

    // 5️⃣ Author OP Highlight
    @Transient
    private boolean isAuthorOp = false;

    // 6️⃣ Edit History
    @Column(name = "edit_history", columnDefinition = "TEXT")
    private String editHistory = "[]";

    // 7️⃣ Pinned
    @Transient
    private boolean isPinned = false;

    // 8️⃣ Computed Score
    @Transient
    private double computedScore = 0.0;

    @Transient
    private List<ForumComment> replies;

    // Getters and Setters

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public ForumPost getPost() { return post; }
    public void setPost(ForumPost post) { this.post = post; }

    public AppUser getAuthor() { return author; }
    public void setAuthor(AppUser author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getUpvotes() { return upvotes; }
    public void setUpvotes(int upvotes) { this.upvotes = upvotes; }

    // ✅ GETTER ET SETTER POUR downvotes
    public int getDownvotes() { return downvotes; }
    public void setDownvotes(int downvotes) { this.downvotes = downvotes; }

    public boolean isAnswer() { return isAnswer; }
    public void setAnswer(boolean answer) { isAnswer = answer; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getReplyToId() { return replyToId; }
    public void setReplyToId(String replyToId) { this.replyToId = replyToId; }

    public int getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(int helpfulCount) { this.helpfulCount = helpfulCount; }

    public int getSolutionCount() { return solutionCount; }
    public void setSolutionCount(int solutionCount) { this.solutionCount = solutionCount; }

    public int getIrrelevantCount() { return irrelevantCount; }
    public void setIrrelevantCount(int irrelevantCount) { this.irrelevantCount = irrelevantCount; }

    public int getAuthorReputation() { return authorReputation; }
    public void setAuthorReputation(int authorReputation) { this.authorReputation = authorReputation; }

    public boolean isAuthorOp() { return isAuthorOp; }
    public void setAuthorOp(boolean authorOp) { isAuthorOp = authorOp; }

    public String getEditHistory() { return editHistory; }
    public void setEditHistory(String editHistory) { this.editHistory = editHistory; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public double getComputedScore() { return computedScore; }
    public void setComputedScore(double computedScore) { this.computedScore = computedScore; }

    public List<ForumComment> getReplies() { return replies; }
    public void setReplies(List<ForumComment> replies) { this.replies = replies; }
}
