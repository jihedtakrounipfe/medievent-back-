package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import skylinkers.tn.mediconnectbackend.entities.Invoice;
import skylinkers.tn.mediconnectbackend.entities.Payment;
import skylinkers.tn.mediconnectbackend.entities.Subscription;

import java.io.IOException;

public interface InvoiceService {
    Invoice generateInvoice(Subscription subscription, Payment payment) throws IOException;
    byte[] getInvoicePDF(String invoiceId) throws IOException;
}