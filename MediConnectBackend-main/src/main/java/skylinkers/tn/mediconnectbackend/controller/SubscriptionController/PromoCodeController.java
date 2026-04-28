package skylinkers.tn.mediconnectbackend.controller.SubscriptionController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeResponseDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeValidationDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PromoCodeValidationResponseDTO;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.PromoCodeService;

import java.util.List;

@RestController
@RequestMapping("/api/promo-codes")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000", "http://localhost:5173"})
public class PromoCodeController {

    private final PromoCodeService promoCodeService;

    // Admin endpoints
    @PostMapping("/admin/create")
    public ResponseEntity<PromoCodeResponseDTO> createPromoCode(
            @RequestBody PromoCodeRequestDTO request,
            @RequestParam Long adminId) {
        log.info("Creating promo code: {}", request.getCode());
        PromoCodeResponseDTO response = promoCodeService.createPromoCode(request, adminId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/admin/update/{id}")
    public ResponseEntity<PromoCodeResponseDTO> updatePromoCode(
            @PathVariable Long id,
            @RequestBody PromoCodeRequestDTO request) {
        log.info("Updating promo code: {}", id);
        PromoCodeResponseDTO response = promoCodeService.updatePromoCode(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/delete/{id}")
    public ResponseEntity<Void> deletePromoCode(@PathVariable Long id) {
        log.info("Deleting promo code: {}", id);
        promoCodeService.deletePromoCode(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<PromoCodeResponseDTO>> getAllPromoCodes() {
        List<PromoCodeResponseDTO> codes = promoCodeService.getAllPromoCodes();
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<PromoCodeResponseDTO> getPromoCode(@PathVariable Long id) {
        PromoCodeResponseDTO code = promoCodeService.getPromoCode(id);
        return ResponseEntity.ok(code);
    }

    @PutMapping("/admin/toggle/{id}")
    public ResponseEntity<Void> togglePromoCode(@PathVariable Long id) {
        log.info("Toggling promo code: {}", id);
        promoCodeService.togglePromoCode(id);
        return ResponseEntity.noContent().build();
    }

    // User endpoints
    @PostMapping("/validate")
    public ResponseEntity<PromoCodeValidationResponseDTO> validatePromoCode(
            @RequestBody PromoCodeValidationDTO request) {
        log.info("Validating promo code: {}", request != null ? request.getCode() : null);
        PromoCodeValidationResponseDTO response;
        try {
            response = promoCodeService.validatePromoCode(request);
        } catch (Exception ex) {
            log.error("Promo validation failed", ex);
            response = PromoCodeValidationResponseDTO.builder()
                    .valid(false)
                    .message("Failed to validate promo code")
                    .build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<PromoCodeValidationResponseDTO> validatePromoCodeGet(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String planType,
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) String billingCycle,
            @RequestParam(required = false) java.math.BigDecimal planPrice,
            HttpServletRequest httpRequest) {

        // Accept common frontend query param variants to avoid hard 400 errors.
        if (userId == null) {
            userId = firstNonBlank(httpRequest.getParameter("userid"),
                    httpRequest.getParameter("user_id"),
                    httpRequest.getParameter("userID"));
        }
        if (planType == null) {
            planType = firstNonBlank(httpRequest.getParameter("plan_type"),
                    httpRequest.getParameter("plantype"));
        }
        if (planName == null) {
            planName = firstNonBlank(httpRequest.getParameter("plan_name"),
                    httpRequest.getParameter("planname"));
        }
        if (billingCycle == null) {
            billingCycle = firstNonBlank(httpRequest.getParameter("billing_cycle"),
                    httpRequest.getParameter("billingcycle"));
        }
        if (planPrice == null) {
            String rawPlanPrice = firstNonBlank(httpRequest.getParameter("plan_price"),
                    httpRequest.getParameter("planprice"));
            if (rawPlanPrice != null) {
                try {
                    planPrice = new java.math.BigDecimal(rawPlanPrice.trim());
                } catch (NumberFormatException ignored) {
                    planPrice = null;
                }
            }
        }

        Long userIdLong = null;
        if (userId != null && !userId.trim().isEmpty()) {
            try {
                userIdLong = Long.parseLong(userId.trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid userId format: {}", userId);
            }
        }

        PromoCodeValidationDTO request = PromoCodeValidationDTO.builder()
                .code(code)
                .userId(userIdLong)
                .planType(planType)
                .planName(planName)
                .billingCycle(billingCycle)
                .planPrice(planPrice)
                .build();

        return validatePromoCode(request);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<PromoCodeValidationResponseDTO> handleMissingRequestParam(MissingServletRequestParameterException ex) {
        log.warn("Promo request missing parameter: {}", ex.getParameterName());
        return ResponseEntity.ok(
                PromoCodeValidationResponseDTO.builder()
                        .valid(false)
                        .message("Missing parameter: " + ex.getParameterName())
                        .build()
        );
    }

    @PostMapping("/use")
    public ResponseEntity<Void> markPromoCodeAsUsed(
            @RequestParam String code,
            @RequestParam Long userId,
            @RequestParam Long subscriptionId) {
        log.info("Marking promo code as used: {}", code);
        promoCodeService.markPromoCodeAsUsed(code, userId, subscriptionId);
        return ResponseEntity.noContent().build();
    }
}