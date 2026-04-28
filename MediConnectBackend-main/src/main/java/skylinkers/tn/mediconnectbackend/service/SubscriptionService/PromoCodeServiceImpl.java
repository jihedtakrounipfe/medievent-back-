package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeResponseDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeValidationDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeValidationResponseDTO;
import skylinkers.tn.mediconnectbackend.entities.AppUser;
import skylinkers.tn.mediconnectbackend.entities.PromoCode;
import skylinkers.tn.mediconnectbackend.entities.PromoCodeUsage;
import skylinkers.tn.mediconnectbackend.entities.Subscription;
import skylinkers.tn.mediconnectbackend.entities.enums.*;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.*;
import skylinkers.tn.mediconnectbackend.repository.UserRepositories.AppUserRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCodeServiceImpl implements PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final AppUserRepository userRepository;
    private final PatientPlanRepository patientPlanRepository;
    private final DoctorPlanRepository doctorPlanRepository;
    private final StudentVerificationRepository studentVerificationRepository;
    private final SubscriptionEmailService subscriptionEmailService;
    private final SubscriptionRepository subscriptionRepository;

    @Override
    public PromoCodeResponseDTO createPromoCode(PromoCodeRequestDTO request, Long adminId) {
        AppUser admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin user not found"));

        final String STUDENT_PROMO_CODE = "STUDENT_PROMO";
        final String PREMIUM_PLAN = "PREMIUM";
        String normalizedCode = request.getCode().trim().toUpperCase();
        String normalizedPlanName = request.getPlanName().trim().toUpperCase();

        // Validate STUDENT_PROMO code constraints
        if (STUDENT_PROMO_CODE.equalsIgnoreCase(normalizedCode)) {
            if (!PREMIUM_PLAN.equals(normalizedPlanName)) {
                throw new RuntimeException("STUDENT_PROMO code can only be applied to PREMIUM plan");
            }
        }

        PromoCode promoCode = PromoCode.builder()
            .code(normalizedCode)
                .description(request.getDescription())
            .discountType(DiscountType.valueOf(request.getDiscountType().trim().toUpperCase()))
                .discountValue(request.getDiscountValue())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .maxUsesTotal(request.getMaxUsesTotal())
                .maxUsesPerUser(request.getMaxUsesPerUser() != null ? request.getMaxUsesPerUser() : 1)
                .currentUseCount(0)
            .planType(PromoCodePlanType.valueOf(request.getPlanType().trim().toUpperCase()))
            .planName(normalizedPlanName)
            .billingCycle(PromoCodeBillingCycle.valueOf(request.getBillingCycle().trim().toUpperCase()))
                .minPurchaseAmount(request.getMinPurchaseAmount() != null ? request.getMinPurchaseAmount() : BigDecimal.ZERO)
                .createdByAdmin(admin)
                .build();

        PromoCode saved = promoCodeRepository.save(promoCode);
        log.info("Promo code created: {} by admin {}", saved.getCode(), admin.getEmail());

        return mapToDTO(saved);
    }

    @Override
    public PromoCodeResponseDTO updatePromoCode(Long id, PromoCodeRequestDTO request) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promo code not found"));

        final String STUDENT_PROMO_CODE = "STUDENT_PROMO";
        final String PREMIUM_PLAN = "PREMIUM";
        String normalizedCode = request.getCode().trim().toUpperCase();
        String normalizedPlanName = request.getPlanName().trim().toUpperCase();

        // Validate STUDENT_PROMO code constraints
        if (STUDENT_PROMO_CODE.equalsIgnoreCase(normalizedCode)) {
            if (!PREMIUM_PLAN.equals(normalizedPlanName)) {
                throw new RuntimeException("STUDENT_PROMO code can only be applied to PREMIUM plan");
            }
        }

        promoCode.setCode(normalizedCode);
        promoCode.setDescription(request.getDescription());
        promoCode.setDiscountType(DiscountType.valueOf(request.getDiscountType().trim().toUpperCase()));
        promoCode.setDiscountValue(request.getDiscountValue());
        promoCode.setStartDate(request.getStartDate());
        promoCode.setEndDate(request.getEndDate());
        promoCode.setIsActive(request.getIsActive());
        promoCode.setMaxUsesTotal(request.getMaxUsesTotal());
        promoCode.setMaxUsesPerUser(request.getMaxUsesPerUser());
        promoCode.setPlanType(PromoCodePlanType.valueOf(request.getPlanType().trim().toUpperCase()));
        promoCode.setPlanName(normalizedPlanName);
        promoCode.setBillingCycle(PromoCodeBillingCycle.valueOf(request.getBillingCycle().trim().toUpperCase()));
        promoCode.setMinPurchaseAmount(request.getMinPurchaseAmount());

        PromoCode updated = promoCodeRepository.save(promoCode);
        log.info("Promo code updated: {}", updated.getCode());

        return mapToDTO(updated);
    }

    @Override
    @Transactional
    public void deletePromoCode(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promo code not found"));

        String promoCodeName = promoCode.getCode();

        // Find all usages of this promo code
        List<PromoCodeUsage> usages = promoCodeUsageRepository.findByPromoCode(promoCode);

        // Deactivate subscriptions that used this promo with auto-renewal enabled
        int deactivatedCount = 0;
        for (PromoCodeUsage usage : usages) {
            if (usage.getSubscription() != null) {
                Subscription subscription = usage.getSubscription();
                
                // Check if subscription is active and has auto-renewal enabled
                if (subscription.getStatus() == SubscriptionStatus.ACTIVE && 
                    Boolean.TRUE.equals(subscription.getAutoRenew())) {
                    
                    subscription.setStatus(SubscriptionStatus.CANCELLED);
                    subscription.setCancelledAt(LocalDateTime.now());
                    subscription.setAutoRenew(false);
                    subscriptionRepository.save(subscription);
                    deactivatedCount++;

                    // Send cancellation email to user
                    String userEmail = subscription.getUser().getEmail();
                    subscriptionEmailService.sendPromoDeletionNotification(userEmail, promoCodeName);
                    log.info("Deactivated auto-renewal subscription for user {} due to promo code deletion: {}", 
                            userEmail, promoCodeName);
                }
            }
        }

        // Delete the promo code
        promoCodeRepository.delete(promoCode);
        log.info("Promo code deleted: {} (deactivated {} auto-renewing subscriptions)", promoCodeName, deactivatedCount);
    }

    @Override
    public PromoCodeResponseDTO getPromoCode(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promo code not found"));

        return mapToDTO(promoCode);
    }

    @Override
    public List<PromoCodeResponseDTO> getAllPromoCodes() {
        return promoCodeRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void togglePromoCode(Long id) {
        PromoCode promoCode = promoCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Promo code not found"));

        promoCode.setIsActive(!promoCode.getIsActive());
        promoCodeRepository.save(promoCode);

        log.info("Promo code toggled: {} -> {}", promoCode.getCode(), promoCode.getIsActive());
    }

    @Override
    public PromoCodeValidationResponseDTO validatePromoCode(PromoCodeValidationDTO request) {
        if (request.getCode() == null || request.getCode().trim().isEmpty()) {
            return PromoCodeValidationResponseDTO.builder()
                .valid(false)
                .message("Promo code is required")
                .build();
        }
        if (request.getPlanType() == null || request.getPlanType().trim().isEmpty()) {
            return PromoCodeValidationResponseDTO.builder()
                .valid(false)
                .message("Plan type is required")
                .build();
        }
        if (request.getPlanName() == null || request.getPlanName().trim().isEmpty()) {
            return PromoCodeValidationResponseDTO.builder()
                .valid(false)
                .message("Plan name is required")
                .build();
        }
        if (request.getBillingCycle() == null || request.getBillingCycle().trim().isEmpty()) {
            return PromoCodeValidationResponseDTO.builder()
                .valid(false)
                .message("Billing cycle is required")
                .build();
        }
        String normalizedCode = request.getCode().trim();
        String normalizedPlanType = request.getPlanType().trim().toUpperCase();
        String normalizedPlanName = request.getPlanName().trim().toUpperCase();
        String normalizedBillingCycle = request.getBillingCycle().trim().toUpperCase();

        BigDecimal effectivePlanPrice = request.getPlanPrice();
        if (effectivePlanPrice == null) {
            effectivePlanPrice = resolvePlanPrice(normalizedPlanType, normalizedPlanName, normalizedBillingCycle);
            if (effectivePlanPrice == null) {
                return PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("Plan price is required")
                    .build();
            }
        }

        PromoCode promoCode = promoCodeRepository.findByCodeIgnoreCase(normalizedCode)
            .orElse(null);
        if (promoCode == null) {
            return PromoCodeValidationResponseDTO.builder()
                .valid(false)
                .message("Invalid promo code")
                .build();
        }

        AppUser user = null;
        if (request.getUserId() != null) {
            user = userRepository.findById(request.getUserId())
                    .orElse(null);
            if (user == null) {
                return PromoCodeValidationResponseDTO.builder()
                        .valid(false)
                        .message("User not found")
                        .build();
            }
        }

        // Check if active
        if (!promoCode.getIsActive()) {
            return PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("This promo code is no longer active")
                    .build();
        }

        // Check date validity
        LocalDate today = LocalDate.now();
        if (today.isBefore(promoCode.getStartDate()) || today.isAfter(promoCode.getEndDate())) {
            return PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("This promo code is expired or not yet valid")
                    .build();
        }

        // Check max uses total
        if (promoCode.getCurrentUseCount() >= promoCode.getMaxUsesTotal()) {
            return PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("This promo code has reached its usage limit")
                    .build();
        }

        // Check one-time per user
        if (user != null) {
            long userUseCount = promoCodeUsageRepository.countByPromoCodeAndUser(promoCode, user);
            int maxPerUser = promoCode.getMaxUsesPerUser() != null ? promoCode.getMaxUsesPerUser() : 1;
            if (userUseCount >= maxPerUser) {
                return PromoCodeValidationResponseDTO.builder()
                        .valid(false)
                        .message("You have already used this promo code")
                        .build();
            }

            // Check student promo exclusivity rules
            PromoCodeValidationResponseDTO studentCheck = studentPromoExclusivityCheck(user, promoCode, normalizedPlanName);
            if (studentCheck != null) {
                return studentCheck;
            }
        }

        // Check plan type compatibility
        if (!planTypeMatches(promoCode.getPlanType(), normalizedPlanType)) {
            return PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("This promo code doesn't apply to your plan type")
                    .build();
        }

        // Check plan name compatibility
        if (!planNameMatches(promoCode.getPlanName(), normalizedPlanName)) {
            return PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("This promo code doesn't apply to your plan")
                    .build();
        }

        // Check billing cycle compatibility
        if (!billingCycleMatches(promoCode.getBillingCycle(), normalizedBillingCycle)) {
            return PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("This promo code doesn't apply to your billing cycle")
                    .build();
        }

        // Check minimum purchase amount
        if (effectivePlanPrice.compareTo(promoCode.getMinPurchaseAmount()) < 0) {
            return PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("Plan price must be at least " + promoCode.getMinPurchaseAmount() + " TND to use this code")
                    .build();
        }

        // Calculate discount
        BigDecimal discountAmount;
        if (promoCode.getDiscountType() == DiscountType.PERCENTAGE) {
            discountAmount = effectivePlanPrice
                    .multiply(promoCode.getDiscountValue())
                    .divide(new BigDecimal(100));
        } else {
            discountAmount = promoCode.getDiscountValue();
        }

        BigDecimal finalPrice = effectivePlanPrice.subtract(discountAmount);
        if (finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            finalPrice = BigDecimal.ZERO;
        }

        return PromoCodeValidationResponseDTO.builder()
                .valid(true)
                .discountAmount(discountAmount)
                .finalPrice(finalPrice)
                .discountType(promoCode.getDiscountType().toString())
                .discountValue(promoCode.getDiscountValue())
                .build();
    }

    private BigDecimal resolvePlanPrice(String planType, String planName, String billingCycle) {
        boolean yearly = "YEARLY".equalsIgnoreCase(billingCycle);

        if ("PATIENT".equalsIgnoreCase(planType)) {
            return patientPlanRepository.findByNameIgnoreCase(planName)
                    .map(plan -> yearly ? plan.getPriceYearly() : plan.getPriceMonthly())
                    .orElse(null);
        }

        if ("DOCTOR".equalsIgnoreCase(planType)) {
            return doctorPlanRepository.findByNameIgnoreCase(planName)
                    .map(plan -> yearly ? plan.getPriceYearly() : plan.getPriceMonthly())
                    .orElse(null);
        }

        return null;
    }

    @Override
    public void markPromoCodeAsUsed(String code, Long userId, Long subscriptionId) {
        PromoCode promoCode = promoCodeRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new RuntimeException("Promo code not found"));

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));

        // Idempotency: repeated callbacks for the same user+promo should not fail the whole payment flow.
        var existingUsage = promoCodeUsageRepository.findByPromoCodeAndUser(promoCode, user);
        if (existingUsage.isPresent()) {
            PromoCodeUsage usage = existingUsage.get();
            if (usage.getSubscription() == null) {
                usage.setSubscription(subscription);
                promoCodeUsageRepository.save(usage);
            }
            log.info("Promo code {} was already marked as used by user {}, skipping duplicate usage insert", code, user.getEmail());
            return;
        }

        long userUseCount = promoCodeUsageRepository.countByPromoCodeAndUser(promoCode, user);
        int maxPerUser = promoCode.getMaxUsesPerUser() != null ? promoCode.getMaxUsesPerUser() : 1;
        if (userUseCount >= maxPerUser) {
            throw new RuntimeException("You have already used this promo code");
        }

        // Create usage record
        PromoCodeUsage usage = PromoCodeUsage.builder()
                .promoCode(promoCode)
                .user(user)
                .subscription(subscription)
                .build();

        promoCodeUsageRepository.save(usage);

        // Increment usage count
        promoCode.setCurrentUseCount(promoCode.getCurrentUseCount() + 1);
        promoCodeRepository.save(promoCode);

        log.info("Promo code {} marked as used by user {}", code, user.getEmail());
    }

    private PromoCodeResponseDTO mapToDTO(PromoCode promoCode) {
        return PromoCodeResponseDTO.builder()
                .id(promoCode.getId())
                .code(promoCode.getCode())
                .description(promoCode.getDescription())
                .discountType(promoCode.getDiscountType().toString())
                .discountValue(promoCode.getDiscountValue())
                .startDate(promoCode.getStartDate())
                .endDate(promoCode.getEndDate())
                .isActive(promoCode.getIsActive())
                .maxUsesTotal(promoCode.getMaxUsesTotal())
                .maxUsesPerUser(promoCode.getMaxUsesPerUser())
                .currentUseCount(promoCode.getCurrentUseCount())
                .planType(promoCode.getPlanType().toString())
                .planName(promoCode.getPlanName())
                .billingCycle(promoCode.getBillingCycle().toString())
                .minPurchaseAmount(promoCode.getMinPurchaseAmount())
                .createdByAdmin(promoCode.getCreatedByAdmin().getEmail())
                .createdAt(promoCode.getCreatedAt())
                .updatedAt(promoCode.getUpdatedAt())
                .build();
    }

    private boolean planTypeMatches(PromoCodePlanType promoType, String requestType) {
        if (promoType == PromoCodePlanType.BOTH) return true;
        return promoType.toString().equalsIgnoreCase(requestType);
    }

    private boolean planNameMatches(String promoName, String requestName) {
        if (promoName.equalsIgnoreCase("ALL")) return true;
        return promoName.equalsIgnoreCase(requestName);
    }

    private boolean billingCycleMatches(PromoCodeBillingCycle promoCycle, String requestCycle) {
        if (promoCycle == PromoCodeBillingCycle.BOTH) return true;
        return promoCycle.toString().equalsIgnoreCase(requestCycle);
    }

    /**
     * Check student promo exclusivity rules:
     * 1. STUDENT_PROMO can only be applied by PATIENT users (not doctors)
     * 2. STUDENT_PROMO only applies to PREMIUM plans
     * 3. User must have APPROVED student verification to use STUDENT_PROMO
     * 4. If user has STUDENT_PROMO applied + approved student verification, they cannot apply other promos
     * 
     * @return null if validation passes, otherwise PromoCodeValidationResponseDTO with error
     */
    private PromoCodeValidationResponseDTO studentPromoExclusivityCheck(
            AppUser user, PromoCode promoCode, String planName) {
        
        final String STUDENT_PROMO_CODE = "STUDENT_PROMO";
        final String PREMIUM_PLAN = "PREMIUM";
        
        // Check if this is the STUDENT_PROMO code
        boolean isStudentPromo = STUDENT_PROMO_CODE.equalsIgnoreCase(promoCode.getCode());
        
        if (isStudentPromo) {
            // STUDENT_PROMO is only for PATIENT users (not doctors)
            if (!skylinkers.tn.mediconnectbackend.entities.enums.PlanType.PATIENT.equals(user.getUserType())) {
                return PromoCodeValidationResponseDTO.builder()
                        .valid(false)
                        .message("Student discount is only available for patients")
                        .build();
            }
            
            // STUDENT_PROMO only applies to PREMIUM plans
            if (!PREMIUM_PLAN.equalsIgnoreCase(planName)) {
                return PromoCodeValidationResponseDTO.builder()
                        .valid(false)
                        .message("Student discount is only available for Premium plan subscribers")
                        .build();
            }
            
            // User must have approved student verification to use STUDENT_PROMO
            var studentVerification = studentVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());
            if (studentVerification.isEmpty() || 
                !studentVerification.get().getStatus().equals(SubVerificationStatus.APPROVED)) {
                return PromoCodeValidationResponseDTO.builder()
                        .valid(false)
                        .message("You must have approved student verification to use this code")
                        .build();
            }
        } else {
            // If applying a non-STUDENT promo and user has STUDENT_PROMO already applied with approved verification
            var studentPromoUsages = promoCodeUsageRepository.findByPromoCode_Code(STUDENT_PROMO_CODE);
            var studentPromoUsedByUser = studentPromoUsages.stream()
                    .filter(usage -> usage.getUser().getId().equals(user.getId()))
                    .findFirst();
            
            if (studentPromoUsedByUser.isPresent()) {
                // Check if user has approved student verification
                var studentVerification = studentVerificationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId());
                if (studentVerification.isPresent() && 
                    studentVerification.get().getStatus().equals(SubVerificationStatus.APPROVED)) {
                    return PromoCodeValidationResponseDTO.builder()
                            .valid(false)
                            .message("You cannot apply multiple discount codes when using the student discount with verified student status")
                            .build();
                }
            }
        }
        
        return null; // All checks pass
    }
}