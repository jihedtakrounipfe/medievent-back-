package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.*;
import skylinkers.tn.mediconnectbackend.entities.*;
import skylinkers.tn.mediconnectbackend.entities.enums.*;
import skylinkers.tn.mediconnectbackend.exception.SubscriptionException.BadRequestException;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.*;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PatientPlanRepository patientPlanRepository;
    private final DoctorPlanRepository doctorPlanRepository;
    private final AppUserRepository appUserRepository;
    private final InvoiceRepository invoiceRepository;
    private final UserCreditRepository userCreditRepository;
    private final PaymentRepository paymentRepository;
    private final SubscriptionEmailService subscriptionEmailService;
    private final GroqService groqService;
    private final PromoCodeService promoCodeService;
    private final StudentVerificationService studentVerificationService;
    private final InvoiceAndEmailService invoiceAndEmailService;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    @Value("${stripe.checkout.currency:usd}")
    private String stripeCheckoutCurrency;

    @Value("${stripe.checkout.tnd-to-currency-rate:0.32}")
    private BigDecimal tndToCheckoutCurrencyRate;

    // ─── SUBSCRIBE ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SubscriptionResponse subscribe(SubscriptionRequest request) {

        AppUser user = appUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getUserType().name().equals(request.getPlanType().name())) {
            throw new RuntimeException(
                    "User role " + user.getUserType() + " cannot subscribe to a " + request.getPlanType() + " plan");
        }

        if (subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
            throw new RuntimeException("User already has an active subscription. Cancel it first.");
        }

        // ── Declare outside if-blocks so they stay in scope below ──
        PatientPlan patientPlan = null;
        DoctorPlan doctorPlan = null;

        Subscription subscription = new Subscription();
        subscription.setUser(user);
        subscription.setPlanType(request.getPlanType());
        subscription.setBillingCycle(request.getBillingCycle());
        subscription.setPaymentProvider(request.getPaymentProvider());
        subscription.setAutoRenew(request.getAutoRenew());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartDate(LocalDate.now());
        subscription.setLastPaymentAt(LocalDateTime.now());

        if (request.getPlanType() == PlanType.PATIENT) {
            patientPlan = patientPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("PatientPlan not found"));
            subscription.setPatientPlan(patientPlan);
        } else {
            doctorPlan = doctorPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("DoctorPlan not found"));
            subscription.setDoctorPlan(doctorPlan);
        }

        LocalDate endDate = switch (request.getBillingCycle()) {
            case MONTHLY -> LocalDate.now().plusMonths(1);
            case YEARLY -> LocalDate.now().plusYears(1);
        };
        subscription.setEndDate(endDate);

        // ── Use the in-scope plan variables, respect billing cycle ──
        BigDecimal planPrice = patientPlan != null
                ? (request.getBillingCycle() == BillingCycle.YEARLY ? patientPlan.getPriceYearly() : patientPlan.getPriceMonthly())
                : (request.getBillingCycle() == BillingCycle.YEARLY ? doctorPlan.getPriceYearly() : doctorPlan.getPriceMonthly());
        subscription.setAmountPaid(planPrice);

        Subscription saved = subscriptionRepository.save(subscription);

        // ── Create synthetic payment so invoice generation has a Payment entity ──
        Payment syntheticPayment = Payment.builder()
                .user(user)
                .amount(planPrice)
                .originalPriceTnd(planPrice)
                .currency("TND")
                .status(PaymentStatus.SUCCESS)
                .billingCycle(request.getBillingCycle())
                .patientPlan(patientPlan)
                .doctorPlan(doctorPlan)
                .build();
        paymentRepository.save(syntheticPayment);
        invoiceAndEmailService.generateInvoiceAndSendEmail(saved.getId(), syntheticPayment.getId());

        return buildResponse(saved);
    }

    // ─── GET ACTIVE SUBSCRIPTION ──────────────────────────────────────────────

    @Override
    public SubscriptionResponse getActiveSubscription(Long userId) {
        Subscription subscription = subscriptionRepository
                .findByUser_IdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));
        return buildResponse(subscription);
    }

    // ─── GET ALL SUBSCRIPTIONS ────────────────────────────────────────────────

    @Override
    public List<SubscriptionResponse> getAllSubscriptions(Long userId) {
        return subscriptionRepository.findByUser_Id(userId)
                .stream()
                .map(this::buildResponse)
                .toList();
    }

    // ─── CANCEL ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SubscriptionResponse cancel(Long userId, CancellationRequestDTO request) {
        if (request == null || request.getSubscriptionId() == null) {
            throw new RuntimeException("subscriptionId is required");
        }

        Subscription subscription = subscriptionRepository
                .findById(request.getSubscriptionId())
                .orElseThrow(() -> new RuntimeException("No active subscription to cancel"));

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new RuntimeException("No active subscription to cancel");
        }

        if (!subscription.getUser().getId().equals(userId)) {
            throw new RuntimeException("Subscription does not belong to the current user");
        }

        String reason = request.getReason() != null ? request.getReason().trim() : "";
        if (reason.length() > 500) {
            throw new RuntimeException("reason must not exceed 500 characters");
        }

        String category = groqService.classifyCancellationReason(reason);
        subscription.setCancellationReason(reason.isBlank() ? null : reason);
        subscription.setCancellationCategory(category);

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscription.setCancelledAt(LocalDateTime.now());
        subscription.setAutoRenew(false);

        Subscription saved = subscriptionRepository.save(subscription);

        // Send cancellation email
        String planName = saved.getPatientPlan() != null
            ? saved.getPatientPlan().getName()
            : saved.getDoctorPlan().getName();
        subscriptionEmailService.sendCancellationConfirmation(saved.getUser().getEmail(), planName);

        return buildResponse(saved);
    }

    // ─── HAS FEATURE ─────────────────────────────────────────────────────────

    @Override
    public boolean hasFeature(Long userId, String feature) {
        return subscriptionRepository
                .findByUser_IdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .map(sub -> checkFeature(sub, feature))
                .orElse(false);
    }

    private boolean checkFeature(Subscription sub, String feature) {
        if (sub.getPlanType() == PlanType.PATIENT && sub.getPatientPlan() != null) {
            PatientPlan plan = sub.getPatientPlan();
            return switch (feature) {
                case "AI"                  -> plan.getHasAI();
                case "FORUM"               -> plan.getHasForum();
                case "MEDICATION_REMINDER" -> plan.getHasMedicationReminder();
                case "DOCUMENT_UPLOAD"     -> plan.getHasDocumentUpload();
                case "LAB_RESULTS"         -> plan.getHasLabResults();
                case "SELF_TEST_READINGS"  -> plan.getHasSelfTestReadings();
                case "HEALTH_EVENTS"       -> plan.getHasHealthEvents();
                default                    -> false;
            };
        }

        if (sub.getPlanType() == PlanType.DOCTOR && sub.getDoctorPlan() != null) {
            DoctorPlan plan = sub.getDoctorPlan();
            return switch (feature) {
                case "AI"                         -> plan.getHasAI();
                case "CALENDAR_SYNC"              -> plan.getHasCalendarSync();
                case "SEARCH_VISIBILITY"          -> plan.getHasSearchVisibility();
                case "BASIC_ANALYTICS"            -> plan.getHasBasicAnalytics();
                case "ADVANCED_ANALYTICS"         -> plan.getHasAdvancedAnalytics();
                case "FORUM_BADGE"                -> plan.getHasForumBadge();
                case "CONSULTATION_PREREQUISITES" -> plan.getHasConsultationPrerequisites();
                default                           -> false;
            };
        }

        return false;
    }

    // ─── SCHEDULED JOB ───────────────────────────────────────────────────────

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireSubscriptions() {
        List<Subscription> expired = subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, LocalDate.now());

        for (Subscription sub : expired) {
            if (sub.getAutoRenew()) {
                LocalDate newEnd = switch (sub.getBillingCycle()) {
                    case MONTHLY -> sub.getEndDate().plusMonths(1);
                    case YEARLY  -> sub.getEndDate().plusYears(1);
                };
                sub.setEndDate(newEnd);
                sub.setLastPaymentAt(LocalDateTime.now());
            } else {
                sub.setStatus(SubscriptionStatus.EXPIRED);
            }
        }

        subscriptionRepository.saveAll(expired);
    }

    // ─── RENEW TOGGLE ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public void toggleAutoRenew(Long userId) {
        Subscription subscription = subscriptionRepository
                .findByUser_IdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        subscription.setAutoRenew(!subscription.getAutoRenew());
        subscriptionRepository.save(subscription);
    }

    @Override
    public UserCredit getUserCredit(Long userId) {
        return userCreditRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("No credit found"));
    }

    @Override
    public List<CreditHistoryEntryDTO> getUserCreditHistory(Long userId) {
        List<CreditHistoryEntryDTO> history = new ArrayList<>();

        // Real credit applied to payments
        List<Payment> creditAppliedPayments = paymentRepository
                .findByUserIdAndCreditAppliedGreaterThanOrderByCreatedAtDesc(userId, BigDecimal.ZERO);
        for (Payment payment : creditAppliedPayments) {
            String planName = resolvePlanName(payment.getPatientPlan(), payment.getDoctorPlan());
            history.add(CreditHistoryEntryDTO.builder()
                    .date(payment.getCreatedAt())
                    .description("Credit applied to " + planName)
                    .amount(payment.getCreditApplied().negate())
                    .remainingBalance(payment.getRemainingCredit())
                    .eventType("APPLIED")
                    .build());
        }
        List<Payment> switchPayments = paymentRepository
                .findByUserIdAndStripeSessionIdStartingWith(userId, "CREDIT-SWITCH-");
        for (Payment payment : switchPayments) {
            if (payment.getRemainingCredit() != null && payment.getRemainingCredit().compareTo(BigDecimal.ZERO) > 0) {
                history.add(CreditHistoryEntryDTO.builder()
                        .date(payment.getCreatedAt())
                        .description("Credit earned from plan switch")
                        .amount(payment.getRemainingCredit())
                        .remainingBalance(payment.getRemainingCredit())
                        .eventType("EARNED")
                        .build());
            }
        }


        userCreditRepository.findByUserId(userId).ifPresent(credit -> {
            if (credit.getBalance() != null && credit.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                history.add(CreditHistoryEntryDTO.builder()
                        .date(credit.getCreatedAt() != null ? credit.getCreatedAt() : LocalDateTime.now())
                        .description("Credit earned")
                        .amount(credit.getBalance())
                        .remainingBalance(credit.getBalance())
                        .eventType("EARNED")
                        .build());
            }
        });

        history.sort(Comparator.comparing(CreditHistoryEntryDTO::getDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return history;
    }

    @Override
    public PriceSummaryResponseDTO calculatePrice(PriceCalculationRequestDTO request) {
        if (request == null || request.getUserId() == null || request.getPlanId() == null || request.getBillingCycle() == null) {
            throw new BadRequestException("userId, planId and billingCycle are required");
        }

        AppUser user = appUserRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        BillingCycle billingCycle;
        try {
            billingCycle = BillingCycle.valueOf(request.getBillingCycle().trim().toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid billingCycle. Use MONTHLY or YEARLY");
        }


        PlanType planType;
        if (request.getPlanType() != null && !request.getPlanType().isBlank()) {
            try {
                planType = PlanType.valueOf(request.getPlanType().trim().toUpperCase());
            } catch (Exception ex) {
                planType = PlanType.valueOf(user.getUserType().name());
            }
        } else {
            planType = PlanType.valueOf(user.getUserType().name());
        }

        PatientPlan patientPlan = null;
        DoctorPlan doctorPlan = null;
        String planName;
        BigDecimal basePrice;

        if (planType == PlanType.PATIENT) {
            patientPlan = patientPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Patient plan not found"));
            planName = patientPlan.getName();
            basePrice = billingCycle == BillingCycle.YEARLY ? patientPlan.getPriceYearly() : patientPlan.getPriceMonthly();
        } else {
            doctorPlan = doctorPlanRepository.findById(request.getPlanId())
                    .orElseThrow(() -> new RuntimeException("Doctor plan not found"));
            planName = doctorPlan.getName();
            basePrice = billingCycle == BillingCycle.YEARLY ? doctorPlan.getPriceYearly() : doctorPlan.getPriceMonthly();
        }


        BigDecimal studentDiscount = BigDecimal.ZERO;
        BigDecimal promoDiscount = BigDecimal.ZERO;
        BigDecimal creditApplied = BigDecimal.ZERO;
        BigDecimal runningPrice = basePrice;

        // Cannot combine student discount with promo code
        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()
                && studentVerificationService.isApproved(request.getUserId())) {
            throw new BadRequestException("Student discount cannot be combined with a promo code.");
        }

        // 1) Student discount
        if (planType == PlanType.PATIENT
                && "PREMIUM".equalsIgnoreCase(planName)
                && studentVerificationService.isApproved(request.getUserId())) {
            BigDecimal discounted = runningPrice.multiply(BigDecimal.valueOf(0.9)); // 10% off
            studentDiscount = runningPrice.subtract(discounted);
            runningPrice = discounted;
        }

        // 2) Promo code
        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
            PromoCodeValidationResponseDTO promoValidation = promoCodeService.validatePromoCode(
                    PromoCodeValidationDTO.builder()
                            .code(request.getPromoCode().trim())
                            .userId(request.getUserId())
                            .planType(planType.name())
                            .planName(planName)
                            .billingCycle(billingCycle.name())
                            .planPrice(runningPrice)
                            .build()
            );
            if (!Boolean.TRUE.equals(promoValidation.getValid())) {
                throw new BadRequestException(promoValidation.getMessage() != null
                        ? promoValidation.getMessage()
                        : "Invalid promo code");
            }
            promoDiscount = promoValidation.getDiscountAmount() != null
                    ? promoValidation.getDiscountAmount()
                    : BigDecimal.ZERO;
            runningPrice = promoValidation.getFinalPrice() != null
                    ? promoValidation.getFinalPrice()
                    : runningPrice;
        }

        // 3) Credit
        UserCredit existingCredit = userCreditRepository
                .findByUserIdAndExpiresAtGreaterThanEqual(request.getUserId(), LocalDate.now())
                .orElse(null);
        BigDecimal availableCredit = existingCredit != null && existingCredit.getBalance() != null
                ? existingCredit.getBalance().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        creditApplied = availableCredit.min(runningPrice).max(BigDecimal.ZERO);
        runningPrice = runningPrice.subtract(creditApplied).max(BigDecimal.ZERO);

        return PriceSummaryResponseDTO.builder()
                .baseAmount(basePrice.max(BigDecimal.ZERO))
                .studentDiscountAmount(studentDiscount.max(BigDecimal.ZERO))
                .promoDiscountAmount(promoDiscount.max(BigDecimal.ZERO))
                .creditUsed(creditApplied.max(BigDecimal.ZERO))
                .finalAmount(runningPrice.max(BigDecimal.ZERO))
                .build();
    }

    @Override
    @Transactional
    public CreateCheckoutResponseDTO upgradeDowngrade(Long userId, UpgradeDowngradeRequestDTO request) {
        if (request == null || request.getNewPlanId() == null || request.getNewBillingCycle() == null) {
            throw new RuntimeException("newPlanId and newBillingCycle are required");
        }

        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription current = subscriptionRepository
                .findByUser_IdAndStatus(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("No active subscription found"));

        BillingCycle newBillingCycle;
        try {
            newBillingCycle = BillingCycle.valueOf(request.getNewBillingCycle().trim().toUpperCase());
        } catch (Exception ex) {
            throw new RuntimeException("Invalid newBillingCycle. Use MONTHLY or YEARLY");
        }

        PatientPlan targetPatientPlan = null;
        DoctorPlan targetDoctorPlan = null;

        if (current.getPlanType() == PlanType.PATIENT) {
            targetPatientPlan = patientPlanRepository.findById(request.getNewPlanId())
                    .orElseThrow(() -> new RuntimeException("Patient plan not found"));
            if (!Boolean.TRUE.equals(targetPatientPlan.getIsActive())) {
                throw new RuntimeException("Target patient plan is inactive");
            }
        } else {
            targetDoctorPlan = doctorPlanRepository.findById(request.getNewPlanId())
                    .orElseThrow(() -> new RuntimeException("Doctor plan not found"));
            if (!Boolean.TRUE.equals(targetDoctorPlan.getIsActive())) {
                throw new RuntimeException("Target doctor plan is inactive");
            }
        }

        boolean samePlan = (current.getPlanType() == PlanType.PATIENT && current.getPatientPlan() != null
                && current.getPatientPlan().getId().equals(request.getNewPlanId()))
                || (current.getPlanType() == PlanType.DOCTOR && current.getDoctorPlan() != null
                && current.getDoctorPlan().getId().equals(request.getNewPlanId()));

        if (samePlan && current.getBillingCycle() == newBillingCycle) {
            throw new RuntimeException("Current plan and billing cycle are already active");
        }

        BigDecimal newPlanPrice = resolvePlanPrice(targetPatientPlan, targetDoctorPlan, newBillingCycle);
        BigDecimal basePrice = newPlanPrice;
        BigDecimal studentDiscountAmount = BigDecimal.ZERO;
        BigDecimal promoDiscountAmount = BigDecimal.ZERO;
        String appliedPromoCode = null;

        // Cannot combine student discount with promo code
        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()
                && studentVerificationService.isApproved(userId)) {
            throw new BadRequestException("Student discount cannot be combined with a promo code.");
        }

        if (isStudentDiscountEligibleForSwitch(userId, current, targetPatientPlan, targetDoctorPlan)) {
            BigDecimal discountedPrice = newPlanPrice.multiply(BigDecimal.valueOf(0.9));
            studentDiscountAmount = newPlanPrice.subtract(discountedPrice);
            newPlanPrice = discountedPrice;
        }

        if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
            String switchPlanType = current.getPlanType().name();
            String switchPlanName;
            if (current.getPlanType() == PlanType.PATIENT) {
                if (targetPatientPlan == null) {
                    throw new RuntimeException("Target patient plan not found");
                }
                switchPlanName = targetPatientPlan.getName();
            } else {
                if (targetDoctorPlan == null) {
                    throw new RuntimeException("Target doctor plan not found");
                }
                switchPlanName = targetDoctorPlan.getName();
            }
            PromoCodeValidationResponseDTO validation = promoCodeService.validatePromoCode(
                    PromoCodeValidationDTO.builder()
                            .code(request.getPromoCode().trim())
                            .userId(userId)
                            .planType(switchPlanType)
                            .planName(switchPlanName)
                            .billingCycle(newBillingCycle.name())
                            .planPrice(newPlanPrice)
                            .build()
            );

            if (!Boolean.TRUE.equals(validation.getValid())) {
                throw new BadRequestException(validation.getMessage() != null
                        ? validation.getMessage()
                        : "Invalid promo code");
            }

            appliedPromoCode = request.getPromoCode().trim().toUpperCase();
            promoDiscountAmount = validation.getDiscountAmount() != null
                    ? validation.getDiscountAmount()
                    : BigDecimal.ZERO;
            newPlanPrice = validation.getFinalPrice() != null
                    ? validation.getFinalPrice()
                    : newPlanPrice;
        }

        BigDecimal unusedValue = calculateUnusedValue(current);
        UserCredit existingCredit = userCreditRepository
                .findByUserIdAndExpiresAtGreaterThanEqual(userId, LocalDate.now())
                .orElse(null);
        BigDecimal existingStoredCredit = existingCredit != null && existingCredit.getBalance() != null
                ? existingCredit.getBalance().max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        BigDecimal totalAvailableCredit = unusedValue.add(existingStoredCredit);

        BigDecimal amountDue = newPlanPrice.subtract(totalAvailableCredit);
        BigDecimal creditApplied = BigDecimal.ZERO;
        BigDecimal leftoverCredit = BigDecimal.ZERO;
        BigDecimal reservedUserCredit = BigDecimal.ZERO;
        // leftoverStoredCredit tracks how much of the user's stored credit remains after this operation
        BigDecimal leftoverStoredCredit = BigDecimal.ZERO;

        if (amountDue.compareTo(BigDecimal.ZERO) > 0) {
            // New plan costs more than available credit — user must pay the difference via Stripe.
            // All available credit (unused value + stored) is consumed.
            creditApplied = totalAvailableCredit.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            amountDue = amountDue.setScale(2, RoundingMode.HALF_UP);
            reservedUserCredit = existingStoredCredit.setScale(2, RoundingMode.HALF_UP);
            // stored credit fully consumed
            leftoverStoredCredit = BigDecimal.ZERO;
        } else {
            // Credit fully covers the new plan — no Stripe checkout needed.
            creditApplied = newPlanPrice.setScale(2, RoundingMode.HALF_UP);
            leftoverCredit = amountDue.abs().setScale(2, RoundingMode.HALF_UP);
            amountDue = BigDecimal.ZERO;

            // Calculate how much stored credit remains.
            // The leftover comes first from the unused value portion, then from stored credit.
            // storedCreditUsed = max(0, newPlanPrice - unusedValue) — but capped at existingStoredCredit
            BigDecimal storedCreditUsed = newPlanPrice.subtract(unusedValue).max(BigDecimal.ZERO).min(existingStoredCredit);
            leftoverStoredCredit = existingStoredCredit.subtract(storedCreditUsed).setScale(2, RoundingMode.HALF_UP);
        }

        if (amountDue.compareTo(BigDecimal.ZERO) == 0) {
            current.setStatus(SubscriptionStatus.CANCELLED);
            current.setCancelledAt(LocalDateTime.now());
            current.setAutoRenew(false);
            subscriptionRepository.save(current);

            Subscription replacement = Subscription.builder()
                    .user(user)
                    .status(SubscriptionStatus.ACTIVE)
                    .billingCycle(newBillingCycle)
                    .paymentProvider(PaymentProvider.STRIPE)
                    .planType(current.getPlanType())
                    .patientPlan(targetPatientPlan)
                    .doctorPlan(targetDoctorPlan)
                    .startDate(LocalDate.now())
                    .endDate(newBillingCycle == BillingCycle.YEARLY
                            ? LocalDate.now().plusYears(1)
                            : LocalDate.now().plusMonths(1))
                    .autoRenew(true)
                    .lastPaymentAt(LocalDateTime.now())
                    .amountPaid(newPlanPrice)  // Store TND plan price so future proration works
                    .build();

            subscriptionRepository.save(replacement);

// ADD THIS — create payment record and trigger invoice
            Payment creditPayment = Payment.builder()
                    .user(user)
                    .stripeSessionId("CREDIT-SWITCH-" + System.currentTimeMillis())
                    .amount(BigDecimal.ZERO)
                    .currency("TND")
                    .billingCycle(newBillingCycle)
                    .promoCode(appliedPromoCode)
                    .promoDiscountAmount(promoDiscountAmount)
                    .studentDiscountAmount(studentDiscountAmount)
                    .creditApplied(creditApplied)
                    .remainingCredit(leftoverCredit)
                    .reservedUserCredit(BigDecimal.ZERO)
                    .originalPriceTnd(basePrice)
                    .status(PaymentStatus.SUCCESS)
                    .subscription(replacement)
                    .build();
            creditPayment.setPatientPlan(targetPatientPlan);
            creditPayment.setDoctorPlan(targetDoctorPlan);
            Payment savedCreditPayment = paymentRepository.save(creditPayment);

            Long newSubId = replacement.getId();
            Long creditPaymentId = savedCreditPayment.getId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    invoiceAndEmailService.generateInvoiceAndSendEmail(newSubId, creditPaymentId);
                }
            });

            // Persist only the remaining stored credit (unused value was consumed by the switch).
            setUserCreditBalance(userId, leftoverCredit);

            return CreateCheckoutResponseDTO.builder()
                    .sessionId("CREDIT_APPLIED_NO_CHECKOUT")
                    .url(null)
                    .message("Subscription switched successfully using available credit")
                    .basePrice(basePrice)
                    .studentDiscount(studentDiscountAmount)
                    .promoDiscount(promoDiscountAmount)
                    .creditApplied(creditApplied)
                    .finalAmount(BigDecimal.ZERO)
                    .build();
        }

        String planName = current.getPlanType() == PlanType.PATIENT
                ? targetPatientPlan.getName()
                : targetDoctorPlan.getName();

        String checkoutCurrency = stripeCheckoutCurrency == null
                ? "usd"
                : stripeCheckoutCurrency.trim().toLowerCase();

        BigDecimal chargeAmountInCheckoutCurrency = "tnd".equals(checkoutCurrency)
                ? amountDue
                : amountDue.multiply(tndToCheckoutCurrencyRate);
        long amountInCents = chargeAmountInCheckoutCurrency
                .setScale(2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        Session session;
        String chargedCurrency = checkoutCurrency;
        long chargedAmountInCents = amountInCents;

        try {
            session = Session.create(buildSessionParams(
                    user.getEmail(),
                    planName,
                    newBillingCycle.name(),
                    checkoutCurrency,
                    amountInCents
            ));
        } catch (StripeException e) {
            boolean invalidCurrency = e.getMessage() != null && e.getMessage().toLowerCase().contains("invalid currency");
            if (!invalidCurrency || "usd".equals(checkoutCurrency)) {
                throw new RuntimeException("Failed to create checkout session: " + e.getMessage());
            }

            chargedCurrency = "usd";
            BigDecimal fallbackAmount = amountDue.multiply(tndToCheckoutCurrencyRate)
                    .setScale(2, RoundingMode.HALF_UP);
            chargedAmountInCents = fallbackAmount.multiply(BigDecimal.valueOf(100)).longValue();

            try {
                session = Session.create(buildSessionParams(
                        user.getEmail(),
                        planName,
                        newBillingCycle.name(),
                        chargedCurrency,
                        chargedAmountInCents
                ));
            } catch (StripeException ex) {
                throw new RuntimeException("Failed to create checkout session: " + ex.getMessage());
            }
        }

        // Consume stored credit now so it cannot be spent twice while checkout is pending.
        // leftoverStoredCredit is 0 here (amountDue > 0 means all credit was consumed).
        if (existingStoredCredit.compareTo(BigDecimal.ZERO) > 0) {
            setUserCreditBalance(userId, leftoverStoredCredit);
        }

        Payment payment = Payment.builder()
                .user(user)
                .stripeSessionId(session.getId())
                .amount(new BigDecimal(chargedAmountInCents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP))
                .currency(chargedCurrency.toUpperCase())
                .status(PaymentStatus.PENDING)
                .billingCycle(newBillingCycle)
                .patientPlan(targetPatientPlan)
                .doctorPlan(targetDoctorPlan)
                .creditApplied(creditApplied)
                .remainingCredit(leftoverCredit)
                .reservedUserCredit(reservedUserCredit)
                .originalPriceTnd(newPlanPrice)
                .promoCode(appliedPromoCode)
                .promoDiscountAmount(promoDiscountAmount)
                .upgradeFromSubscriptionId(current.getId())
                .build();
        paymentRepository.save(payment);

        return CreateCheckoutResponseDTO.builder()
                .sessionId(session.getId())
                .url(session.getUrl())
                .message("Checkout created for plan switch")
                .basePrice(basePrice)
                .studentDiscount(studentDiscountAmount)
                .promoDiscount(promoDiscountAmount)
                .creditApplied(creditApplied)
                .finalAmount(amountDue.max(BigDecimal.ZERO))
                .build();
    }

    private BigDecimal resolvePlanPrice(PatientPlan patientPlan, DoctorPlan doctorPlan, BillingCycle billingCycle) {
        if (patientPlan != null) {
            return billingCycle == BillingCycle.YEARLY
                    ? patientPlan.getPriceYearly()
                    : patientPlan.getPriceMonthly();
        }
        return billingCycle == BillingCycle.YEARLY
                ? doctorPlan.getPriceYearly()
                : doctorPlan.getPriceMonthly();
    }

    private BigDecimal calculateUnusedValue(Subscription subscription) {
        if (subscription.getAmountPaid() == null || subscription.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        long totalDays = ChronoUnit.DAYS.between(subscription.getStartDate(), subscription.getEndDate()) + 1;
        if (totalDays <= 0) {
            return BigDecimal.ZERO;
        }

        long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), subscription.getEndDate()) + 1;
        if (daysRemaining <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal dailyRate = subscription.getAmountPaid()
                .divide(BigDecimal.valueOf(totalDays), 6, RoundingMode.HALF_UP);

        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining)).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal estimateCreditFromCancellation(Subscription subscription) {
        if (subscription == null || subscription.getCancelledAt() == null) {
            return BigDecimal.ZERO;
        }
        if (subscription.getAmountPaid() == null || subscription.getAmountPaid().compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (subscription.getStartDate() == null || subscription.getEndDate() == null) {
            return BigDecimal.ZERO;
        }

        long totalDays = ChronoUnit.DAYS.between(subscription.getStartDate(), subscription.getEndDate()) + 1;
        if (totalDays <= 0) {
            return BigDecimal.ZERO;
        }

        LocalDate cancellationDate = subscription.getCancelledAt().toLocalDate();
        long daysRemaining = ChronoUnit.DAYS.between(cancellationDate, subscription.getEndDate()) + 1;
        if (daysRemaining <= 0) {
            return BigDecimal.ZERO;
        }
        if (daysRemaining > totalDays) {
            daysRemaining = totalDays;
        }

        BigDecimal dailyRate = subscription.getAmountPaid()
                .divide(BigDecimal.valueOf(totalDays), 6, RoundingMode.HALF_UP);
        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining)).setScale(2, RoundingMode.HALF_UP);
    }

    private void setUserCreditBalance(Long userId, BigDecimal balance) {
        BigDecimal normalizedBalance = balance == null
                ? BigDecimal.ZERO
                : balance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        UserCredit userCredit = userCreditRepository.findByUserId(userId).orElse(null);

        if (userCredit == null) {
            if (normalizedBalance.compareTo(BigDecimal.ZERO) == 0) {
                return;
            }

            userCredit = UserCredit.builder()
                    .userId(userId)
                    .balance(normalizedBalance)
                    .expiresAt(LocalDate.now().plusYears(1))
                    .build();
        } else {
            userCredit.setBalance(normalizedBalance);
            userCredit.setExpiresAt(LocalDate.now().plusYears(1));
        }

        userCreditRepository.save(userCredit);
    }

    private boolean isStudentDiscountEligibleForSwitch(
            Long userId,
            Subscription current,
            PatientPlan targetPatientPlan,
            DoctorPlan targetDoctorPlan
    ) {
        if (current.getPlanType() != PlanType.PATIENT || targetPatientPlan == null || targetDoctorPlan != null) {
            return false;
        }
        return "PREMIUM".equalsIgnoreCase(targetPatientPlan.getName()) && studentVerificationService.isApproved(userId);
    }

    private String resolvePlanName(PatientPlan patientPlan, DoctorPlan doctorPlan) {
        if (patientPlan != null && patientPlan.getName() != null && !patientPlan.getName().isBlank()) {
            return patientPlan.getName();
        }
        if (doctorPlan != null && doctorPlan.getName() != null && !doctorPlan.getName().isBlank()) {
            return doctorPlan.getName();
        }
        return "subscription";
    }

    private SessionCreateParams buildSessionParams(
            String customerEmail,
            String planName,
            String billingCycle,
            String currency,
            long amountInCents
    ) {
        return SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(customerEmail)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency(currency)
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(planName)
                                                                .build()
                                                )
                                                .setUnitAmount(amountInCents)
                                                .setRecurring(
                                                        SessionCreateParams.LineItem.PriceData.Recurring.builder()
                                                                .setInterval("YEARLY".equalsIgnoreCase(billingCycle)
                                                                        ? SessionCreateParams.LineItem.PriceData.Recurring.Interval.YEAR
                                                                        : SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();
    }

    // ─── BUILD RESPONSE ───────────────────────────────────────────────────────

    private SubscriptionResponse buildResponse(Subscription sub) {
        SubscriptionResponse.SubscriptionResponseBuilder builder = SubscriptionResponse.builder()
                .id(sub.getId())
                .userId(sub.getUser().getId())
                .planType(sub.getPlanType())
                .status(sub.getStatus())
                .billingCycle(sub.getBillingCycle())
                .autoRenew(sub.getAutoRenew())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .cancelledAt(sub.getCancelledAt())
                .paymentProvider(sub.getPaymentProvider())
                .paymentRef(sub.getPaymentRef())
                .paymentUrl(sub.getPaymentUrl())
                .lastPaymentAt(sub.getLastPaymentAt())
                .createdAt(sub.getCreatedAt())
                .amountPaid(sub.getAmountPaid());

        // Resolve plan price based on billing cycle
        if (sub.getPlanType() == PlanType.PATIENT && sub.getPatientPlan() != null) {
            builder.planPrice(sub.getBillingCycle() == BillingCycle.YEARLY
                    ? sub.getPatientPlan().getPriceYearly()
                    : sub.getPatientPlan().getPriceMonthly());
        } else if (sub.getPlanType() == PlanType.DOCTOR && sub.getDoctorPlan() != null) {
            builder.planPrice(sub.getBillingCycle() == BillingCycle.YEARLY
                    ? sub.getDoctorPlan().getPriceYearly()
                    : sub.getDoctorPlan().getPriceMonthly());
        }

            invoiceRepository.findTopBySubscription_IdOrderByCreatedAtDesc(sub.getId())
                .map(Invoice::getId)
                .ifPresent(builder::invoiceId);

        if (sub.getPlanType() == PlanType.PATIENT && sub.getPatientPlan() != null) {
            PatientPlan plan = sub.getPatientPlan();
            builder.planName(plan.getName())
                    .hasAI(plan.getHasAI())
                    .hasForum(plan.getHasForum())
                    .hasMedicationReminder(plan.getHasMedicationReminder())
                    .hasDocumentUpload(plan.getHasDocumentUpload())
                    .hasLabResults(plan.getHasLabResults())
                    .hasSelfTestReadings(plan.getHasSelfTestReadings())
                    .hasHealthEvents(plan.getHasHealthEvents())
                    .maxAppointmentsPerMonth(plan.getMaxAppointmentsPerMonth());
        } else if (sub.getPlanType() == PlanType.DOCTOR && sub.getDoctorPlan() != null) {
            DoctorPlan plan = sub.getDoctorPlan();
            builder.planName(plan.getName())
                    .hasAI(plan.getHasAI())
                    .hasCalendarSync(plan.getHasCalendarSync())
                    .hasSearchVisibility(plan.getHasSearchVisibility())
                    .hasBasicAnalytics(plan.getHasBasicAnalytics())
                    .hasAdvancedAnalytics(plan.getHasAdvancedAnalytics())
                    .hasForumBadge(plan.getHasForumBadge())
                    .hasConsultationPrerequisites(plan.getHasConsultationPrerequisites())
                    .maxConsultationsPerMonth(plan.getMaxConsultationsPerMonth());
        }

        return builder.build();
    }


}