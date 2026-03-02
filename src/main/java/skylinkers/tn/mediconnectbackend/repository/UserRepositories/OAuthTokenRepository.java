package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import skylinkers.tn.mediconnectbackend.entities.OAuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {

    Optional<OAuthToken> findByUserIdAndProvider(Long userId, String provider);

    @Modifying
    @Query("DELETE FROM OAuthToken t WHERE t.user.id = :userId AND t.provider = :provider")
    void deleteByUserIdAndProvider(Long userId, String provider);

    boolean existsByUserIdAndProvider(Long userId, String provider);
}

