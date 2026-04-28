package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.UserSavedPost;
import skylinkers.tn.mediconnectbackend.entities.UserSavedPostId;

import java.util.List;

public interface UserSavedPostRepository extends JpaRepository<UserSavedPost, UserSavedPostId> {

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM UserSavedPost s WHERE s.post.id = :postId AND s.user.id = :userId")
    boolean existsByPostIdAndUserId(@Param("postId") String postId, @Param("userId") Long userId);

    @Query("SELECT s FROM UserSavedPost s WHERE s.user.id = :userId")
    List<UserSavedPost> findByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM UserSavedPost s WHERE s.post.id = :postId")
    void deleteByPostId(@Param("postId") String postId);
}
