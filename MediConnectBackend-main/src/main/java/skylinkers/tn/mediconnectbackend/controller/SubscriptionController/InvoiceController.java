package skylinkers.tn.mediconnectbackend.controller.SubscriptionController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import skylinkers.tn.mediconnectbackend.entities.Invoice;
import skylinkers.tn.mediconnectbackend.entities.Payment;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.InvoiceRepository;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.PaymentRepository;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.InvoiceAndEmailService;
import skylinkers.tn.mediconnectbackend.service.SubscriptionService.InvoiceService;

import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceAndEmailService invoiceAndEmailService;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;

    @GetMapping("/url/{invoiceId}")
    public ResponseEntity<String> getInvoiceUrl(@PathVariable String invoiceId) {
        try {
            Invoice invoice = invoiceRepository.findById(Long.parseLong(invoiceId))
                    .orElseThrow(() -> new RuntimeException("Invoice not found"));
            return ResponseEntity.ok(invoice.getFilePath());
        } catch (RuntimeException e) {
            log.error("Invoice not found: {}", invoiceId);
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/download/{invoiceId}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String invoiceId) {
        try {
            byte[] pdf = invoiceService.getInvoicePDF(invoiceId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=invoice_" + invoiceId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (NumberFormatException e) {
            log.error("Invalid invoice ID format: {}", invoiceId);
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Invoice not found or error retrieving invoice {}: {}", invoiceId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to download invoice {}: {}", invoiceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/view/{invoiceId}")
    public ResponseEntity<byte[]> viewInvoice(@PathVariable String invoiceId) {
        try {
            byte[] pdf = invoiceService.getInvoicePDF(invoiceId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=invoice_" + invoiceId + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (NumberFormatException e) {
            log.error("Invalid invoice ID format: {}", invoiceId);
            return ResponseEntity.badRequest().build();
        } catch (RuntimeException e) {
            log.error("Invoice not found or error retrieving invoice {}: {}", invoiceId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to view invoice {}: {}", invoiceId, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/debug/generate-invoice/{subscriptionId}")
    public ResponseEntity<?> debugGenerateInvoice(@PathVariable Long subscriptionId) {
        log.info("DEBUG: Manually triggering invoice generation for subscription {}", subscriptionId);
        try {
            Payment lastPayment = paymentRepository.findTopBySubscription_IdOrderByCreatedAtDesc(subscriptionId)
                    .orElseThrow(() -> new RuntimeException("No payment found for this subscription"));
            invoiceAndEmailService.generateInvoiceAndSendEmail(subscriptionId, lastPayment.getId());
            return ResponseEntity.ok("Async generation triggered. Check backend logs.");
        } catch (Exception e) {
            log.error("Debug generation failed", e);
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }
}