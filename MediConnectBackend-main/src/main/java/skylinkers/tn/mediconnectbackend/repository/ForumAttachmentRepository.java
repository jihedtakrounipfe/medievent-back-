package skylinkers.tn.mediconnectbackend.repository;

import skylinkers.tn.mediconnectbackend.entities.ForumAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface ForumAttachmentRepository extends JpaRepository<ForumAttachment, String> {
    List<ForumAttachment> findByPostId(String postId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ForumAttachment a WHERE a.post.id = :postId")
    void deleteByPostId(@Param("postId") String postId);
}