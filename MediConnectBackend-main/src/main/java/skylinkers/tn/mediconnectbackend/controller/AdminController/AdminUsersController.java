package skylinkers.tn.mediconnectbackend.controller.AdminController;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import skylinkers.tn.mediconnectbackend.dto.request.UserSearchCriteria;
import skylinkers.tn.mediconnectbackend.dto.response.AppUserResponse;
import skylinkers.tn.mediconnectbackend.dto.response.AuditLogResponse;
import skylinkers.tn.mediconnectbackend.dto.response.UserAuditSummaryResponse;
import skylinkers.tn.mediconnectbackend.entities.enums.Specialization;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.IAppUserSearchService;

import java.time.LocalDate;

@RestController
@RequestMapping({"/api/admin/users", "/api/v1/admin/users"})
@RequiredArgsConstructor
public class AdminUsersController {

    private final IAppUserSearchService searchService;
    private final AppUserRepository userRepository;
    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<AppUserResponse>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(searchService.search(new UserSearchCriteria(), pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<AppUserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(searchService.getById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<AppUserResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserType userType,
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bloodType,
            @RequestParam(required = false) Integer ageMin,
            @RequestParam(required = false) Integer ageMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate signedUpSince,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate signedUpUntil,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean is2FAEnabled,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        UserSearchCriteria criteria = new UserSearchCriteria();
        criteria.setQ(q);
        criteria.setUserType(userType);
        criteria.setStatus(status);
        criteria.setBloodType(bloodType);
        criteria.setAgeMin(ageMin);
        criteria.setAgeMax(ageMax);
        criteria.setSignedUpSince(signedUpSince);
        criteria.setSignedUpUntil(signedUpUntil);
        criteria.setIsActive(isActive);
        criteria.setIs2FAEnabled(is2FAEnabled);

        if (specialty != null && !specialty.isBlank()) {
            try {
                criteria.setSpecialization(Specialization.valueOf(specialty));
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid specialization filters instead of failing the whole query.
            }
        }

        return ResponseEntity.ok(searchService.search(criteria, pageable));
    }

    @GetMapping("/{id}/audit-logs")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getLogsForUser(id, pageable));
    }

    @GetMapping("/{id}/audit-summary")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<UserAuditSummaryResponse> getAuditSummary(@PathVariable Long id) {
        return ResponseEntity.ok(auditLogService.getSummaryForUser(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
