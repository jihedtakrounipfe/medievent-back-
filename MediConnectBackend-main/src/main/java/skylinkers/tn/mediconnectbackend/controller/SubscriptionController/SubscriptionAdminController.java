package skylinkers.tn.mediconnectbackend.controller.SubscriptionController;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CancellationStatsDTO;
import skylinkers.tn.mediconnectbackend.entities.DoctorPlan;
import skylinkers.tn.mediconnectbackend.entities.PatientPlan;
import skylinkers.tn.mediconnectbackend.entities.Payment;
import skylinkers.tn.mediconnectbackend.entities.Subscription;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.SubscriptionAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class SubscriptionAdminController {

    private final SubscriptionAdminService subscriptionAdminService;

    // ─── PATIENT PLANS ────────────────────────────────────────────────────────

    @GetMapping("/plans/patient/getAll")
    public ResponseEntity<List<PatientPlan>> getAllPatientPlans() {
        return ResponseEntity.ok(subscriptionAdminService.getAllPatientPlans());
    }

    @GetMapping("/plans/patient/get/{id}")
    public ResponseEntity<PatientPlan> getPatientPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionAdminService.getPatientPlanById(id));
    }

    @PostMapping("/plans/patient/create")
    public ResponseEntity<PatientPlan> createPatientPlan(@RequestBody PatientPlan plan) {
        return ResponseEntity.ok(subscriptionAdminService.createPatientPlan(plan));
    }

    @PutMapping("/plans/patient/update/{id}")
    public ResponseEntity<PatientPlan> updatePatientPlan(@PathVariable Long id, @RequestBody PatientPlan plan) {
        return ResponseEntity.ok(subscriptionAdminService.updatePatientPlan(id, plan));
    }

    @DeleteMapping("/plans/patient/delete/{id}")
    public ResponseEntity<Void> deletePatientPlan(@PathVariable Long id) {
        subscriptionAdminService.deletePatientPlan(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/plans/patient/toggle/{id}")
    public ResponseEntity<Void> togglePatientPlanStatus(@PathVariable Long id) {
        subscriptionAdminService.togglePatientPlanStatus(id);
        return ResponseEntity.ok().build();
    }

    // ─── DOCTOR PLANS ─────────────────────────────────────────────────────────

    @GetMapping("/plans/doctor/getAll")
    public ResponseEntity<List<DoctorPlan>> getAllDoctorPlans() {
        return ResponseEntity.ok(subscriptionAdminService.getAllDoctorPlans());
    }

    @GetMapping("/plans/doctor/get/{id}")
    public ResponseEntity<DoctorPlan> getDoctorPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(subscriptionAdminService.getDoctorPlanById(id));
    }

    @PostMapping("/plans/doctor/create")
    public ResponseEntity<DoctorPlan> createDoctorPlan(@RequestBody DoctorPlan plan) {
        return ResponseEntity.ok(subscriptionAdminService.createDoctorPlan(plan));
    }

    @PutMapping("/plans/doctor/update/{id}")
    public ResponseEntity<DoctorPlan> updateDoctorPlan(@PathVariable Long id, @RequestBody DoctorPlan plan) {
        return ResponseEntity.ok(subscriptionAdminService.updateDoctorPlan(id, plan));
    }

    @DeleteMapping("/plans/doctor/delete/{id}")
    public ResponseEntity<Void> deleteDoctorPlan(@PathVariable Long id) {
        subscriptionAdminService.deleteDoctorPlan(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/plans/doctor/toggle/{id}")
    public ResponseEntity<Void> toggleDoctorPlanStatus(@PathVariable Long id) {
        subscriptionAdminService.toggleDoctorPlanStatus(id);
        return ResponseEntity.ok().build();
    }

    // ─── SUBSCRIPTIONS ────────────────────────────────────────────────────────

    @GetMapping("/subscriptions/getAll")
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionAdminService.getAllSubscriptions());
    }

    @GetMapping("/subscriptions/getByUser/{userId}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionAdminService.getSubscriptionsByUserId(userId));
    }

    @GetMapping("/subscriptions/cancellation-stats")
    public ResponseEntity<CancellationStatsDTO> getCancellationStats() {
        return ResponseEntity.ok(subscriptionAdminService.getCancellationStats());
    }

    @PutMapping("/subscriptions/cancel/{id}")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long id) {
        subscriptionAdminService.cancelSubscription(id);
        return ResponseEntity.ok().build();
    }

    // ─── PAYMENTS ─────────────────────────────────────────────────────────────

    @GetMapping("/payments/getAll")
    public ResponseEntity<List<Payment>> getAllPayments() {
        return ResponseEntity.ok(subscriptionAdminService.getAllPayments());
    }

    @GetMapping("/payments/getByUser/{userId}")
    public ResponseEntity<List<Payment>> getPaymentsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(subscriptionAdminService.getPaymentsByUserId(userId));
    }

    @GetMapping("/subscriptions/getByPlan/{planType}/{planId}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByPlan(
            @PathVariable String planType,
            @PathVariable Long planId) {
        return ResponseEntity.ok(subscriptionAdminService.getSubscriptionsByPlan(planType, planId));
    }
}
