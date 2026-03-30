package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import skylinkers.tn.mediconnectbackend.entities.BiometricData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Access to this repository MUST be restricted to BiometricService only.
 * No controller should call this directly — enforced via package-private
 * service access and Spring Security method security.
 */
@Repository
public interface BiometricDataRepository extends JpaRepository<BiometricData, Long> {

    Optional<BiometricData> findByUserIdAndIsActiveTrue(Long userId);

    boolean existsByUserIdAndIsActiveTrue(Long userId);

    @Modifying
    @Query("UPDATE BiometricData b SET b.isActive = false WHERE b.user.id = :userId")
    void deactivateByUserId(Long userId);
}
