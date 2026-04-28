package skylinkers.tn.mediconnectbackend.controller.SubscriptionController;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CreateCheckoutRequestDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.CreateCheckoutResponseDTO;
import skylinkers.tn.mediconnectbackend.dto.SubscriptionDto.PriceCalculationRequestDTO;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.PaymentService;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.SubscriptionService;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200", "http://localhost:5173"})
public class PaymentController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/create-checkout")
    public ResponseEntity<CreateCheckoutResponseDTO> createCheckout(
            @RequestBody CreateCheckoutRequestDTO request,
            @RequestParam(required = false) String promoCode,
            @RequestHeader(value = "X-Promo-Code", required = false) String promoCodeHeader) {
        if ((request.getPromoCode() == null || request.getPromoCode().trim().isEmpty())) {
            String fallbackPromo = firstNonBlank(promoCode, promoCodeHeader);
            if (fallbackPromo != null) {
                request.setPromoCode(fallbackPromo);
            }
        }
        log.info("Creating checkout session for user: {}", request.getUserId());
        try {
            CreateCheckoutResponseDTO response = paymentService.createCheckoutSession(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.warn("Checkout validation failed: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(
                    CreateCheckoutResponseDTO.builder()
                            .message(ex.getMessage())
                            .build()
            );
        }
    }

    @PostMapping("/price-summary")
    public ResponseEntity<?> getPriceSummary(@RequestBody PriceCalculationRequestDTO request) {
        // Reuse the logic from SubscriptionController
        // Assuming the request has userId and it's validated
        return ResponseEntity.ok(subscriptionService.calculatePrice(request));
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

    @PostMapping("/confirm-session/{sessionId}")
    public ResponseEntity<Void> confirmSession(@PathVariable String sessionId) {
        log.info("Confirming checkout session from success redirect: {}", sessionId);
        paymentService.handlePaymentSuccess(sessionId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            switch (event.getType()) {
                case "checkout.session.completed":
                    handleCheckoutSessionCompleted(event);
                    break;
                case "invoice.payment_failed":
                    handleInvoicePaymentFailed(event);
                    break;
                default:
                    log.debug("Ignoring unhandled event type: {}", event.getType());
            }

            return ResponseEntity.ok("{}");
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature: {}", e.getMessage());
            return ResponseEntity.status(403).body("Invalid signature");
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage());
            return ResponseEntity.status(400).body("Webhook error");
        }
    }

    private void handleCheckoutSessionCompleted(Event event) {
        try {
            String rawJson = event.getData().toJson();
            JsonObject data = JsonParser.parseString(rawJson)
                    .getAsJsonObject()
                    .getAsJsonObject("object");
            String sessionId = data.get("id").getAsString();
            log.info("Checkout session completed: {}", sessionId);
            paymentService.handlePaymentSuccess(sessionId);
        } catch (Exception e) {
            log.error("Failed to process checkout session - Exception: {} | Root cause: {}", e.getMessage(), e.getCause(), e);
            throw new RuntimeException("Failed to handle checkout session: " + e.getMessage(), e);
        }
    }

    private void handleInvoicePaymentFailed(Event event) {
        try {
            String customerEmail = extractEmailFromEvent(event);
            if (customerEmail != null) {
                log.info("Invoice payment failed for customer: {}", customerEmail);
                paymentService.handlePaymentFailedByEmail(customerEmail);
            } else {
                log.warn("Could not extract customer email from invoice event");
            }
        } catch (Exception e) {
            log.error("Failed to process invoice payment failed: {}", e.getMessage());
        }
    }

    private String extractEmailFromEvent(Event event) {
        try {
            String rawJson = event.getData().toJson();
            JsonObject data = JsonParser.parseString(rawJson)
                    .getAsJsonObject()
                    .getAsJsonObject("object");
            if (data.has("customer_email") && !data.get("customer_email").isJsonNull()) {
                return data.get("customer_email").getAsString();
            }
        } catch (Exception e) {
            log.error("Failed to extract email from event: {}", e.getMessage());
        }
        return null;
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CreateCheckoutResponseDTO> handleRuntimeException(RuntimeException ex, WebRequest request) {
        String path = request.getDescription(false);
        log.warn("Handled runtime exception at {}: {}", path, ex.getMessage());
        return ResponseEntity.badRequest().body(
                CreateCheckoutResponseDTO.builder()
                        .message(ex.getMessage() != null ? ex.getMessage() : "Checkout failed")
                        .build()
        );
    }
}