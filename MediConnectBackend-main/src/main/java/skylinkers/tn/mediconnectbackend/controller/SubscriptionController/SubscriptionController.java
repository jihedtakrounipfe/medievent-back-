package skylinkers.tn.mediconnectbackend.controller.SubscriptionController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.*;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.DoctorPlan;
import skylinkers.tn.mediconnectbackend.entities.PatientPlan;
import skylinkers.tn.mediconnectbackend.exception.SubscriptionException.ForbiddenException;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.DoctorPlanRepository;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.PatientPlanRepository;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.SubscriptionService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final PatientPlanRepository patientPlanRepository;
    private final DoctorPlanRepository doctorPlanRepository;
    private final AppUserRepository appUserRepository;

    // ─── PLANS ────────────────────────────────────────────────────────────────

    // GET /api/plans/patient/getAll
    @GetMapping("/plans/patient/getAll")
    public ResponseEntity<List<PatientPlan>> getPatientPlans() {
        return ResponseEntity.ok(patientPlanRepository.findByIsActiveTrue());
    }

    // GET /api/plans/doctor/getAll
    @GetMapping("/plans/doctor/getAll")
    public ResponseEntity<List<DoctorPlan>> getDoctorPlans() {
        return ResponseEntity.ok(doctorPlanRepository.findByIsActiveTrue());
    }

    // ─── CREDIT ───────────────────────────────────────────────────────────────



    // ─── SUBSCRIPTIONS ────────────────────────────────────────────────────────

    // POST /api/subscriptions/subscribe
    @PostMapping("/subscriptions/subscribe")
    public ResponseEntity<SubscriptionResponse> subscribe(@Valid @RequestBody SubscriptionRequest request) {
        return ResponseEntity.ok(subscriptionService.subscribe(request));
    }

    // GET /api/subscriptions/getActive/{userId}
    @GetMapping("/subscriptions/getActive/{userId}")
    public ResponseEntity<SubscriptionResponse> getActiveSubscription(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(subscriptionService.getActiveSubscription(userId));
        } catch (RuntimeException ex) {
            if ("No active subscription found".equals(ex.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            throw ex;
        }
    }

    @GetMapping("/subscriptions/credit/history/{userId}")
    public ResponseEntity<?> getUserCreditHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getUserCreditHistory(userId));
    }

    // PUT /api/subscriptions/cancel/{userId}
    @PutMapping("/subscriptions/cancel/{userId}")
    public ResponseEntity<SubscriptionResponse> cancel(
            @PathVariable Long userId,
            @Valid @RequestBody CancellationRequestDTO request) {
        return ResponseEntity.ok(subscriptionService.cancel(userId, request));
    }

    // GET /api/subscriptions/hasFeature/{userId}/{feature}
    @GetMapping("/subscriptions/hasFeature/{userId}/{feature}")
    public ResponseEntity<Boolean> hasFeature(@PathVariable Long userId, @PathVariable String feature) {
        return ResponseEntity.ok(subscriptionService.hasFeature(userId, feature));
    }

    // PUT /api/subscriptions/autorenew/{userId}
    @PutMapping("/subscriptions/autorenew/{userId}")
    public ResponseEntity<Void> toggleAutoRenew(@PathVariable Long userId) {
        subscriptionService.toggleAutoRenew(userId);
        return ResponseEntity.ok().build();
    }

    // GET /api/subscriptions/credit/{userId}
    @GetMapping("/subscriptions/credit/{userId}")
    public ResponseEntity<?> getUserCredit(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(subscriptionService.getUserCredit(userId));
        } catch (RuntimeException ex) {
            if ("No credit found".equals(ex.getMessage())) {
                return ResponseEntity.notFound().build();
            }
            throw ex;
        }
    }


    // POST /api/subscriptions/calculate-price
    @PostMapping("/subscriptions/calculate-price")
    public ResponseEntity<?> calculatePrice(@RequestBody PriceCalculationRequestDTO request) {
        Long currentUserId = requireCurrentUserId();
        if (request == null || request.getUserId() == null || !request.getUserId().equals(currentUserId)) {
            throw new ForbiddenException("Access denied");
        }
        return ResponseEntity.ok(subscriptionService.calculatePrice(request));
    }

    // AFTER
    @PostMapping("/subscriptions/upgrade-downgrade/{userId}")
    public ResponseEntity<?> upgradeDowngrade(
            @PathVariable Long userId,
            @RequestBody UpgradeDowngradeRequestDTO request) {
        return ResponseEntity.ok(subscriptionService.upgradeDowngrade(userId, request));
    }

    private Long requireCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ForbiddenException("Access denied");
        }

        Object principal = auth.getPrincipal();
        String keycloakId = null;

        if (principal instanceof UUID uuid) {
            keycloakId = uuid.toString();
        } else if (principal instanceof String textPrincipal) {
            keycloakId = textPrincipal;
        } else {
            // Reflective fallback for other principal types
            try {
                Object idValue = principal.getClass().getMethod("getId").invoke(principal);
                if (idValue != null) {
                    keycloakId = idValue.toString();
                }
            } catch (Exception ignored) {
            }
        }

        if (keycloakId == null) {
            throw new ForbiddenException("Access denied: Could not determine user identity");
        }

        return appUserRepository.findByKeycloakId(keycloakId)
                .map(AppUser::getId)
                .orElseThrow(() -> new ForbiddenException("Access denied: User not found in database"));
    }


    // GET /api/subscriptions/getHistory/{userId}
    @GetMapping("/subscriptions/getHistory/{userId}")
    public ResponseEntity<?> getSubscriptionHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions(userId));
    }

}
