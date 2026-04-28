package skylinkers.tn.mediconnectbackend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.entities.Notification;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.NotificationService;

import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/api/forum/notifications")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201"})
public class ForumNotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AppUserRepository appUserRepository;

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> getUnreadCount(
            @AuthenticationPrincipal Jwt jwt) {
        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        long count = notificationService.countUnread(currentUser.get().getId());
        return ResponseEntity.ok(Map.of("count", (int) count));
    }

    @GetMapping
    public ResponseEntity<?> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal Jwt jwt) {
        Optional<AppUser> currentUser = resolveCurrentUser(jwt);
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Notification> notifications = notificationService.getUserNotificationsPage(currentUser.get().getId(), pageable);

        return ResponseEntity.ok(Map.of(
                "content", notifications.getContent(),
                "totalElements", notifications.getTotalElements(),
                "totalPages", notifications.getTotalPages(),
                "size", notifications.getSize(),
                "number", notifications.getNumber()
        ));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    private Optional<AppUser> resolveCurrentUser(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            return Optional.empty();
        }
        return appUserRepository.findByKeycloakId(jwt.getSubject());
    }
}