package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import skylinkers.tn.mediconnectbackend.entities.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    /** Patients with high no-show risk — used by the scheduling service. */
    @Query("SELECT p FROM Patient p WHERE p.noShowScore >= :threshold AND p.isActive = true")
    Page<Patient> findHighRiskPatients(Double threshold, Pageable pageable);

    /** Patients who have enrolled biometrics — used to gate facial auth. */
    Page<Patient> findByBiometricEnrolledTrue(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.isActive = true")
    long countActive();
}
