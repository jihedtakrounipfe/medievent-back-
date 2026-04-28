package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CreateCheckoutRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CreateCheckoutResponseDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeValidationDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeValidationResponseDTO;
import skylinkers.tn.mediconnectbackend.entities.*;
import skylinkers.tn.mediconnectbackend.entities.enums.*;
import skylinkers.tn.mediconnectbackend.exception.SubscriptionException.BadRequestException;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.*;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
// UUID no longer used for user/plan IDs

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AppUserRepository appUserRepository;
    private final PatientPlanRepository patientPlanRepository;
    private final DoctorPlanRepository doctorPlanRepository;
    private final UserCreditRepository userCreditRepository;
    private final SubscriptionEmailService subscriptionEmailService;
    private final InvoiceAndEmailService invoiceAndEmailService;
    private final StudentVerificationRepository studentVerificationRepository;
    private final PromoCodeService promoCodeService;
    private final StudentVerificationService studentVerificationService;

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

        @Value("${stripe.checkout.currency:usd}")
        private String stripeCheckoutCurrency;

        @Value("${stripe.checkout.tnd-to-currency-rate:0.32}")
        private BigDecimal tndToCheckoutCurrencyRate;

    @Override
    @Transactional
    public CreateCheckoutResponseDTO createCheckoutSession(CreateCheckoutRequestDTO request) {
        try {
            AppUser user = appUserRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isPlanChange = request.getCurrentSubscriptionId() != null || Boolean.TRUE.equals(request.getIsPlanChange());
            if (!isPlanChange && subscriptionRepository.existsByUserAndStatus(user, SubscriptionStatus.ACTIVE)) {
                throw new RuntimeException("User already has an active subscription. Cancel it first.");
            }

            String planName;
            long amountInCents;
            PatientPlan patientPlan = null;
            DoctorPlan doctorPlan = null;
            BigDecimal price;
            String planType;
            String appliedPromoCode = null;
            BigDecimal promoDiscountAmount = BigDecimal.ZERO;
            BigDecimal studentDiscountAmount = BigDecimal.ZERO;
            BigDecimal originalPriceTnd;  // Track TND price before credit subtraction

            if (request.getPatientPlanId() != null) {
                patientPlan = patientPlanRepository.findById(request.getPatientPlanId())
                        .orElseThrow(() -> new RuntimeException("Patient plan not found"));
                planName = patientPlan.getName();
                planType = "PATIENT";
                price = request.getBillingCycle().equals("YEARLY") ?
                        patientPlan.getPriceYearly() :
                        patientPlan.getPriceMonthly();
            } else if (request.getDoctorPlanId() != null) {
                doctorPlan = doctorPlanRepository.findById(request.getDoctorPlanId())
                        .orElseThrow(() -> new RuntimeException("Doctor plan not found"));
                planName = doctorPlan.getName();
                planType = "DOCTOR";
                price = request.getBillingCycle().equals("YEARLY") ?
                        doctorPlan.getPriceYearly() :
                        doctorPlan.getPriceMonthly();
            } else {
                throw new RuntimeException("Either patientPlanId or doctorPlanId must be provided");
            }

            // Capture the base TND price before any discount/credit modifications
            originalPriceTnd = price;

            // Cannot combine student discount with promo code
            if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()
                    && studentVerificationService.isApproved(request.getUserId())) {
                throw new BadRequestException("Student discount cannot be combined with a promo code.");
            }

            // 1) Student discount (Premium patient plans only)
            if (isStudentDiscountEligible(request.getUserId(), planType, planName)) {
                BigDecimal discountedPrice = price.multiply(BigDecimal.valueOf(0.9)); // 10% off
                studentDiscountAmount = price.subtract(discountedPrice);
                price = discountedPrice;
            }

                        log.info("Checkout base price resolved for user {}: planType={}, planName={}, billingCycle={}, basePrice={} TND",
                                        request.getUserId(), planType, planName, request.getBillingCycle(), price);

            // 2) Promo code
            if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
                                log.info("Promo code received for checkout user {}: {}", request.getUserId(), request.getPromoCode());
                PromoCodeValidationResponseDTO validation = promoCodeService.validatePromoCode(
                        PromoCodeValidationDTO.builder()
                                .code(request.getPromoCode().trim())
                                .userId(request.getUserId())
                                .planType(planType)
                                .planName(planName)
                                .billingCycle(request.getBillingCycle())
                                .planPrice(price)
                                .build()
                );

                if (!Boolean.TRUE.equals(validation.getValid())) {
                    throw new RuntimeException(validation.getMessage() != null
                            ? validation.getMessage()
                            : "Invalid promo code");
                }

                appliedPromoCode = request.getPromoCode().trim().toUpperCase();
                promoDiscountAmount = validation.getDiscountAmount() != null
                        ? validation.getDiscountAmount()
                        : BigDecimal.ZERO;
                price = validation.getFinalPrice() != null
                        ? validation.getFinalPrice()
                        : price;

                log.info("Promo applied for user {}: code={}, discount={} TND, finalPrice={} TND",
                        request.getUserId(), appliedPromoCode, promoDiscountAmount, price);
            } else {
                log.info("No promo code received for checkout user {}. Proceeding without discount.", request.getUserId());
            }

                        // 3) Credit
                        var userCreditOpt = userCreditRepository.findByUserIdAndExpiresAtGreaterThanEqual(
                                request.getUserId(),
                                LocalDate.now()
                        );

                        BigDecimal creditBalance = BigDecimal.ZERO;
                        BigDecimal creditApplied = BigDecimal.ZERO;
                        BigDecimal remainingCredit = BigDecimal.ZERO;
                        BigDecimal reservedUserCredit = BigDecimal.ZERO;

                        if (userCreditOpt.isPresent()) {
                                creditBalance = userCreditOpt.get().getBalance();
                                log.info("User {} has available credit: {} TND, plan price after discounts: {} TND",
                                        request.getUserId(), creditBalance, price);

                                // Case A: Credit fully covers the plan price
                                if (creditBalance.compareTo(price) >= 0) {
                                        log.info("Credit fully covers plan price for user {}. Creating subscription without Stripe.",
                                                request.getUserId());

                                        creditApplied = price;
                                        remainingCredit = creditBalance.subtract(price);

                                        // Update credit
                                        UserCredit credit = userCreditOpt.get();
                                        credit.setBalance(remainingCredit);
                                        userCreditRepository.save(credit);

                                        // Create subscription directly
                                        LocalDate endDate = "YEARLY".equalsIgnoreCase(request.getBillingCycle())
                                                ? LocalDate.now().plusYears(1)
                                                : LocalDate.now().plusMonths(1);

                                        Subscription subscription = Subscription.builder()
                                                .user(user)
                                                .status(SubscriptionStatus.ACTIVE)
                                                .billingCycle("YEARLY".equalsIgnoreCase(request.getBillingCycle()) ? BillingCycle.YEARLY : BillingCycle.MONTHLY)
                                                .paymentProvider(PaymentProvider.STRIPE)
                                                .planType(patientPlan != null ? PlanType.PATIENT : PlanType.DOCTOR)
                                                .patientPlan(patientPlan)
                                                .doctorPlan(doctorPlan)
                                                .startDate(LocalDate.now())
                                                .endDate(endDate)
                                                .autoRenew(true)
                                                .lastPaymentAt(LocalDateTime.now())
                                                .amountPaid(price)  // Store TND plan price so future proration works
                                                .build();

                                        subscriptionRepository.save(subscription);
                                        log.info("✅ Subscription created directly using credit for user {}", request.getUserId());

                                    // ADD THIS — generate invoice same as Stripe webhook does
                                    Long subscriptionId = subscription.getId();
                                    // Create a payment record for the invoice
                                    Payment creditPayment = Payment.builder()
                                            .user(user)
                                            .stripeSessionId("CREDIT-" + System.currentTimeMillis())
                                            .amount(BigDecimal.ZERO)
                                            .currency("TND")
                                            .billingCycle(subscription.getBillingCycle())
                                            .promoCode(appliedPromoCode)
                                            .promoDiscountAmount(promoDiscountAmount)
                                            .studentDiscountAmount(studentDiscountAmount)
                                            .creditApplied(creditApplied)
                                            .remainingCredit(remainingCredit)
                                            .reservedUserCredit(BigDecimal.ZERO)
                                            .originalPriceTnd(originalPriceTnd)
                                            .status(PaymentStatus.SUCCESS)
                                            .subscription(subscription)
                                            .build();
                                    creditPayment.setPatientPlan(patientPlan);
                                    creditPayment.setDoctorPlan(doctorPlan);
                                    Payment savedPayment = paymentRepository.save(creditPayment);
                                    Long paymentId = savedPayment.getId();
                                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                        @Override
                                        public void afterCommit() {
                                            invoiceAndEmailService.generateInvoiceAndSendEmail(subscriptionId, paymentId);
                                        }
                                    });


                                        return CreateCheckoutResponseDTO.builder()
                                                .sessionId("CREDIT_APPLIED_NO_CHECKOUT")
                                                .url(null)
                                                .message("Subscription created successfully using available credit")
                                                .basePrice(originalPriceTnd)
                                                .studentDiscount(studentDiscountAmount)
                                                .promoDiscount(promoDiscountAmount)
                                                .creditApplied(creditApplied)
                                                .finalAmount(BigDecimal.ZERO)
                                                .build();
                                }

                                // Case B: Credit partially covers the plan price
                                if (creditBalance.compareTo(BigDecimal.ZERO) > 0) {
                                        creditApplied = creditBalance;
                                        price = price.subtract(creditBalance);
                                        remainingCredit = BigDecimal.ZERO;
                                        reservedUserCredit = creditBalance;

                                        log.info("Credit partially covers plan for user {}. Applying credit: {} TND, remaining to pay: {} TND",
                                                request.getUserId(), creditApplied, price);

                                        // Update credit to zero (will be applied on payment success)
                                        UserCredit credit = userCreditOpt.get();
                                        credit.setBalance(BigDecimal.ZERO);
                                        userCreditRepository.save(credit);
                                }
                        }

                        // Require verification only for STUDENT patient plan.
                        if (patientPlan != null && "STUDENT".equalsIgnoreCase(patientPlan.getName())) {
                                var verification = studentVerificationRepository.findByUserId(request.getUserId());
                                if (verification.isEmpty()) {
                                        throw new RuntimeException("STUDENT_VERIFICATION_REQUIRED: submit your student document first.");
                                }

                                String status = verification.get().getStatus().toString();
                                if ("PENDING".equals(status)) {
                                        throw new RuntimeException("STUDENT_VERIFICATION_PENDING: your document is under review.");
                                }
                                if (!"APPROVED".equals(status)) {
                                        throw new RuntimeException("STUDENT_VERIFICATION_REJECTED: your request was rejected.");
                                }
                        }

                        String checkoutCurrency = stripeCheckoutCurrency == null
                                        ? "usd"
                                        : stripeCheckoutCurrency.trim().toLowerCase();

                        BigDecimal chargeAmountInCheckoutCurrency;
                        if ("tnd".equals(checkoutCurrency)) {
                                chargeAmountInCheckoutCurrency = price;
                        } else {
                                // Plan prices are stored in TND; convert when Stripe currency differs.
                                chargeAmountInCheckoutCurrency = price.multiply(tndToCheckoutCurrencyRate);
                        }

                        amountInCents = chargeAmountInCheckoutCurrency.multiply(BigDecimal.valueOf(100)).longValue();

            SessionCreateParams params = buildSessionParams(
                    user.getEmail(),
                    planName,
                    request.getBillingCycle(),
                    checkoutCurrency,
                    amountInCents
            );

            BillingCycle selectedBillingCycle = "YEARLY".equalsIgnoreCase(request.getBillingCycle())
                    ? BillingCycle.YEARLY
                    : BillingCycle.MONTHLY;

            Session session;
            String chargedCurrency = checkoutCurrency;
            long chargedAmountInCents = amountInCents;

            try {
                session = Session.create(params);
            } catch (StripeException e) {
                boolean invalidCurrency = e.getMessage() != null && e.getMessage().toLowerCase().contains("invalid currency");
                if (!invalidCurrency || "usd".equals(checkoutCurrency)) {
                    throw e;
                }

                chargedCurrency = "usd";
                BigDecimal fallbackAmount = price.multiply(tndToCheckoutCurrencyRate);
                chargedAmountInCents = fallbackAmount.multiply(BigDecimal.valueOf(100)).longValue();

                SessionCreateParams fallbackParams = buildSessionParams(
                        user.getEmail(),
                        planName,
                        request.getBillingCycle(),
                        chargedCurrency,
                        chargedAmountInCents
                );

                log.warn("Stripe rejected currency '{}'. Retrying checkout in USD for user {}.", checkoutCurrency, user.getId());
                session = Session.create(fallbackParams);
            }

            // Note: price has been reduced by creditApplied at this point
            // originalPriceTnd holds the full TND price (after promo, before credit)
            Payment payment = Payment.builder()
                    .user(user)
                    .stripeSessionId(session.getId())
                    .amount(price.max(BigDecimal.ZERO))  // TND amount after all discounts and credit
                    .currency("TND")
                    .billingCycle(selectedBillingCycle)
                    .promoCode(appliedPromoCode)
                    .promoDiscountAmount(promoDiscountAmount)
                    .creditApplied(creditApplied)
                    .remainingCredit(remainingCredit)
                    .reservedUserCredit(reservedUserCredit)
                    .originalPriceTnd(originalPriceTnd)
                    .status(PaymentStatus.PENDING)
                    .promoDiscountAmount(promoDiscountAmount)
                    .studentDiscountAmount(studentDiscountAmount) // ← add this
                    .creditApplied(creditApplied)
                    .build();

            payment.setPatientPlan(patientPlan);
            payment.setDoctorPlan(doctorPlan);
            payment.setUpgradeFromSubscriptionId(request.getCurrentSubscriptionId());
            paymentRepository.save(payment);

            log.info("Stripe checkout session created: {}", session.getId());

            return CreateCheckoutResponseDTO.builder()
                    .sessionId(session.getId())
                    .url(session.getUrl())
                    .basePrice(originalPriceTnd)
                    .studentDiscount(studentDiscountAmount)
                    .promoDiscount(promoDiscountAmount)
                    .creditApplied(creditApplied)
                    .finalAmount(price.max(BigDecimal.ZERO))
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage());
            throw new RuntimeException("Failed to create checkout session: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void handlePaymentSuccess(String stripeSessionId) {
                Payment payment = paymentRepository.findByStripeSessionIdForUpdate(stripeSessionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for session: " + stripeSessionId));

                // Keep local payment amount in sync with Stripe's final charged amount.
                try {
                        Session stripeSession = Session.retrieve(stripeSessionId);
                        if (stripeSession.getAmountTotal() != null) {
                                BigDecimal charged = BigDecimal.valueOf(stripeSession.getAmountTotal())
                                                .divide(BigDecimal.valueOf(100));
                                payment.setAmount(charged);
                        }
                        if (stripeSession.getCurrency() != null && !stripeSession.getCurrency().isBlank()) {
                                payment.setCurrency(stripeSession.getCurrency().toUpperCase());
                        }
                } catch (StripeException ex) {
                        log.warn("Could not sync payment amount from Stripe session {}: {}", stripeSessionId, ex.getMessage());
                }

        // ✅ IDEMPOTENCY CHECK - if already processed, return immediately
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already processed for session: {}", stripeSessionId);
            return;
        }

                boolean upgradeFlow = payment.getUpgradeFromSubscriptionId() != null;

                if (upgradeFlow) {
                        subscriptionRepository.findById(payment.getUpgradeFromSubscriptionId()).ifPresent(oldSubscription -> {
                                if (oldSubscription.getStatus() == SubscriptionStatus.ACTIVE) {
                                        oldSubscription.setStatus(SubscriptionStatus.CANCELLED);
                                        oldSubscription.setCancelledAt(LocalDateTime.now());
                                        oldSubscription.setAutoRenew(false);
                                        subscriptionRepository.save(oldSubscription);
                                }
                        });
                }

        var existingActive = subscriptionRepository
                .findByUser_IdAndStatus(payment.getUser().getId(), SubscriptionStatus.ACTIVE);
                if (!upgradeFlow && existingActive.isPresent()) {
            payment.setSubscription(existingActive.get());
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);
            log.warn("Active subscription already exists for user {}. Linked payment {} to existing subscription {}.",
                    payment.getUser().getId(), payment.getId(), existingActive.get().getId());
            return;
        }

        Subscription subscription = Subscription.builder()
                .user(payment.getUser())
                .status(SubscriptionStatus.ACTIVE)
                .billingCycle(payment.getBillingCycle() != null ? payment.getBillingCycle() : BillingCycle.MONTHLY)
                .paymentProvider(PaymentProvider.STRIPE)
                .planType(payment.getPatientPlan() != null ? PlanType.PATIENT : PlanType.DOCTOR)
                .patientPlan(payment.getPatientPlan())
                .doctorPlan(payment.getDoctorPlan())
                .startDate(LocalDate.now())
                .endDate((payment.getBillingCycle() != null ? payment.getBillingCycle() : BillingCycle.MONTHLY) == BillingCycle.YEARLY
                        ? LocalDate.now().plusYears(1)
                        : LocalDate.now().plusMonths(1))
                .autoRenew(true)
                .lastPaymentAt(LocalDateTime.now())
                // Use TND price for amountPaid so future proration calculations are currency-consistent.
                // Falls back to Stripe amount if originalPriceTnd wasn't set (legacy payments).
                .amountPaid(payment.getOriginalPriceTnd() != null ? payment.getOriginalPriceTnd() : payment.getAmount())
                .build();

        subscriptionRepository.save(subscription);
        log.info("✅ Subscription created and saved: {}", subscription.getId());
        
        payment.setSubscription(subscription);
        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);
        log.info("✅ Payment updated: {}", payment.getId());

                // ─── CREDIT SYSTEM: Restore remaining credit if any ───────────────────────
                if (payment.getCreditApplied() != null && payment.getCreditApplied().compareTo(BigDecimal.ZERO) > 0) {
                        var userCredit = userCreditRepository.findByUserId(payment.getUser().getId()).orElse(null);
                        if (userCredit != null && payment.getRemainingCredit() != null && payment.getRemainingCredit().compareTo(BigDecimal.ZERO) > 0) {
                                userCredit.setBalance(payment.getRemainingCredit());
                                userCredit.setExpiresAt(LocalDate.now().plusYears(1));
                                userCreditRepository.save(userCredit);
                                log.info("Restored remaining credit {} for user {} after payment success",
                                        payment.getRemainingCredit(), payment.getUser().getId());
                        }

                        // Send email with credit info
                        subscriptionEmailService.sendSubscriptionWithCreditConfirmation(
                                payment.getUser().getEmail(),
                                payment.getPatientPlan() != null ? payment.getPatientPlan().getName() : payment.getDoctorPlan().getName(),
                                payment.getAmount(),
                                payment.getCreditApplied(),
                                payment.getRemainingCredit() != null ? payment.getRemainingCredit() : BigDecimal.ZERO,
                                subscription.getStartDate(),
                                subscription.getEndDate()
                        );
                }

                if (payment.getPromoCode() != null && !payment.getPromoCode().isBlank()) {
                        try {
                                promoCodeService.markPromoCodeAsUsed(payment.getPromoCode(), payment.getUser().getId(), subscription.getId());
                        } catch (Exception ex) {
                                log.error("Failed to mark promo code as used for payment {}", payment.getId(), ex);
                        }
                }

        log.info("Payment success and subscription created for session: {}", stripeSessionId);
        
                Long subscriptionId = subscription.getId();
                Long paymentId = payment.getId();

                // Run async invoice/email only after this transaction commits to avoid FK failures.
                System.out.println(">>> [DEBUG] Registering Transaction Synchronization for Invoice");
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                System.out.println(">>> [DEBUG] Transaction Committed. Triggering Async Invoice...");
                                invoiceAndEmailService.generateInvoiceAndSendEmail(subscriptionId, paymentId);
                        }
                });
    }

    @Override
    @Transactional
    public void handlePaymentSuccessByEmail(String email) {
        log.info("Looking up payment for email: {}", email);

        Payment payment = paymentRepository.findTopByUserEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("No pending payment found for email: " + email));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already processed for email: {}", email);
            return;
        }

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        log.info("Payment status updated via email lookup for: {}", email);
    }

    @Override
    @Transactional
    public void handlePaymentFailed(String stripeSessionId) {
        Payment payment = paymentRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new RuntimeException("Payment not found for session: " + stripeSessionId));

                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                        log.warn("Ignoring failed status update for already successful payment session: {}", stripeSessionId);
                        return;
                }

                if (payment.getStatus() == PaymentStatus.FAILED) {
                        log.info("Payment already marked as failed for session: {}", stripeSessionId);
                        return;
                }

                restoreReservedUserCredit(payment);

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        subscriptionEmailService.sendPaymentFailureEmail(payment.getUser().getEmail());

        log.info("Payment failed for session: {}", stripeSessionId);
    }

    @Override
    @Transactional
    public void handlePaymentFailedByEmail(String email) {
        Payment payment = paymentRepository.findTopByUserEmailOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new RuntimeException("No payment found for email: " + email));

                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                        log.warn("Ignoring failed status update for already successful payment of user {}", email);
                        return;
                }

        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Payment already marked as failed for: {}", email);
            return;
        }

                restoreReservedUserCredit(payment);

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        subscriptionEmailService.sendPaymentFailureEmail(email);
        log.info("Payment failed via email lookup for: {}", email);
    }

        private void restoreReservedUserCredit(Payment payment) {
                BigDecimal reserved = payment.getReservedUserCredit() != null
                                ? payment.getReservedUserCredit()
                                : BigDecimal.ZERO;

                if (reserved.compareTo(BigDecimal.ZERO) <= 0) {
                        return;
                }

                UserCredit userCredit = userCreditRepository.findByUserId(payment.getUser().getId()).orElse(null);
                if (userCredit == null) {
                        userCredit = UserCredit.builder()
                                        .userId(payment.getUser().getId())
                                        .balance(reserved)
                                        .expiresAt(LocalDate.now().plusYears(1))
                                        .build();
                } else {
                        userCredit.setBalance((userCredit.getBalance() == null ? BigDecimal.ZERO : userCredit.getBalance()).add(reserved));
                        userCredit.setExpiresAt(LocalDate.now().plusYears(1));
                }

                userCreditRepository.save(userCredit);
                payment.setReservedUserCredit(BigDecimal.ZERO);
                log.info("Restored reserved user credit {} for failed payment {}", reserved, payment.getId());
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
                                                                .setInterval("YEARLY".equals(billingCycle)
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

    private boolean isStudentDiscountEligible(Long userId, String planType, String planName) {
        return "PATIENT".equalsIgnoreCase(planType)
                && "PREMIUM".equalsIgnoreCase(planName)
                && studentVerificationService.isApproved(userId);
    }
}
