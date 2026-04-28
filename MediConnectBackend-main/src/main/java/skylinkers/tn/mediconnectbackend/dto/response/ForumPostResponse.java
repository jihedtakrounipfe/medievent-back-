package skylinkers.tn.mediconnectbackend.dto.response;

import skylinkers.tn.mediconnectbackend.entities.ForumPost;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import java.time.LocalDateTime;

public class ForumPostResponse {
    private String id;
    private String title;
    private String content;
    private ForumPost.PostCategory category;
    private boolean isVerifiedByDoctor;
    private int viewCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private AuthorInfo author;

    public static class AuthorInfo {
        private Long id;
        private String firstName;
        private String fullName;
        private UserType userType;

        public AuthorInfo(Long id, String firstName, String fullName, UserType userType) {
            this.id = id;
            this.firstName = firstName;
            this.fullName = fullName;
            this.userType = userType;
        }

        public Long getId() { return id; }
        public String getFirstName() { return firstName; }
        public String getFullName() { return fullName; }
        public UserType getUserType() { return userType; }
    }

    public ForumPostResponse(String id, String title, String content, ForumPost.PostCategory category,
                            boolean isVerifiedByDoctor, int viewCount, int commentCount,
                            LocalDateTime createdAt, AuthorInfo author) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.isVerifiedByDoctor = isVerifiedByDoctor;
        this.viewCount = viewCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.author = author;
    }

    public static ForumPostResponse fromEntity(ForumPost post) {
        Long authorId = post.getAuthor() != null ? post.getAuthor().getId() : null;
        String firstName = post.getAuthor() != null ? post.getAuthor().getFirstName() : "";
        String lastName = post.getAuthor() != null ? post.getAuthor().getLastName() : "";
        String fullName = (firstName + " " + lastName).trim();
        UserType userType = post.getAuthor() != null ? post.getAuthor().getUserType() : null;

        AuthorInfo authorInfo = new AuthorInfo(
            authorId,
            firstName,
            fullName,
            userType
        );
        
        return new ForumPostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getCategory(),
            post.isVerifiedByDoctor(),
            post.getViewCount(),
            post.getCommentCount(),
            post.getCreatedAt(),
            authorInfo
        );
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public ForumPost.PostCategory getCategory() { return category; }
    public boolean isVerifiedByDoctor() { return isVerifiedByDoctor; }
    public int getViewCount() { return viewCount; }
    public int getCommentCount() { return commentCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public AuthorInfo getAuthor() { return author; }
}
