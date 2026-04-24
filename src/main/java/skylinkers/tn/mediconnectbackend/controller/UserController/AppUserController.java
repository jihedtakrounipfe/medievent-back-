package skylinkers.tn.mediconnectbackend.controller.UserController;

import skylinkers.tn.mediconnectbackend.dto.request.UserSearchCriteria;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.IAppUserSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Cross-entity user search + "who am I" resolution.
 *
 * GET /api/v1/users/search  — search across all user types (patients & doctors)
 * GET /api/v1/users/me      — resolve caller's own full profile from JWT subject
 * GET /api/v1/users/{id}    — fetch any user by internal DB id (admin)
 *
 * Results are lightweight AppUserResponse projections — no PII (no SSN, no embedding vectors).
 * Full profiles are at /patients/{id} and /doctors/{id} respectively.
 *
 * SOLID:
 *   SRP — search + identity resolution only
 *   DIP — depends on IAppUserSearchService interface
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AppUserController {

    private final IAppUserSearchService searchService;

    /**
     * GET /api/v1/users/search
     *
     * All params are optional — combine freely.
     *
     * Examples:
     *   /users/search?name=ali&userType=DOCTOR
     *   /users/search?specialization=CARDIOLOGY&city=Tunis&isVerified=true
     *   /users/search?name=benali&isActive=true&page=0&size=10
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('DOCTOR') or hasRole('PATIENT')")
    public ResponseEntity<Page<AppUserResponse>> search(
            @RequestParam(required = false) String         name,
            @RequestParam(required = false) String         email,
            @RequestParam(required = false) UserType       userType,
            @RequestParam(required = false) Specialization specialization,
            @RequestParam(required = false) String         city,
            @RequestParam(required = false) Boolean        isActive,
            @RequestParam(required = false) Boolean        isVerified,
            @PageableDefault(size = 20) Pageable           pageable) {

        // Build criteria object — keeps controller thin (no logic here)
        UserSearchCriteria criteria = new UserSearchCriteria();
        criteria.setName(name);
        criteria.setEmail(email);
        criteria.setUserType(userType);
        criteria.setSpecialization(specialization);
        criteria.setCity(city);
        criteria.setIsActive(isActive);
        criteria.setIsVerified(isVerified);

        return ResponseEntity.ok(searchService.search(criteria, pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<AppUserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(searchService.getByKeycloakId(jwt.getSubject()));
    }


    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<AppUserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(searchService.getById(id));
    }
}
