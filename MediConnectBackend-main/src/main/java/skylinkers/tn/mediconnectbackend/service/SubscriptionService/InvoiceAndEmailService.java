package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.entities.Invoice;
import skylinkers.tn.mediconnectbackend.entities.Payment;
import skylinkers.tn.mediconnectbackend.entities.Subscription;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.PaymentRepository;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.SubscriptionRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceAndEmailService {

    private final InvoiceTransactionService invoiceTransactionService;
    private final SubscriptionEmailService subscriptionEmailService;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @Async
    public void generateInvoiceAndSendEmail(Long subscriptionId, Long paymentId) {
        log.info(">>> [ASYNC] Entering generateInvoiceAndSendEmail. subId={}, payId={}", subscriptionId, paymentId);
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
        Payment payment = paymentRepository.findById(paymentId).orElse(null);

        if (subscription == null || payment == null) {
            log.error("Async: Missing subscription/payment for invoice generation. subscriptionId={}, paymentId={}", subscriptionId, paymentId);
            return;
        }

        Invoice invoice = null;
        try {
            log.info("Async: Starting invoice generation for subscription: {}", subscription.getId());
            // Call through the separate bean so @Transactional(REQUIRES_NEW) is honoured by Spring proxy
            invoice = invoiceTransactionService.generateInvoiceInNewTransaction(subscription.getId(), payment.getId());
            if (invoice != null) {
                log.info("Async: Invoice generated successfully with ID: {}", invoice.getId());
            } else {
                log.error("Async: Invoice generation returned null for subscription: {}", subscription.getId());
            }
        } catch (Exception e) {
            log.error("Async: CRITICAL FAILURE in invoice generation for subscription {}: {}", subscription.getId(), e.getMessage(), e);
        }

        try {
            String userEmail = payment.getUser().getEmail();
            String planName = "Your Plan";
            if (subscription.getDoctorPlan() != null && subscription.getDoctorPlan().getName() != null) {
                planName = subscription.getDoctorPlan().getName();
            } else if (subscription.getPatientPlan() != null && subscription.getPatientPlan().getName() != null) {
                planName = subscription.getPatientPlan().getName();
            }
            String amount = invoice != null && invoice.getAmount() != null
                    ? invoice.getAmount().toPlainString()
                    : (payment.getAmount() != null ? payment.getAmount().toPlainString() : "N/A");
            String currency = payment.getCurrency() != null ? payment.getCurrency().toUpperCase() : "TND";

            subscriptionEmailService.sendSubscriptionConfirmationEmail(
                    userEmail,
                    subscription.getId().toString(),
                    invoice != null ? invoice.getId().toString() : null,
                    planName,
                    amount,
                    currency
            );
            if (invoice != null) {
                log.info("Async: Confirmation email sent with invoice");
            } else {
                log.info("Async: Fallback confirmation email sent (no invoice)");
            }
        } catch (Exception e) {
            log.error("Async: Failed to send confirmation email: {}", e.getMessage(), e);
        }
    }
}
