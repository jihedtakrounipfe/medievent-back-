package skylinkers.tn.mediconnectbackend.repository;

import skylinkers.tn.mediconnectbackend.entities.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ForumPostRepository extends JpaRepository<ForumPost, String> {

    // ✅ Récupérer les posts par catégorie
    Page<ForumPost> findByCategory(ForumPost.PostCategory category, Pageable pageable);

    // ✅ Récupérer les posts épinglés
    List<ForumPost> findByPinnedTrue();

    // ✅ Recherche par titre ou contenu
    Page<ForumPost> findByTitleContainingIgnoreCaseOrContentContainingIgnoreCase(String title, String content, Pageable pageable);

    // ✅ Optionnel: recherche par tag
    @Query("SELECT DISTINCT p FROM ForumPost p JOIN p.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :tag, '%'))")
    Page<ForumPost> findByTagContainingIgnoreCase(@Param("tag") String tag, Pageable pageable);
}