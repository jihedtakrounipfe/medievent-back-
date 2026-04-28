package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CancellationStatsDTO;
import skylinkers.tn.mediconnectbackend.entities.*;
import skylinkers.tn.mediconnectbackend.entities.enums.SubscriptionStatus;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionAdminServiceImpl implements SubscriptionAdminService {

    private static final Set<String> CANCELLATION_CATEGORIES = Set.of("PRICE", "FEATURES", "UX", "OTHER");

    private final PatientPlanRepository patientPlanRepository;
    private final DoctorPlanRepository doctorPlanRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;
    private final UserCreditRepository userCreditRepository;
    private final SubscriptionEmailService subscriptionEmailService;

    // ─── PATIENT PLANS ────────────────────────────────────────────────────────

    @Override
    public List<PatientPlan> getAllPatientPlans() {
        return patientPlanRepository.findAll();
    }

    @Override
    public PatientPlan getPatientPlanById(Long id) {
        return patientPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PatientPlan not found"));
    }

    @Override
    @Transactional
    public PatientPlan createPatientPlan(PatientPlan plan) {
        if (patientPlanRepository.existsByNameIgnoreCase(plan.getName())) {
            throw new RuntimeException("A patient plan with this name already exists");
        }
        plan.setIsActive(true);
        plan.setHasDocumentUpload(Boolean.TRUE.equals(plan.getHasDocumentUpload()));
        plan.setHasMedicationReminder(Boolean.TRUE.equals(plan.getHasMedicationReminder()));
        plan.setHasLabResults(Boolean.TRUE.equals(plan.getHasLabResults()));
        plan.setHasSelfTestReadings(Boolean.TRUE.equals(plan.getHasSelfTestReadings()));
        plan.setHasForum(Boolean.TRUE.equals(plan.getHasForum()));
        plan.setHasAI(Boolean.TRUE.equals(plan.getHasAI()));
        plan.setHasHealthEvents(Boolean.TRUE.equals(plan.getHasHealthEvents()));
        plan.setIsPromoApplicable(Boolean.TRUE.equals(plan.getIsPromoApplicable()));
        return patientPlanRepository.save(plan);
    }
    @Override
    @Transactional
    public PatientPlan updatePatientPlan(Long id, PatientPlan plan) {
        PatientPlan existing = patientPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PatientPlan not found"));
        existing.setName(plan.getName());
        existing.setPriceMonthly(plan.getPriceMonthly());
        existing.setPriceYearly(plan.getPriceYearly());
        existing.setMaxAppointmentsPerMonth(plan.getMaxAppointmentsPerMonth());
        existing.setHasDocumentUpload(plan.getHasDocumentUpload());
        existing.setHasMedicationReminder(plan.getHasMedicationReminder());
        existing.setHasLabResults(plan.getHasLabResults());
        existing.setHasSelfTestReadings(plan.getHasSelfTestReadings());
        existing.setHasForum(plan.getHasForum());
        existing.setHasAI(plan.getHasAI());
        existing.setHasHealthEvents(plan.getHasHealthEvents());
        return patientPlanRepository.save(existing);
    }

    @Override
    @Transactional
    public void togglePatientPlanStatus(Long id) {
        PatientPlan plan = patientPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PatientPlan not found"));
        
        // If deactivating (true → false), handle credit calculation
        if (plan.getIsActive()) {
            handlePlanDeactivation(plan, true); // true = patient plan
        }
        
        plan.setIsActive(!plan.getIsActive());
        patientPlanRepository.save(plan);
    }

    // ─── DOCTOR PLANS ─────────────────────────────────────────────────────────

    @Override
    public List<DoctorPlan> getAllDoctorPlans() {
        return doctorPlanRepository.findAll();
    }

    @Override
    public DoctorPlan getDoctorPlanById(Long id) {
        return doctorPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DoctorPlan not found"));
    }

    @Override
    @Transactional
    public DoctorPlan createDoctorPlan(DoctorPlan plan) {
        if (doctorPlanRepository.existsByNameIgnoreCase(plan.getName())) {
            throw new RuntimeException("A doctor plan with this name already exists");
        }
        plan.setIsActive(true);
        plan.setHasCalendarSync(Boolean.TRUE.equals(plan.getHasCalendarSync()));
        plan.setHasSearchVisibility(Boolean.TRUE.equals(plan.getHasSearchVisibility()));
        plan.setHasBasicAnalytics(Boolean.TRUE.equals(plan.getHasBasicAnalytics()));
        plan.setHasAdvancedAnalytics(Boolean.TRUE.equals(plan.getHasAdvancedAnalytics()));
        plan.setHasForumBadge(Boolean.TRUE.equals(plan.getHasForumBadge()));
        plan.setHasConsultationPrerequisites(Boolean.TRUE.equals(plan.getHasConsultationPrerequisites()));
        plan.setHasAI(Boolean.TRUE.equals(plan.getHasAI()));
        return doctorPlanRepository.save(plan);
    }
    @Override
    @Transactional
    public DoctorPlan updateDoctorPlan(Long id, DoctorPlan plan) {
        DoctorPlan existing = doctorPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DoctorPlan not found"));
        existing.setName(plan.getName());
        existing.setPriceMonthly(plan.getPriceMonthly());
        existing.setPriceYearly(plan.getPriceYearly());
        existing.setMaxConsultationsPerMonth(plan.getMaxConsultationsPerMonth());
        existing.setHasCalendarSync(plan.getHasCalendarSync());
        existing.setHasSearchVisibility(plan.getHasSearchVisibility());
        existing.setHasBasicAnalytics(plan.getHasBasicAnalytics());
        existing.setHasAdvancedAnalytics(plan.getHasAdvancedAnalytics());
        existing.setHasForumBadge(plan.getHasForumBadge());
        existing.setHasConsultationPrerequisites(plan.getHasConsultationPrerequisites());
        existing.setHasAI(plan.getHasAI());
        return doctorPlanRepository.save(existing);
    }

    @Override
    @Transactional
    public void toggleDoctorPlanStatus(Long id) {
        DoctorPlan plan = doctorPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DoctorPlan not found"));
        
        // If deactivating (true → false), handle credit calculation
        if (plan.getIsActive()) {
            handlePlanDeactivation(plan, false); // false = doctor plan
        }
        
        plan.setIsActive(!plan.getIsActive());
        doctorPlanRepository.save(plan);
    }

    // ─── SUBSCRIPTIONS ────────────────────────────────────────────────────────

    @Override
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    @Override
    public List<Subscription> getSubscriptionsByUserId(Long userId) {
        return subscriptionRepository.findByUser_Id(userId);
    }

    @Override
    public CancellationStatsDTO getCancellationStats() {
        List<Subscription> subscriptions = subscriptionRepository.findByCancellationCategoryIsNotNull();

        Map<String, Long> categoryCounts = new LinkedHashMap<>();
        categoryCounts.put("PRICE", 0L);
        categoryCounts.put("FEATURES", 0L);
        categoryCounts.put("UX", 0L);
        categoryCounts.put("OTHER", 0L);

        for (Subscription subscription : subscriptions) {
            String raw = subscription.getCancellationCategory();
            if (raw == null) {
                continue;
            }

            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            if (!CANCELLATION_CATEGORIES.contains(normalized)) {
                normalized = "OTHER";
            }

            categoryCounts.put(normalized, categoryCounts.get(normalized) + 1);
        }

        return CancellationStatsDTO.builder()
                .categoryCounts(categoryCounts)
                .build();
    }

    @Override
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("Only active subscriptions can be cancelled");
        }

        BigDecimal credit = calculateProratedCredit(subscription);
        if (credit.compareTo(BigDecimal.ZERO) > 0) {
            storeUserCredit(subscription.getUser().getId(), credit);
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setAutoRenew(false);
        subscriptionRepository.save(subscription);

        String planName = subscription.getPatientPlan() != null
                ? subscription.getPatientPlan().getName()
                : (subscription.getDoctorPlan() != null ? subscription.getDoctorPlan().getName() : "Subscription");
        subscriptionEmailService.sendCancellationConfirmation(subscription.getUser().getEmail(), planName);
    }

    // ─── PAYMENTS ─────────────────────────────────────────────────────────────

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }

    @Override
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    @Override
    @Transactional
    public void deletePatientPlan(Long id) {
        PatientPlan plan = patientPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("PatientPlan not found"));

        boolean hasSubscribers = !subscriptionRepository
                .findByPatientPlan_IdAndStatus(id, SubscriptionStatus.ACTIVE).isEmpty();
        if (hasSubscribers) {
            throw new RuntimeException("Cannot delete plan with active subscribers. Deactivate it instead.");
        }

        boolean usedInPayments = paymentRepository.existsByPatientPlan_Id(id);
        if (usedInPayments) {
            throw new RuntimeException("Cannot delete plan with payment records. Deactivate it instead.");
        }

        patientPlanRepository.delete(plan);
    }

    @Override
    @Transactional
    public void deleteDoctorPlan(Long id) {
        DoctorPlan plan = doctorPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("DoctorPlan not found"));

        boolean hasSubscribers = !subscriptionRepository
                .findByDoctorPlan_IdAndStatus(id, SubscriptionStatus.ACTIVE).isEmpty();
        if (hasSubscribers) {
            throw new RuntimeException("Cannot delete plan with active subscribers. Deactivate it instead.");
        }

        boolean usedInPayments = paymentRepository.existsByDoctorPlan_Id(id);
        if (usedInPayments) {
            throw new RuntimeException("Cannot delete plan with payment records. Deactivate it instead.");
        }

        doctorPlanRepository.delete(plan);
    }

    // ─── CREDIT SYSTEM ────────────────────────────────────────────────────────

    /**
     * Handle plan deactivation: Calculate prorated credits and cancel active subscriptions
     */
    private void handlePlanDeactivation(Object plan, boolean isPatientPlan) {
        Long planId = isPatientPlan ? ((PatientPlan) plan).getId() : ((DoctorPlan) plan).getId();
        String planName = isPatientPlan ? ((PatientPlan) plan).getName() : ((DoctorPlan) plan).getName();
        
        // Find ALL active subscriptions for this plan (regardless of autoRenew setting)
        List<Subscription> activeSubscriptions;
        if (isPatientPlan) {
            activeSubscriptions = subscriptionRepository
                    .findByPatientPlan_IdAndStatus(planId, SubscriptionStatus.ACTIVE);
        } else {
            activeSubscriptions = subscriptionRepository
                    .findByDoctorPlan_IdAndStatus(planId, SubscriptionStatus.ACTIVE);
        }
        
        if (activeSubscriptions.isEmpty()) {
            log.info("Plan {} has no active subscriptions to deactivate", planName);
            return;
        }
        
        // Process each subscription
        for (Subscription subscription : activeSubscriptions) {
            // Calculate prorated credit
            BigDecimal credit = calculateProratedCredit(subscription);
            
            if (credit.compareTo(BigDecimal.ZERO) > 0) {
                // Store credit in UserCredit
                Long userId = subscription.getUser().getId();
                storeUserCredit(userId, credit);
                
                // Send email notification with credit amount
                String userEmail = subscription.getUser().getEmail();
                subscriptionEmailService.sendPlanDeprecationNotification(
                    userEmail, 
                    planName, 
                    credit,
                    LocalDate.now().plusYears(1)
                );
            }
            
            // Cancel subscription
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setCancelledAt(LocalDateTime.now());
            subscription.setAutoRenew(false);
            subscriptionRepository.save(subscription);
            
            log.info("Deactivated subscription {} for user {} with credit {}", 
                    subscription.getId(), subscription.getUser().getId(), credit);
        }
    }
    
    /**
     * Calculate prorated credit for a subscription
     * credit = (amountPaid / totalDays) * daysRemaining
     */
    private BigDecimal calculateProratedCredit(Subscription subscription) {
        if (subscription.getAmountPaid() == null) {
            log.warn("Subscription {} has no amountPaid, cannot calculate credit", subscription.getId());
            return BigDecimal.ZERO;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate startDate = subscription.getStartDate();
        LocalDate endDate = subscription.getEndDate();
        
        // Calculate total days and remaining days
        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
        long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(today, endDate) + 1;
        
        if (daysRemaining <= 0) {
            return BigDecimal.ZERO;
        }
        
        // dailyRate = amountPaid / totalDays
        BigDecimal dailyRate = subscription.getAmountPaid()
                .divide(BigDecimal.valueOf(totalDays), 4, java.math.RoundingMode.HALF_UP);
        
        // credit = dailyRate * daysRemaining
        BigDecimal credit = dailyRate
                .multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        
        return credit;
    }
    
    /**
     * Store or update user credit
     */
    private void storeUserCredit(Long userId, BigDecimal creditAmount) {
        UserCredit userCredit = userCreditRepository.findByUserId(userId)
                .orElse(null);
        
        LocalDate expiresAt = LocalDate.now().plusYears(1);
        
        if (userCredit == null) {
            // Create new credit record
            userCredit = UserCredit.builder()
                    .userId(userId)
                    .balance(creditAmount)
                    .expiresAt(expiresAt)
                    .build();
        } else {
            // Update existing credit
            userCredit.setBalance(userCredit.getBalance().add(creditAmount));
            userCredit.setExpiresAt(expiresAt);
        }
        
        userCreditRepository.save(userCredit);
        log.info("Stored credit {} for user {}, expires at {}", creditAmount, userId, expiresAt);
    }

    @Override
    public List<Subscription> getSubscriptionsByPlan(String planType, Long planId) {
        if ("patient".equalsIgnoreCase(planType)) {
            return subscriptionRepository.findByPatientPlan_Id(planId);
        }
        return subscriptionRepository.findByDoctorPlan_Id(planId);
    }
}