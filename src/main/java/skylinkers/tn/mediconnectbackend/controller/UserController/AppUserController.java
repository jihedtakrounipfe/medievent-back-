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
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.service.MedicalEventService;
import skylinkers.tn.mediconnectbackend.dto.MedicalEventDTO;
import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class AppUserController {
    private final IAppUserSearchService searchService;
    private final AppUserRepository userRepository;
    private final MedicalEventService eventService;

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
        if (jwt == null) return ResponseEntity.status(401).build();
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        try {
            return ResponseEntity.ok(searchService.getByKeycloakId(keycloakId));
        } catch (Exception e) {
            AppUser user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found: " + email));
            user.setKeycloakId(keycloakId);
            userRepository.save(user);
            return ResponseEntity.ok(searchService.getByKeycloakId(keycloakId));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<AppUserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(searchService.getById(id));
    }

    @PutMapping("/interests")
    public ResponseEntity<Void> updateInterests(@AuthenticationPrincipal Jwt jwt, @RequestBody Set<String> interests) {
        if (jwt == null) return ResponseEntity.status(401).build();
        String keycloakId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    AppUser u = userRepository.findByEmail(email)
                            .orElseThrow(() -> new RuntimeException("User not found: " + email));
                    u.setKeycloakId(keycloakId);
                    return u;
                });
        user.setInterests(interests);
        userRepository.save(user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/force-test-email")
    public ResponseEntity<String> forceTestEmail(@RequestParam String email, @RequestParam String tag) {
        MedicalEventDTO testEvent = MedicalEventDTO.builder()
            .title("TEST FINAL: Custom Tag Detection")
            .description("Ceci est une simulation directe pour valider la réception sur votre boîte mail.")
            .eventDate(LocalDateTime.now().plusDays(2))
            .location("Virtuel")
            .targetAudience(skylinkers.tn.mediconnectbackend.entities.enums.EventAudience.PUBLIC)
            .tags(Set.of(tag))
            .maxParticipants(100)
            .build();
        
        // Use an existing doctor email from the DB or a default one
        String doctorEmail = userRepository.findAll().stream()
            .filter(u -> "DOCTOR".equals(u.getUserType().name()))
            .map(u -> u.getEmail())
            .findFirst()
            .orElse("tjihed9@gmail.com");
            
        eventService.createEvent(testEvent, doctorEmail);
        return ResponseEntity.ok("Emails sent to " + email + " for tag '" + tag + "'!");
    }
}
