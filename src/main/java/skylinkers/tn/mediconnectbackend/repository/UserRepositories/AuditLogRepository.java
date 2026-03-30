package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import skylinkers.tn.mediconnectbackend.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    List<AuditLog> findByUserIdAndActionAndTimestampBetween(
            Long userId, String action, LocalDateTime from, LocalDateTime to);

    /** Security: count failed auth attempts in sliding window. */
    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.user.id = :userId
          AND a.action = 'AUTH_LOGIN'
          AND a.success = false
          AND a.timestamp >= :since
        """)
    long countFailedLoginsSince(Long userId, LocalDateTime since);

    /**
     * RGPD right-to-erasure: anonymise logs (replace user reference with null,
     * replace IP with '0.0.0.0') instead of hard-deleting audit history.
     */
    @Modifying
    @Query("""
        UPDATE AuditLog a
        SET a.user = null,
            a.ipAddress = '0.0.0.0',
            a.userAgent = '[ERASED]'
        WHERE a.user.id = :userId
        """)
    void anonymiseByUserId(Long userId);

    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);
}
