package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.PostReaction;
import skylinkers.tn.mediconnectbackend.entities.PostReactionId;

import java.util.List;
import java.util.Optional;

public interface PostReactionRepository extends JpaRepository<PostReaction, PostReactionId> {

    @Query("SELECT r FROM PostReaction r WHERE r.post.id = :postId AND r.user.id = :userId")
    Optional<PostReaction> findByPostIdAndUserId(@Param("postId") String postId, @Param("userId") Long userId);

    @Query("SELECT r.emoji, COUNT(r) FROM PostReaction r WHERE r.post.id = :postId GROUP BY r.emoji")
    List<Object[]> countByEmojiForPost(@Param("postId") String postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM PostReaction r WHERE r.post.id = :postId")
    void deleteByPostId(@Param("postId") String postId);
}
