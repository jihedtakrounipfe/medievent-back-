package skylinkers.tn.mediconnectbackend.repository;

import skylinkers.tn.mediconnectbackend.entities.ForumComment;
import skylinkers.tn.mediconnectbackend.entities.ForumPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ForumCommentRepository extends JpaRepository<ForumComment, String> {

    // Existing
    List<ForumComment> findByPostOrderByCreatedAtAsc(ForumPost post);

    // NEW METHODS FOR 8 FEATURES

    // 1️⃣ Threading: Find all replies to a comment
    List<ForumComment> findByReplyToIdOrderByCreatedAtAsc(String replyToId);

    // 2️⃣ Find root comments (no parent)
    List<ForumComment> findByPostAndReplyToIdIsNullOrderByCreatedAtAsc(ForumPost post);

    // 3️⃣ Delete all comments by postId (for cleanup)
    @Modifying
    @Transactional
    @Query("DELETE FROM ForumComment c WHERE c.post.id = :postId")
    void deleteByPostId(@Param("postId") String postId);
}