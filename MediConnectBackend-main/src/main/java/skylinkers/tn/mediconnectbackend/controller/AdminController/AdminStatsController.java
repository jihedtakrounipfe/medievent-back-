package skylinkers.tn.mediconnectbackend.controller.AdminController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skylinkers.tn.mediconnectbackend.dto.response.AdminAuditStatsResponse;
import skylinkers.tn.mediconnectbackend.service.UserServices.IUser.AuditLogService;

@RestController
@RequestMapping({"/api/v1/admin", "/api/admin"})
@RequiredArgsConstructor
public class AdminStatsController {

    private final AuditLogService auditLogService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<AdminAuditStatsResponse> stats() {
        return ResponseEntity.ok(auditLogService.getAdminAuditStats());
    }
}
