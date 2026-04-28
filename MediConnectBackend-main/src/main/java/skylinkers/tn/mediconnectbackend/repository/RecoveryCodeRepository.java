package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.RecoveryCode;

import java.util.List;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, Long> {

    List<RecoveryCode> findByUserIdAndUsedFalse(Long userId);

    List<RecoveryCode> findByUserId(Long userId);

    long countByUserIdAndUsedFalse(Long userId);

    @Modifying
    @Query("DELETE FROM RecoveryCode rc WHERE rc.user.id = :userId")
    void deleteAllByUserId(Long userId);
}
