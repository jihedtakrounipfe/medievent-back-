package skylinkers.tn.mediconnectbackend.service.UserServices.IUser;

import skylinkers.tn.mediconnectbackend.dto.request.UserSearchCriteria;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IAppUserSearchService {

    /**
     * Search across all user subtypes using optional criteria.
     * Results are lightweight AppUserResponse projections — no PII.
     */
    Page<AppUserResponse> search(UserSearchCriteria criteria, Pageable pageable);

    /** Resolve the subtype + return full profile from a Keycloak UUID. */
    AppUserResponse getByKeycloakId(String keycloakId);

    /** Resolve any user by internal DB id — used by admin. */
    AppUserResponse getById(Long id);
}