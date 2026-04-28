package skylinkers.tn.mediconnectbackend.utils;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.ITemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;
    private final AtomicBoolean missingCredsWarned = new AtomicBoolean(false);

    @Value("${mediconnect.mail.from-name:MediConnect}")
    private String fromName;

    @Value("${mediconnect.mail.from-address:noreply@mediconnect.tn}")
    private String fromAddress;

    @Value("${mediconnect.mail.enabled:true}")
    private boolean enabled;

    public EmailService(JavaMailSender mailSender, ITemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Sends an HTML email using a Thymeleaf template.
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!enabled) return;
        if (!isMailConfigured()) return;
        try {
            Context ctx = new Context();
            if (variables != null) {
                variables.forEach(ctx::setVariable);
            }
            String html = templateEngine.process("email/" + templateName, ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected email error to {}", to, e);
        }
    }

    /**
     * Sends a plain text email.
     */
    @Async
    public void sendSimpleEmail(String to, String subject, String body) {
        if (!enabled) return;
        if (!isMailConfigured()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}", to, e);
        }
    }

    /**
     * Sends a 6-digit verification code email.
     */
    @Async
    public void sendVerificationCode(String to, String firstName, String code) {
        sendHtmlEmail(
                to,
                "Vérification de votre email",
                "verification-code.html",
                Map.of("firstName", firstName, "code", code)
        );
    }

    /**
     * Sends a welcome email after registration.
     */
    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        sendHtmlEmail(to, "Bienvenue sur MediConnect", "welcome.html", Map.of("firstName", firstName));
    }

    /**
     * Sends approval notification for doctor accounts.
     */
    @Async
    public void sendDoctorApprovedEmail(String to, String doctorName) {
        sendHtmlEmail(to, "Votre compte médecin est approuvé", "doctor-approved.html", Map.of("doctorName", doctorName));
    }

    /**
     * Sends rejection notification for doctor accounts.
     */
    @Async
    public void sendDoctorRejectedEmail(String to, String doctorName, String reason) {
        sendHtmlEmail(to, "Votre compte médecin a été rejeté", "doctor-rejected.html", Map.of("doctorName", doctorName, "reason", reason));
    }

    /**
     * Sends password reset link.
     */
    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        sendHtmlEmail(to, "Réinitialisation de mot de passe", "password-reset.html", Map.of("resetLink", resetLink));
    }

    /**
     * Sends a 6-digit 2FA login code.
     * If email is not configured, logs the code to the console for dev/test.
     */
    @Async
    public void send2FACode(String to, String firstName, String code) {
        log.info("[DEV/TEST] 2FA login code for {}: {}", to, code);
        sendHtmlEmail(
                to,
                "MediConnect — Code de vérification 2FA",
                "2fa-code.html",
                Map.of("firstName", firstName, "code", code, "expiryMinutes", 5)
        );
    }

    /**
     * Sends a 6-digit password reset code.
     * If email is not configured, logs the code to the console for dev/test.
     */
    @Async
    public void sendPasswordResetCode(String to, String firstName, String code) {
        log.info("[DEV/TEST] Password reset code for {}: {}", to, code);
        sendHtmlEmail(
                to,
                "Réinitialisation de mot de passe — code de vérification",
                "password-reset-code.html",
                Map.of("firstName", firstName, "code", code)
        );
    }

    /**
     * Sends appointment reminder email.
     */
    @Async
    public void sendAppointmentReminderEmail(String to, String patientName, String doctorName, String dateTime) {
        sendHtmlEmail(to, "Rappel de rendez-vous", "appointment-reminder.html", Map.of(
                "patientName", patientName,
                "doctorName", doctorName,
                "dateTime", dateTime
        ));
    }

    /**
     * Sends a generic doctor account status change email.
     */
    @Async
    public void sendDoctorStatusChangeEmail(String to, String doctorName, String oldStatus, String newStatus, String reason) {
        sendHtmlEmail(
                to,
                "Changement de statut de votre compte médecin",
                "doctor-status-change.html",
                Map.of(
                        "doctorName", doctorName,
                        "oldStatus", oldStatus,
                        "newStatus", newStatus,
                        "reason", reason == null ? "" : reason
                )
        );
    }

    /**
     * Sends the 6-digit code for the authenticated "change my password" flow.
     * Different from sendPasswordResetCode — this is for logged-in users who know
     * their current password but need to prove identity before changing it.
     */
    @Async
    public void sendPasswordChangeCode(String to, String firstName, String code) {
        log.info("[DEV/TEST] Password change verification code for {}: {}", to, code);
        sendHtmlEmail(
                to,
                "MediConnect — Code de vérification pour changer votre mot de passe",
                "password-reset-code.html",   // reuse the reset template (same structure)
                Map.of("firstName", firstName, "code", code, "expiryMinutes", 10)
        );
    }

    public void sendDoctorApprovalEmail(String to, String doctorName) {
        sendDoctorApprovedEmail(to, doctorName);
    }

    public void sendDoctorRejectionEmail(String to, String doctorName, String reason) {
        sendDoctorRejectedEmail(to, doctorName, reason);
    }

    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        sendHtmlEmail(to, subject, templateName, variables);
    }

    private boolean isMailConfigured() {
        if (mailSender instanceof JavaMailSenderImpl impl) {
            String username = impl.getUsername();
            String password = impl.getPassword();
            boolean ok = username != null && !username.isBlank() && password != null && !password.isBlank();
            if (!ok && missingCredsWarned.compareAndSet(false, true)) {
                log.warn("Mail is enabled but spring.mail.username/password are not configured. Skipping email sends.");
            }
            return ok;
        }
        return true;
    }
}
