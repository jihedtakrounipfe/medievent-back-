package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import skylinkers.tn.mediconnectbackend.entities.Doctor;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByRppsNumber(String rppsNumber);
    Optional<Doctor> findByEmail(String email);

    Optional<Doctor> findByKeycloakId(String keycloakId);

    boolean existsByRppsNumber(String rppsNumber);

    /** Patient-facing search: only APPROVED + active doctors. */
    Page<Doctor> findBySpecializationAndVerificationStatusAndIsActiveTrue(
            Specialization specialization,
            VerificationStatus verificationStatus,
            Pageable pageable
    );

    /** Admin panel: doctors awaiting RPPS verification. */
    List<Doctor> findByVerificationStatus(VerificationStatus status);

    List<Doctor> findByClinicId(Long clinicId);

    @Modifying
    @Query("UPDATE Doctor d SET d.verificationStatus = :status WHERE d.id = :id")
    void updateVerificationStatus(Long id, VerificationStatus status);

    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.verificationStatus = 'PENDING'")
    long countPendingVerification();
}
