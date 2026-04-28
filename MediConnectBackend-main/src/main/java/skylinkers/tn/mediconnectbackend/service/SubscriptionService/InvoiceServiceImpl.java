package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.entities.Invoice;
import skylinkers.tn.mediconnectbackend.entities.Payment;
import skylinkers.tn.mediconnectbackend.entities.Subscription;
import skylinkers.tn.mediconnectbackend.repository.SubscriptionRepository.InvoiceRepository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.options.BlobParallelUploadOptions;

import java.awt.Color;
import java.math.RoundingMode;
import java.time.LocalDate;

import java.time.format.FormatStyle;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private static final PDType1Font FONT_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font FONT_REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.US);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Color BRAND_PRIMARY = new Color(13, 148, 136); // #0d9488
    private static final Color BRAND_ACCENT = new Color(8, 145, 178);   // #0891b2
    private static final Color LIGHT_GRAY = new Color(242, 244, 248);

    @Value("${azure.storage.connection-string}")
    private String azureConnectionString;

    @Value("${azure.storage.container-name}")
    private String containerName;

    private BlobContainerClient getBlobContainerClient() {
        if (azureConnectionString == null || azureConnectionString.isBlank()) {
            throw new IllegalStateException("azure.storage.connection-string is missing");
        }
        if (containerName == null || containerName.isBlank()) {
            throw new IllegalStateException("azure.storage.container-name is missing");
        }

        BlobContainerClient containerClient = new BlobContainerClientBuilder()
                .connectionString(azureConnectionString)
                .containerName(containerName)
                .buildClient();

        if (!containerClient.exists()) {
            log.warn("Azure container '{}' was missing. Creating it now.", containerName);
            containerClient.create();
        }

        return containerClient;
    }

    @Override
    public Invoice generateInvoice(Subscription subscription, Payment payment) throws IOException {
        String invoiceId = UUID.randomUUID().toString();
        String invoiceNumber = buildInvoiceNumber();
        String blobName = "invoice_" + invoiceId + ".pdf";

        String planName = subscription.getPatientPlan() != null ?
                subscription.getPatientPlan().getName().toString() :
                subscription.getDoctorPlan().getName().toString();

        // Use original TND price if available (from payment), otherwise fall back to subscription amountPaid or plan price
        BigDecimal amount;
        String currencyCode;

        if (payment != null && payment.getOriginalPriceTnd() != null) {
            // Use the original TND plan price (before currency conversion)
            amount = payment.getOriginalPriceTnd();
            currencyCode = "TND";
        } else if (subscription.getAmountPaid() != null) {
            // Fall back to subscription amount paid (should be in TND)
            amount = subscription.getAmountPaid();
            currencyCode = "TND";
        } else {
            // Fall back to plan price if no payment/subscription amount
            amount = subscription.getPatientPlan() != null
                    ? subscription.getPatientPlan().getPriceMonthly()
                    : subscription.getDoctorPlan().getPriceMonthly();
            currencyCode = "TND";
        }

        LocalDateTime generatedAt = LocalDateTime.now();
        String customerEmail = subscription.getUser() != null ? subscription.getUser().getEmail() : "N/A";
        String billingCycle = subscription.getBillingCycle() != null ? subscription.getBillingCycle().name() : "N/A";
        String paymentProvider = subscription.getPaymentProvider() != null ? subscription.getPaymentProvider().name() : "N/A";
        String paymentRef = safeText(subscription.getPaymentRef());
        String status = subscription.getStatus() != null ? subscription.getStatus().name() : "N/A";
        String period = formatDate(subscription.getStartDate()) + " - " + formatDate(subscription.getEndDate());
        String unitPrice = formatCurrency(amount, currencyCode);
        String totalAmount = formatCurrency(amount, currencyCode);

        // Generate PDF into memory
        ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float margin = 40f;
                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();
                float contentWidth = pageWidth - (margin * 2);
                float left = margin;
                float cursorY = pageHeight - margin;

                // Header (fully inside page)
                float headerH = 72f;
                float headerY = cursorY - headerH;
                fillRect(contentStream, left, headerY, contentWidth, headerH, BRAND_PRIMARY);
                drawBrandMark(contentStream, left + 16, headerY + 34);
                writeLine(contentStream, left + 52, headerY + 44, FONT_BOLD, 24, Color.WHITE, "INVOICE");
                writeLine(contentStream, left + 52, headerY + 24, FONT_REGULAR, 10, Color.WHITE, "MediConnect Subscription Billing");

                float metaW = 210f;
                float metaH = 56f;
                float metaX = left + contentWidth - metaW - 12;
                float metaY = headerY + 8;
                fillRect(contentStream, metaX, metaY, metaW, metaH, Color.WHITE);
                strokeRect(contentStream, metaX, metaY, metaW, metaH, Color.BLACK);
                writeLine(contentStream, metaX + 10, metaY + 38, FONT_BOLD, 9, "DATE");
                writeRightAligned(contentStream, metaX + metaW - 10, metaY + 38, FONT_REGULAR, 9, generatedAt.format(DATE_TIME_FORMATTER));
                writeLine(contentStream, metaX + 10, metaY + 24, FONT_BOLD, 9, "INVOICE NO.");
                writeRightAligned(contentStream, metaX + metaW - 10, metaY + 24, FONT_REGULAR, 9, invoiceNumber);
                writeLine(contentStream, metaX + 10, metaY + 10, FONT_BOLD, 9, "STATUS");
                writeRightAligned(contentStream, metaX + metaW - 10, metaY + 10, FONT_REGULAR, 9, status);

                cursorY = headerY - 18;

                // Company + customer blocks
                float blockGap = 15f;
                float boxW = (contentWidth - blockGap) / 2f;
                float boxH = 118f;
                float leftBoxX = left;
                float rightBoxX = left + boxW + blockGap;
                float boxY = cursorY - boxH;

                fillRect(contentStream, leftBoxX, boxY + boxH - 22, boxW, 22, LIGHT_GRAY);
                fillRect(contentStream, rightBoxX, boxY + boxH - 22, boxW, 22, LIGHT_GRAY);
                strokeRect(contentStream, leftBoxX, boxY, boxW, boxH, Color.BLACK);
                strokeRect(contentStream, rightBoxX, boxY, boxW, boxH, Color.BLACK);

                writeLine(contentStream, leftBoxX + 10, boxY + boxH - 15, FONT_BOLD, 10, "FROM");
                writeLine(contentStream, leftBoxX + 10, boxY + boxH - 36, FONT_REGULAR, 10, "MediConnect");
                writeLine(contentStream, leftBoxX + 10, boxY + boxH - 52, FONT_REGULAR, 10, "support@mediconnect.tn");
                writeLine(contentStream, leftBoxX + 10, boxY + boxH - 68, FONT_REGULAR, 10, "Invoice ID:");
                writeLine(contentStream, leftBoxX + 78, boxY + boxH - 68, FONT_REGULAR, 9, truncate(invoiceId, 28));
                writeLine(contentStream, leftBoxX + 10, boxY + boxH - 84, FONT_REGULAR, 10, "Generated:");
                writeLine(contentStream, leftBoxX + 78, boxY + boxH - 84, FONT_REGULAR, 9, generatedAt.format(DATE_TIME_FORMATTER));

                writeLine(contentStream, rightBoxX + 10, boxY + boxH - 15, FONT_BOLD, 10, "BILL TO");
                writeLine(contentStream, rightBoxX + 10, boxY + boxH - 36, FONT_REGULAR, 10, truncate(customerEmail, 32));
                writeLine(contentStream, rightBoxX + 10, boxY + boxH - 52, FONT_REGULAR, 10, "Payment Provider:");
                writeLine(contentStream, rightBoxX + 102, boxY + boxH - 52, FONT_REGULAR, 10, truncate(paymentProvider, 18));
                writeLine(contentStream, rightBoxX + 10, boxY + boxH - 68, FONT_REGULAR, 10, "Payment Ref:");
                writeLine(contentStream, rightBoxX + 78, boxY + boxH - 68, FONT_REGULAR, 9, truncate(paymentRef, 28));
                writeLine(contentStream, rightBoxX + 10, boxY + boxH - 84, FONT_REGULAR, 10, "Subscription:");
                writeLine(contentStream, rightBoxX + 78, boxY + boxH - 84, FONT_REGULAR, 10, truncate(status, 18));

                cursorY = boxY - 20;

                // Items table
                float tableHeaderH = 24f;
                float rowH = 28f;
                float tableH = tableHeaderH + rowH;
                float tableY = cursorY - tableH;
                float[] cols = new float[]{220f, 70f, 110f, 115f};

                fillRect(contentStream, left, tableY + rowH, contentWidth, tableHeaderH, BRAND_PRIMARY);
                strokeRect(contentStream, left, tableY, contentWidth, tableH, Color.BLACK);
                strokeLine(contentStream, left, tableY + rowH, left + contentWidth, tableY + rowH, Color.BLACK);

                float x = left;
                for (float col : cols) {
                    strokeLine(contentStream, x, tableY, x, tableY + tableH, Color.BLACK);
                    x += col;
                }
                strokeLine(contentStream, left + contentWidth, tableY, left + contentWidth, tableY + tableH, Color.BLACK);

                writeLine(contentStream, left + 10, tableY + rowH + 8, FONT_BOLD, 10, Color.WHITE, "DESCRIPTION");
                writeLine(contentStream, left + 230, tableY + rowH + 8, FONT_BOLD, 10, Color.WHITE, "QTY");
                writeLine(contentStream, left + 300, tableY + rowH + 8, FONT_BOLD, 10, Color.WHITE, "UNIT PRICE");
                writeLine(contentStream, left + 415, tableY + rowH + 8, FONT_BOLD, 10, Color.WHITE, "TOTAL");

                writeLine(contentStream, left + 10, tableY + 9, FONT_REGULAR, 10, "MediConnect " + truncate(planName, 22) + " subscription");
                writeRightAligned(contentStream, left + 285, tableY + 9, FONT_REGULAR, 10, "1");
                writeRightAligned(contentStream, left + 400, tableY + 9, FONT_REGULAR, 10, unitPrice);
                writeRightAligned(contentStream, left + 505, tableY + 9, FONT_BOLD, 10, totalAmount);

                cursorY = tableY - 20;

                // Details row with more context
                float detailsH = 94f;
                float detailsY = cursorY - detailsH;
                strokeRect(contentStream, left, detailsY, contentWidth, detailsH, Color.BLACK);
                strokeLine(contentStream, left + contentWidth / 2f, detailsY, left + contentWidth / 2f, detailsY + detailsH, Color.BLACK);

                writeLine(contentStream, left + 10, detailsY + 72, FONT_BOLD, 10, "SUBSCRIPTION DETAILS");
                writeLine(contentStream, left + 10, detailsY + 54, FONT_REGULAR, 10, "Billing Cycle: " + truncate(billingCycle, 20));
                writeLine(contentStream, left + 10, detailsY + 38, FONT_REGULAR, 10, "Start Date: " + formatDate(subscription.getStartDate()));
                writeLine(contentStream, left + 10, detailsY + 22, FONT_REGULAR, 10, "End Date: " + formatDate(subscription.getEndDate()));

                float rightDetailsX = left + contentWidth / 2f + 10;
                writeLine(contentStream, rightDetailsX, detailsY + 72, FONT_BOLD, 10, "PAYMENT DETAILS");
                writeLine(contentStream, rightDetailsX, detailsY + 54, FONT_REGULAR, 10, "Provider: " + truncate(paymentProvider, 22));
                writeLine(contentStream, rightDetailsX, detailsY + 38, FONT_REGULAR, 10, "Reference: " + truncate(paymentRef, 24));
                writeLine(contentStream, rightDetailsX, detailsY + 22, FONT_REGULAR, 10, "Customer: " + truncate(customerEmail, 28));

                // Summary box
                float summaryW = 220f;
                float summaryX = left + contentWidth - summaryW;
                float summaryRowH = 24f;
                float summaryH = summaryRowH * 3;
                float summaryY = detailsY - 16 - summaryH;

                strokeRect(contentStream, summaryX, summaryY, summaryW, summaryH, Color.BLACK);
                strokeLine(contentStream, summaryX, summaryY + summaryRowH, summaryX + summaryW, summaryY + summaryRowH, Color.BLACK);
                strokeLine(contentStream, summaryX, summaryY + summaryRowH * 2, summaryX + summaryW, summaryY + summaryRowH * 2, Color.BLACK);
                strokeLine(contentStream, summaryX + 130, summaryY, summaryX + 130, summaryY + summaryH, Color.BLACK);
                fillRect(contentStream, summaryX, summaryY, 130, summaryRowH, BRAND_ACCENT);

                writeLine(contentStream, summaryX + 10, summaryY + summaryRowH * 2 + 7, FONT_BOLD, 10, "SUBTOTAL");
                writeRightAligned(contentStream, summaryX + summaryW - 10, summaryY + summaryRowH * 2 + 7, FONT_REGULAR, 10, totalAmount);
                writeLine(contentStream, summaryX + 10, summaryY + summaryRowH + 7, FONT_BOLD, 10, "TAX");
                writeRightAligned(contentStream, summaryX + summaryW - 10, summaryY + summaryRowH + 7, FONT_REGULAR, 10, "0.00 " + currencyCode);
                writeLine(contentStream, summaryX + 10, summaryY + 7, FONT_BOLD, 10, Color.WHITE, "TOTAL DUE");
                writeRightAligned(contentStream, summaryX + summaryW - 10, summaryY + 7, FONT_BOLD, 10, totalAmount);

                // Footer
                writeLine(contentStream, left, margin + 34, FONT_REGULAR, 10, "Remarks: This invoice was generated automatically by MediConnect.");
                writeLine(contentStream, left, margin + 18, FONT_REGULAR, 9, "For support, contact support@mediconnect.tn");
                writeLine(contentStream, left, margin + 2, FONT_BOLD, 14, "Thank you for your business.");
            }

            document.save(pdfOutputStream);
        }

        // Upload to Azure Blob Storage
        try {
            BlobContainerClient containerClient = getBlobContainerClient();
            byte[] pdfBytes = pdfOutputStream.toByteArray();
            log.debug("PDF size: {} bytes", pdfBytes.length);

            BlobClient blobClient = containerClient.getBlobClient(blobName);
            BlobParallelUploadOptions uploadOptions = new BlobParallelUploadOptions(new ByteArrayInputStream(pdfBytes), (long) pdfBytes.length)
                    .setHeaders(new BlobHttpHeaders().setContentType("application/pdf"));
            blobClient.uploadWithResponse(uploadOptions, null, null);

            String blobUrl = blobClient.getBlobUrl();
            log.info("PDF uploaded to Azure Blob: {}", blobUrl);

            // Save invoice metadata to DB
            Invoice invoice = Invoice.builder()
                    .subscription(subscription)
                    .invoiceNumber(invoiceNumber)
                    .filePath(blobUrl)
                    .fileName(blobName)
                    .amount(amount)
                    .createdAt(generatedAt)
                    .build();

            invoiceRepository.save(invoice);
            log.info("Invoice generated and uploaded to Azure Blob: {}", blobName);

            return invoice;
        } catch (Exception e) {
            log.error("Failed to upload invoice to Azure Blob Storage. container='{}', connectionConfigured={}, invoiceBlob='{}', reason={}",
                    containerName,
                    azureConnectionString != null && !azureConnectionString.isBlank(),
                    blobName,
                    e.getMessage(),
                    e);
            throw new IOException("Failed to upload invoice to Azure Blob Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getInvoicePDF(String invoiceId) throws IOException {
        Invoice invoice = invoiceRepository.findById(Long.parseLong(invoiceId))
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        BlobContainerClient containerClient = getBlobContainerClient();
        BlobClient blobClient = containerClient.getBlobClient(invoice.getFileName());

        if (!blobClient.exists()) {
            log.error("Invoice blob missing for invoiceId={}, blobName={}", invoiceId, invoice.getFileName());
            throw new IOException("Invoice file not found in Azure Blob Storage");
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.download(outputStream);
        return outputStream.toByteArray();
    }

    private String buildInvoiceNumber() {
        return "MC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "N/A";
    }

    private String formatCurrency(BigDecimal value, String currencyCode) {
        BigDecimal amount = value != null ? value : BigDecimal.ZERO;
        return amount.setScale(2, RoundingMode.HALF_UP) + " " + currencyCode;
    }

    private String safeText(String value) {
        return (value == null || value.isBlank()) ? "N/A" : value;
    }

    private void writeLine(PDPageContentStream contentStream,
                           float x,
                           float y,
                           PDType1Font font,
                           float fontSize,
                           String text) throws IOException {
        writeLine(contentStream, x, y, font, fontSize, Color.BLACK, text);
    }

    private void writeLine(PDPageContentStream contentStream,
                           float x,
                           float y,
                           PDType1Font font,
                           float fontSize,
                           Color color,
                           String text) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, fontSize);
        contentStream.setNonStrokingColor(color);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text != null ? text : "");
        contentStream.endText();
        contentStream.setNonStrokingColor(Color.BLACK);
    }

    private void writeRightAligned(PDPageContentStream contentStream,
                                   float x,
                                   float y,
                                   PDType1Font font,
                                   float fontSize,
                                   String text) throws IOException {
        String safe = text != null ? text : "";
        float width = font.getStringWidth(safe) / 1000f * fontSize;
        writeLine(contentStream, x - width, y, font, fontSize, safe);
    }

    private void fillRect(PDPageContentStream contentStream,
                          float x,
                          float y,
                          float width,
                          float height,
                          Color color) throws IOException {
        contentStream.setNonStrokingColor(color);
        contentStream.addRect(x, y, width, height);
        contentStream.fill();
        contentStream.setNonStrokingColor(Color.BLACK);
    }

    private void drawBrandMark(PDPageContentStream contentStream, float x, float y) throws IOException {
        // Simple inline logo mark so invoices remain branded without external image dependencies.
        contentStream.setNonStrokingColor(Color.WHITE);
        contentStream.addRect(x, y, 26, 26);
        contentStream.fill();

        contentStream.setNonStrokingColor(BRAND_PRIMARY);
        contentStream.addRect(x + 11, y + 5, 4, 16);
        contentStream.fill();
        contentStream.addRect(x + 5, y + 11, 16, 4);
        contentStream.fill();

        writeLine(contentStream, x + 32, y + 13, FONT_BOLD, 11, Color.WHITE, "MediConnect");
        writeLine(contentStream, x + 32, y + 2, FONT_REGULAR, 8, Color.WHITE, "Health Platform");
    }

    private void strokeRect(PDPageContentStream contentStream,
                            float x,
                            float y,
                            float width,
                            float height,
                            Color color) throws IOException {
        contentStream.setStrokingColor(color);
        contentStream.addRect(x, y, width, height);
        contentStream.stroke();
        contentStream.setStrokingColor(Color.BLACK);
    }

    private void strokeLine(PDPageContentStream contentStream,
                            float x1,
                            float y1,
                            float x2,
                            float y2,
                            Color color) throws IOException {
        contentStream.setStrokingColor(color);
        contentStream.moveTo(x1, y1);
        contentStream.lineTo(x2, y2);
        contentStream.stroke();
        contentStream.setStrokingColor(Color.BLACK);
    }

    private String truncate(String value, int maxLength) {
        String safe = safeText(value);
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, maxLength - 3) + "...";
    }
}