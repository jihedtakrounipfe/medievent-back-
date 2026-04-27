package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
public interface AppUserRepository extends JpaRepository<AppUser, Long> , JpaSpecificationExecutor<AppUser> {

    Optional<AppUser> findByKeycloakId(String keycloakId);
    Optional<AppUser> findByEmail(String email);
    boolean           existsByEmail(String email);
    boolean           existsByKeycloakId(String keycloakId);

    @Query("SELECT u FROM AppUser u WHERE u.userType = :userType AND u.isActive = true")
    java.util.List<AppUser> findAllActiveByUserType(UserType userType);

    long countByUserType(UserType userType);

    long countByUserTypeAndIsActiveTrue(UserType userType);
    
    @Query("SELECT DISTINCT u FROM AppUser u JOIN u.interests i WHERE i IN :interests")
    java.util.List<AppUser> findByInterestsIn(java.util.Set<String> interests);
}

