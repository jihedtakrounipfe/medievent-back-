package skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import skylinkers.tn.mediconnectbackend.entities.StudentVerification;
import skylinkers.tn.mediconnectbackend.entities.enums.SubVerificationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentVerificationRepository extends JpaRepository<StudentVerification, Long> {
    Optional<StudentVerification> findByUserId(Long userId);
    Optional<StudentVerification> findTopByUserIdOrderByCreatedAtDesc(Long userId);
    List<StudentVerification> findByStatus(SubVerificationStatus status);
    List<StudentVerification> findByStatusAndExpiresAtBefore(SubVerificationStatus status, LocalDateTime now);
    boolean existsByUserIdAndStatus(Long userId, SubVerificationStatus status);
    long deleteByUserId(Long userId);
}