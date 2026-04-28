package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import skylinkers.tn.mediconnectbackend.entities.AppUser;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionEmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;
    
    @Value("${spring.mail.username}")
    private String fromEmail; 

    @Value("${app.backend-base-url:http://localhost:8080}")
    private String backendBaseUrl;

    @Value("${app.logo-url:https://via.placeholder.com/140x50?text=MediConnect}")
    private String appLogoUrl;

    private byte[] inlineLogoPng;
    
    /**
     * Send subscription confirmation email using HTML template
     */
    public void sendSubscriptionConfirmation(String toEmail, String planName, String billingCycle) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", "Valued Customer");
        variables.put("planName", planName);
        variables.put("billingCycle", billingCycle);
        variables.put("planType", "Doctor/Patient Plan");
        
        sendHtmlEmail(
            toEmail,
            "MediConnect — Subscription Confirmed ✅",
            "subscription-confirmed.html",
            variables
        );
    }

    /**
     * Send payment success confirmation with invoice download link
     */
    public void sendSubscriptionConfirmationEmail(
            String toEmail,
            String subscriptionId,
            String invoiceId,
            String planName,
            String amount,
            String currency
    ) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", "Valued Customer");
        variables.put("subscriptionId", subscriptionId);
        variables.put("invoiceId", invoiceId);
        variables.put("planName", planName != null ? planName : "Your Plan");
        variables.put("amount", amount != null ? amount : "N/A");
        variables.put("currency", currency != null ? currency : "TND");
        variables.put("billingCycle", "Monthly"); // Default billing cycle
        variables.put("planType", "Doctor/Patient Plan");
        if (invoiceId != null && !invoiceId.isBlank()) {
            String normalizedBase = backendBaseUrl.endsWith("/")
                    ? backendBaseUrl.substring(0, backendBaseUrl.length() - 1)
                    : backendBaseUrl;
            variables.put("invoiceDownloadUrl", normalizedBase + "/api/invoices/download/" + invoiceId);
        }
        
        sendHtmlEmail(
            toEmail,
            "MediConnect — Subscription Confirmed ✅",
            "subscription-confirmed.html",
            variables
        );
    }

    /**
     * Send subscription cancellation confirmation
     */
    @Async
    public void sendCancellationConfirmation(String toEmail, String planName) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", "Valued Customer");
            variables.put("planName", planName);
            variables.put("cancellationDate", java.time.LocalDate.now().toString());

            sendHtmlEmail(
                toEmail,
                "MediConnect — Subscription Cancelled",
                "subscription-cancelled.html",
                variables
            );
        } catch (Exception ex) {
            log.error("Failed to send cancellation email to {}", toEmail, ex);
        }
    }

    /**
     * Send payment failure notification
     */
    public void sendPaymentFailureEmail(String toEmail) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", "Valued Customer");
        variables.put("planName", "Your Plan");
        variables.put("reason", "Your payment could not be processed. Please try again.");
        
        sendHtmlEmail(
            toEmail,
            "Payment Failed - MediConnect",
            "payment-failed.html",
            variables
        );
    }

    /**
     * Send payment success email (simplified version)
     */
    public void sendPaymentSuccessEmail(String toEmail, String planName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", "Valued Customer");
        variables.put("planName", planName);
        
        sendHtmlEmail(
            toEmail,
            "Payment Successful - Welcome to " + planName,
            "payment-success.html",
            variables
        );
    }

    /**
     * student verification
     */
    private void sendHtmlEmail(String toEmail, String subject, String templateName, Map<String, Object> variables) {
        try {
            variables.putIfAbsent("logoUrl", appLogoUrl);
            String htmlContent = emailTemplateService.renderTemplate(templateName, variables);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML content
            helper.addInline("app-logo", new ByteArrayResource(getInlineLogoPng()), "image/png");

            mailSender.send(message);
            log.info("Email sent successfully to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send email to: {} with template: {}", toEmail, templateName, e);
            throw new RuntimeException("Unable to send email", e);
        }
    }

    private synchronized byte[] getInlineLogoPng() {
        if (inlineLogoPng != null) {
            return inlineLogoPng;
        }

        try {
            int width = 260;
            int height = 64;
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setColor(new Color(245, 250, 255));
            g.fillRoundRect(0, 0, width, height, 18, 18);

            g.setColor(new Color(191, 214, 238));
            g.setStroke(new BasicStroke(1.4f));
            g.drawRoundRect(1, 1, width - 3, height - 3, 18, 18);

            g.setColor(new Color(15, 118, 110));
            g.fillRoundRect(12, 12, 40, 40, 10, 10);

            g.setColor(Color.WHITE);
            g.fillRoundRect(29, 20, 6, 24, 3, 3);
            g.fillRoundRect(21, 28, 22, 6, 3, 3);

            g.setColor(new Color(15, 39, 66));
            g.setFont(new Font("SansSerif", Font.BOLD, 22));
            g.drawString("MediConnect", 62, 41);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            inlineLogoPng = out.toByteArray();
            return inlineLogoPng;
        } catch (Exception e) {
            log.warn("Failed to generate inline email logo. Falling back to empty image.", e);
            inlineLogoPng = new byte[0];
            return inlineLogoPng;
        }
    }


    public void sendStudentVerificationApprovedEmail(String toEmail, String fullName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", fullName == null || fullName.isBlank() ? "Student" : fullName);
        sendHtmlEmail(
            toEmail,
            "MediConnect - Student Verification Approved",
            "student-verification-approved.html",
            variables
        );
    }

    public void sendStudentVerificationRejectedEmail(String toEmail, String fullName, String reason) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", fullName == null || fullName.isBlank() ? "Student" : fullName);
        variables.put("reason", reason == null || reason.isBlank() ? "Verification requirements were not fully met." : reason);
        sendHtmlEmail(
            toEmail,
            "MediConnect - Student Verification Update",
            "student-verification-rejected.html",
            variables
        );
    }

    public void sendStudentVerificationReceivedEmail(String toEmail, String fullName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("fullName", fullName == null || fullName.isBlank() ? "Student" : fullName);
        sendHtmlEmail(
            toEmail,
            "MediConnect - Verification Request Received",
            "student-verification-received.html",
            variables
        );
    }

    /**
     * Send notification when an admin deletes a plan
     * Informs users that their subscription has been cancelled due to plan deletion
     */
    @Async
    public void sendPlanDeletionNotification(String toEmail, String planName) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", "Valued Customer");
            variables.put("planName", planName != null ? planName : "Your Plan");
            variables.put("cancellationDate", java.time.LocalDate.now().toString());
            variables.put("reason", "The plan has been removed from our system.");

            sendHtmlEmail(
                toEmail,
                "MediConnect — Subscription Cancelled Due to Plan Removal",
                "subscription-cancelled.html",
                variables
            );
            log.info("Plan deletion notification sent to: {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send plan deletion notification to {}", toEmail, ex);
        }
    }

    /**
     * Send notification when an admin deletes a promo code
     * Informs users that their discount code has been removed
     */
    @Async
    public void sendPromoDeletionNotification(String toEmail, String promoCode) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", "Valued Customer");
            variables.put("promoCode", promoCode != null ? promoCode : "Your Promo Code");
            variables.put("cancellationDate", java.time.LocalDate.now().toString());
            variables.put("reason", "The promo code has been discontinued.");

            sendHtmlEmail(
                toEmail,
                "MediConnect — Discount Code Discontinued",
                "subscription-cancelled.html",
                variables
            );
            log.info("Promo code deletion notification sent to: {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send promo code deletion notification to {}", toEmail, ex);
        }
    }

    /**
     * Send notification when admin deactivates a plan with credit compensation
     */
    @Async
    public void sendPlanDeprecationNotification(String toEmail, String planName, java.math.BigDecimal creditAmount, java.time.LocalDate creditExpiresAt) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", "Valued Customer");
            variables.put("planName", planName != null ? planName : "Your Plan");
            variables.put("creditAmount", creditAmount != null ? creditAmount.toPlainString() : "0.00");
            variables.put("creditExpiresAt", creditExpiresAt != null ? creditExpiresAt.toString() : "");
            variables.put("cancellationDate", java.time.LocalDate.now().toString());

            sendHtmlEmail(
                toEmail,
                "MediConnect — Your Plan Has Been Discontinued",
                "plan-deactivation.html",
                variables
            );
            log.info("Plan deactivation notification sent to: {} with credit amount: {}", toEmail, creditAmount);
        } catch (Exception ex) {
            log.error("Failed to send plan deactivation notification to {}", toEmail, ex);
        }
    }

    /**
     * Send notification when user credit expires
     */
    @Async
    public void sendCreditExpiryNotification(String toEmail, java.math.BigDecimal expiredAmount) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", "Valued Customer");
            variables.put("amount", expiredAmount != null ? expiredAmount.toPlainString() : "0.00");
            variables.put("expiryDate", java.time.LocalDate.now().toString());

            sendHtmlEmail(
                toEmail,
                "MediConnect — Your Credit Has Expired",
                "subscription-cancelled.html",
                variables
            );
            log.info("Credit expiry notification sent to: {} for amount: {}", toEmail, expiredAmount);
        } catch (Exception ex) {
            log.error("Failed to send credit expiry notification to {}", toEmail, ex);
        }
    }

    /**
     * Send confirmation when user subscribes with credit applied
     */
    @Async
    public void sendSubscriptionWithCreditConfirmation(
            String toEmail,
            String planName,
            java.math.BigDecimal amountPaid,
            java.math.BigDecimal creditApplied,
            java.math.BigDecimal remainingCredit,
            java.time.LocalDate subscriptionStart,
            java.time.LocalDate subscriptionEnd
    ) {
        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("fullName", "Valued Customer");
            variables.put("planName", planName != null ? planName : "Your Plan");
            variables.put("amountPaid", amountPaid != null ? amountPaid.toPlainString() : "0.00");
            variables.put("creditApplied", creditApplied != null ? creditApplied.toPlainString() : "0.00");
            variables.put("remainingCredit", remainingCredit != null ? remainingCredit.toPlainString() : "0.00");
            variables.put("subscriptionStart", subscriptionStart != null ? subscriptionStart.toString() : "");
            variables.put("subscriptionEnd", subscriptionEnd != null ? subscriptionEnd.toString() : "");

            sendHtmlEmail(
                toEmail,
                "MediConnect — Subscription Confirmed (Credit Applied)",
                "subscription-confirmed.html",
                variables
            );
            log.info("Subscription with credit confirmation sent to: {}", toEmail);
        } catch (Exception ex) {
            log.error("Failed to send subscription with credit confirmation to {}", toEmail, ex);
        }
    }

    public void sendBirthdayPromoEmail(AppUser user, String promoCode) {
        try {
            String subject = "🎂 Happy Birthday " + user.getFirstName() + "! Here's your gift from MediConnect";
            String htmlBody = "<div style='font-family:sans-serif;max-width:600px;margin:auto;padding:32px'>"
                    + "<h2 style='color:#0f766e'>Happy Birthday, " + user.getFirstName() + "! 🎉</h2>"
                    + "<p>As a gift from the MediConnect team, here's an exclusive <strong>15% discount</strong> on any plan.</p>"
                    + "<div style='background:#f0fdfa;border:1px solid #99f6e4;border-radius:12px;padding:24px;text-align:center;margin:24px 0'>"
                    + "<p style='margin:0;font-size:13px;color:#0f766e;font-weight:600;letter-spacing:.1em;text-transform:uppercase'>Your promo code</p>"
                    + "<h1 style='margin:8px 0;color:#0f172a;letter-spacing:.15em'>" + promoCode + "</h1>"
                    + "<p style='margin:0;font-size:12px;color:#64748b'>Valid for 7 days · 1 use only</p>"
                    + "</div>"
                    + "<p>Wishing you a wonderful birthday!</p>"
                    + "<p style='color:#64748b;font-size:12px'>— The MediConnect Team</p>"
                    + "</div>";

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            log.info("Birthday promo email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send birthday promo email to: {}", user.getEmail(), e);
        }
    }
}