package skylinkers.tn.mediconnectbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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

    List<VerificationCode> findByEmailAndUsedFalse(String email);

    long countByEmailAndCreatedAtAfter(String email, LocalDateTime after);

    void deleteByExpiresAtBefore(LocalDateTime now);
}

