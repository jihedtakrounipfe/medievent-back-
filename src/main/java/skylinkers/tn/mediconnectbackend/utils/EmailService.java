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
            log.info("Sending HTML email to: {} with subject: '{}'", to, subject);
            mailSender.send(message);
            log.info("SUCCESS: HTML Email successfully sent to: {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected email error to {}", to, e);
        }
    }

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
            log.info("Sending simple email to: {} with subject: '{}'", to, subject);
            mailSender.send(msg);
            log.info("SUCCESS: Simple Email successfully sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to {}", to, e);
        }
    }

    @Async
    public void sendVerificationCode(String to, String firstName, String code) {
        sendHtmlEmail(to, "Vérification de votre email", "verification-code.html", Map.of("firstName", firstName, "code", code));
    }

    @Async
    public void sendWelcomeEmail(String to, String firstName) {
        sendHtmlEmail(to, "Bienvenue sur MediConnect", "welcome.html", Map.of("firstName", firstName));
    }

    @Async
    public void sendDoctorApprovedEmail(String to, String doctorName) {
        sendHtmlEmail(to, "Votre compte médecin est approuvé", "doctor-approved.html", Map.of("doctorName", doctorName));
    }

    @Async
    public void sendDoctorRejectedEmail(String to, String doctorName, String reason) {
        sendHtmlEmail(to, "Votre compte médecin a été rejeté", "doctor-rejected.html", Map.of("doctorName", doctorName, "reason", reason));
    }

    @Async
    public void sendPasswordResetEmail(String to, String resetLink) {
        sendHtmlEmail(to, "Réinitialisation de mot de passe", "password-reset.html", Map.of("resetLink", resetLink));
    }

    @Async
    public void sendGuestInvitationEmail(String to, String guestName, String doctorName, String eventTitle, String eventDate, String joinUrl) {
        sendHtmlEmail(to, "🩺 Invitation — " + eventTitle, "guest-invitation.html", Map.of(
                "guestName", guestName,
                "doctorName", doctorName,
                "eventTitle", eventTitle,
                "eventDate", eventDate,
                "joinUrl", joinUrl
        ));
    }

    @Async
    public void sendAppointmentReminderEmail(String to, String patientName, String doctorName, String dateTime) {
        sendHtmlEmail(to, "Rappel de rendez-vous", "appointment-reminder.html", Map.of(
                "patientName", patientName,
                "doctorName", doctorName,
                "dateTime", dateTime
        ));
    }

    @Async
    public void sendDoctorStatusChangeEmail(String to, String doctorName, String oldStatus, String newStatus, String reason) {
        sendHtmlEmail(to, "Changement de statut de votre compte médecin", "doctor-status-change.html", Map.of(
                "doctorName", doctorName,
                "oldStatus", oldStatus,
                "newStatus", newStatus,
                "reason", reason == null ? "" : reason
        ));
    }

    @Async
    public void sendParticipationConfirmedEmail(String to, String userName, String eventTitle, String eventDate, String eventLocation, String eventUrl) {
        sendHtmlEmail(to, "✅ Inscription Confirmée : " + eventTitle, "participation-confirmed.html", Map.of(
                "userName", userName,
                "eventTitle", eventTitle,
                "eventDate", eventDate,
                "eventLocation", eventLocation == null ? "En ligne" : eventLocation,
                "eventUrl", eventUrl
        ));
    }

    @Async
    public void sendParticipationWaitlistEmail(String to, String userName, String eventTitle, String eventDate, String eventUrl) {
        sendHtmlEmail(to, "⏳ En Liste d'Attente : " + eventTitle, "participation-waitlist.html", Map.of(
                "userName", userName,
                "eventTitle", eventTitle,
                "eventDate", eventDate,
                "eventUrl", eventUrl
        ));
    }

    @Async
    public void sendParticipationPromotedEmail(String to, String userName, String eventTitle, String eventDate, String eventLocation, String eventUrl) {
        sendHtmlEmail(to, "🌟 Place Confirmée : " + eventTitle, "participation-promoted.html", Map.of(
                "userName", userName,
                "eventTitle", eventTitle,
                "eventDate", eventDate,
                "eventLocation", eventLocation == null ? "En ligne" : eventLocation,
                "eventUrl", eventUrl
        ));
    }

    // ── NEW: Event cancelled — notify all enrolled participants ───────────────
    @Async
    public void sendEventCancelledEmail(String to, String userName, String eventTitle, String eventDate) {
        sendHtmlEmail(to, "❌ Événement annulé : " + eventTitle, "event-cancelled.html", Map.of(
                "userName", userName,
                "eventTitle", eventTitle,
                "eventDate", eventDate
        ));
    }

    // ── NEW: 30-minute pre-event reminder ─────────────────────────────────────
    @Async
    public void sendEventReminderEmail(String to, String userName, String eventTitle, String eventDate, String eventUrl) {
        sendHtmlEmail(to, "⏰ Rappel : " + eventTitle + " commence dans 30 min !", "event-reminder.html", Map.of(
                "userName", userName,
                "eventTitle", eventTitle,
                "eventDate", eventDate,
                "eventUrl", eventUrl
        ));
    }

    // ── NEW: Event has started — join now ─────────────────────────────────────
    @Async
    public void sendEventStartedEmail(String to, String userName, String eventTitle, String eventUrl) {
        sendHtmlEmail(to, "🚀 C'est parti ! " + eventTitle + " vient de commencer", "event-started.html", Map.of(
                "userName", userName,
                "eventTitle", eventTitle,
                "eventUrl", eventUrl
        ));
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
