package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Root repository for the SINGLE_TABLE hierarchy.
 *
 * SOLID — ISP: Only generic cross-type queries live here.
 *              Type-specific queries go in PatientRepository, DoctorRepository, etc.
 */
@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmail(String email);

    Optional<AppUser> findByKeycloakId(String keycloakId);

    boolean existsByEmail(String email);

    boolean existsByKeycloakId(String keycloakId);

    @Query("SELECT u FROM AppUser u WHERE u.userType = :userType AND u.isActive = true")
    java.util.List<AppUser> findAllActiveByUserType(UserType userType);
}

