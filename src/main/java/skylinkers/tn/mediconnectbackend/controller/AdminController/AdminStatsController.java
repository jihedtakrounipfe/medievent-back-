package skylinkers.tn.mediconnectbackend.controller.AdminController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import skylinkers.tn.mediconnectbackend.entities.enums.UserType;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;

@RestController
@RequestMapping({"/api/v1/admin", "/api/admin"})
@RequiredArgsConstructor
public class AdminStatsController {

    public record AdminStatsResponse(
            long totalUsers,
            long totalPatients,
            long totalDoctors,
            long activeUsers
    ) {}

    private final AppUserRepository userRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminStatsResponse> stats() {
        long total = userRepository.count();
        long patients = userRepository.countByUserType(UserType.PATIENT);
        long doctors = userRepository.countByUserType(UserType.DOCTOR);
        long active = userRepository.countByUserTypeAndIsActiveTrue(UserType.PATIENT)
                + userRepository.countByUserTypeAndIsActiveTrue(UserType.DOCTOR)
                + userRepository.countByUserTypeAndIsActiveTrue(UserType.ADMINISTRATOR);
        return ResponseEntity.ok(new AdminStatsResponse(total, patients, doctors, active));
    }
}
