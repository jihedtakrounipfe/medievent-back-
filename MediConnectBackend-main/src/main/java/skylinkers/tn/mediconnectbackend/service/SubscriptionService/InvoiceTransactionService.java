package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import skylinkers.tn.mediconnectbackend.entities.Invoice;
import skylinkers.tn.mediconnectbackend.entities.Payment;
import skylinkers.tn.mediconnectbackend.entities.Subscription;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.PaymentRepository;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.SubscriptionRepository;

/**
 * Separate bean to ensure @Transactional(REQUIRES_NEW) is honoured by Spring AOP proxy.
 * Self-invocation within InvoiceAndEmailService would bypass the proxy and silently
 * ignore the propagation setting.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceTransactionService {

    private final InvoiceService invoiceService;
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Invoice generateInvoiceInNewTransaction(Long subscriptionId, Long paymentId) throws Exception {
        log.info(">>> [TX] Opening REQUIRES_NEW transaction for invoice generation. subId={}, payId={}", subscriptionId, paymentId);
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found for invoice: " + subscriptionId));
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found for invoice: " + paymentId));
        return invoiceService.generateInvoice(subscription, payment);
    }
}
