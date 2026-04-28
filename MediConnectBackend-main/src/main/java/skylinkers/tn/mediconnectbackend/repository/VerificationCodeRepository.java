package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skylinkers.tn.mediconnectbackend.entities.VerificationCode;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {
    Optional<VerificationCode> findByEmailAndCodeAndUsedFalseAndExpiresAtAfter(
            String email,
            String code,
            LocalDateTime now
    );

    Optional<VerificationCode> findByEmailAndCodeAndPurposeAndUsedFalseAndExpiresAtAfter(
            String email,
            String code,
            String purpose,
            LocalDateTime now
    );

    List<VerificationCode> findByEmailAndUsedFalse(String email);

    List<VerificationCode> findByEmailAndPurposeAndUsedFalse(String email, String purpose);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    /** Rate-limit check: how many codes have been sent for this email+purpose since a given time. */
    @Query("""
        SELECT COUNT(v) FROM VerificationCode v
        WHERE v.email = :email
          AND v.purpose = :purpose
          AND v.createdAt >= :since
        """)
    long countByEmailAndPurposeAndCreatedAtAfter(
            @Param("email") String email,
            @Param("purpose") String purpose,
            @Param("since") LocalDateTime since);

    void deleteByExpiresAtBefore(LocalDateTime now);
}

