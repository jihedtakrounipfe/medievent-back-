package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import skylinkers.tn.mediconnectbackend.entities.AuditLog;
import skylinkers.tn.mediconnectbackend.dto.response.AuditActionStatResponse;
import skylinkers.tn.mediconnectbackend.dto.response.AuditCategoryStatResponse;
import skylinkers.tn.mediconnectbackend.dto.response.MostActiveAccountResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);

    Optional<AuditLog> findFirstByUserIdOrderByTimestampDesc(Long userId);

    List<AuditLog> findByUserEmailOrderByTimestampDesc(String email);

    List<AuditLog> findByUserIdAndActionAndTimestampBetween(
            Long userId, String action, LocalDateTime from, LocalDateTime to);

    List<AuditLog> findByCategoryOrderByTimestampDesc(String category);

    List<AuditLog> findByIpAddressOrderByTimestampDesc(String ipAddress);

    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime from, LocalDateTime to, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndSuccessTrue(Long userId);

    long countByUserIdAndSuccessFalse(Long userId);

    long countBySuccessTrue();

    long countBySuccessFalse();

    @Query("""
        SELECT new skylinkers.tn.mediconnectbackend.dto.response.AuditActionStatResponse(
            a.action,
            COUNT(a),
            SUM(CASE WHEN a.success = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN a.success = false THEN 1 ELSE 0 END)
        )
        FROM AuditLog a
        WHERE a.user.id = :userId
        GROUP BY a.action
        ORDER BY COUNT(a) DESC, a.action ASC
        """)
    List<AuditActionStatResponse> findTopActionStatsForUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
        SELECT new skylinkers.tn.mediconnectbackend.dto.response.AuditActionStatResponse(
            a.action,
            COUNT(a),
            SUM(CASE WHEN a.success = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN a.success = false THEN 1 ELSE 0 END)
        )
        FROM AuditLog a
        GROUP BY a.action
        ORDER BY COUNT(a) DESC, a.action ASC
        """)
    List<AuditActionStatResponse> findTopActionStats(Pageable pageable);

    @Query("""
        SELECT new skylinkers.tn.mediconnectbackend.dto.response.AuditCategoryStatResponse(
            COALESCE(a.category, 'SYSTEM'),
            COUNT(a)
        )
        FROM AuditLog a
        GROUP BY COALESCE(a.category, 'SYSTEM')
        ORDER BY COUNT(a) DESC, COALESCE(a.category, 'SYSTEM') ASC
        """)
    List<AuditCategoryStatResponse> findCategoryBreakdown(Pageable pageable);

    @Query("""
        SELECT new skylinkers.tn.mediconnectbackend.dto.response.MostActiveAccountResponse(
            u.id,
            u.email,
            u.firstName,
            u.lastName,
            u.userType,
            COUNT(a),
            SUM(CASE WHEN a.success = true THEN 1 ELSE 0 END),
            SUM(CASE WHEN a.success = false THEN 1 ELSE 0 END),
            MAX(a.timestamp)
        )
        FROM AuditLog a
        JOIN a.user u
        GROUP BY u.id, u.email, u.firstName, u.lastName, u.userType
        ORDER BY COUNT(a) DESC, MAX(a.timestamp) DESC
        """)
    List<MostActiveAccountResponse> findMostActiveAccounts(Pageable pageable);

    /** Count failed logins per userId in a sliding window (brute-force check). */
    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.user.id = :userId
          AND a.action IN ('AUTH_LOGIN_FAILED', 'LOGIN_FAILED')
          AND a.success = false
          AND a.timestamp >= :since
        """)
    long countFailedLoginsSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** Count failed logins per email — works even when user entity is null. */
    @Query("""
        SELECT COUNT(a) FROM AuditLog a
        WHERE a.userEmail = :email
          AND a.action IN ('AUTH_LOGIN_FAILED', 'LOGIN_FAILED')
          AND a.timestamp >= :since
        """)
    long countRecentFailedLogins(@Param("email") String email, @Param("since") LocalDateTime since);

    /**
     * RGPD right-to-erasure: anonymise logs (replace user reference with null,
     * replace IP with '0.0.0.0', clear PII fields) instead of hard-deleting audit history.
     */
    @Modifying
    @Query("""
        UPDATE AuditLog a
        SET a.user        = null,
            a.userEmail   = '[ERASED]',
            a.keycloakId  = null,
            a.ipAddress   = '0.0.0.0',
            a.userAgent   = '[ERASED]'
        WHERE a.user.id = :userId
        """)
    void anonymiseByUserId(@Param("userId") Long userId);

    /** For AI training data export — all logs in a category within a date range. */
    @Query("""
        SELECT a FROM AuditLog a
        WHERE a.category = :category
          AND a.timestamp BETWEEN :from AND :to
        ORDER BY a.timestamp ASC
        """)
    List<AuditLog> findByCategoryAndDateRange(
            @Param("category") String category,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // ── RiskFeatureBuilder queries ────────────────────────────────────────────

    boolean existsByUserIdAndUserAgentAndSuccessTrueAndTimestampAfter(
            Long userId, String userAgent, LocalDateTime since);

    boolean existsByUserIdAndIpAddressAndSuccessTrueAndTimestampAfter(
            Long userId, String ipAddress, LocalDateTime since);

    long countByUserIdAndSuccessFalseAndTimestampAfter(
            Long userId, LocalDateTime since);

    // ── ML feedback loop ──────────────────────────────────────────────────────

    @Modifying
    @Query("""
        UPDATE AuditLog a
        SET a.twofaOutcome = :outcome,
            a.feedbackLabel = :label
        WHERE a.id = :id
        """)
    void updateFeedback(@Param("id") Long id,
                        @Param("outcome") String outcome,
                        @Param("label") Integer label);
}
