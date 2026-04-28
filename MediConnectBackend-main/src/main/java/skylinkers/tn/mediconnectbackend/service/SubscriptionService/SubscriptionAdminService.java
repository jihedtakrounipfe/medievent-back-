package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CancellationStatsDTO;
import skylinkers.tn.mediconnectbackend.entities.DoctorPlan;
import skylinkers.tn.mediconnectbackend.entities.PatientPlan;
import skylinkers.tn.mediconnectbackend.entities.Payment;
import skylinkers.tn.mediconnectbackend.entities.Subscription;

import java.util.List;

public interface SubscriptionAdminService {

    // ─── PATIENT PLANS ────────────────────────────────────────────────────────
    List<PatientPlan> getAllPatientPlans();
    PatientPlan getPatientPlanById(Long id);
    PatientPlan createPatientPlan(PatientPlan plan);
    PatientPlan updatePatientPlan(Long id, PatientPlan plan);
    void deletePatientPlan(Long id);
    void togglePatientPlanStatus(Long id);

    // ─── DOCTOR PLANS ─────────────────────────────────────────────────────────
    List<DoctorPlan> getAllDoctorPlans();
    DoctorPlan getDoctorPlanById(Long id);
    DoctorPlan createDoctorPlan(DoctorPlan plan);
    DoctorPlan updateDoctorPlan(Long id, DoctorPlan plan);
    void deleteDoctorPlan(Long id);
    void toggleDoctorPlanStatus(Long id);

    // ─── SUBSCRIPTIONS ────────────────────────────────────────────────────────
    List<Subscription> getAllSubscriptions();
    List<Subscription> getSubscriptionsByUserId(Long userId);
    CancellationStatsDTO getCancellationStats();
    void cancelSubscription(Long subscriptionId);

    // ─── PAYMENTS ─────────────────────────────────────────────────────────────
    List<Payment> getAllPayments();
    List<Payment> getPaymentsByUserId(Long userId);

    List<Subscription> getSubscriptionsByPlan(String planType, Long planId);
}