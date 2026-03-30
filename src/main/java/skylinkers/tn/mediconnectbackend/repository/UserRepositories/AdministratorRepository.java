package skylinkers.tn.mediconnectbackend.repository.UserRepositories;

import skylinkers.tn.mediconnectbackend.entities.Administrator;
import skylinkers.tn.mediconnectbackend.entities.enums.AdminLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdministratorRepository extends JpaRepository<Administrator, Long> {

    Optional<Administrator> findByKeycloakId(String keycloakId);

    Optional<Administrator> findByEmail(String email);

    List<Administrator> findByAdminLevel(AdminLevel level);
}

