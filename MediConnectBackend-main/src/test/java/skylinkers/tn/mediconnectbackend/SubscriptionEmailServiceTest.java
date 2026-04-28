package skylinkers.tn.mediconnectbackend;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import skylinkers.tn.mediconnectbackend.utils.EmailService;

import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;
import java.util.Properties;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubscriptionEmailServiceTest {

    @Test
    void sendEmailRendersTemplateAndSendsMimeMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);

        MimeMessage mime = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);
        when(templateEngine.process(eq("email/welcome.html"), any())).thenReturn("<html>ok</html>");

        EmailService svc = new EmailService(mailSender, templateEngine);
        ReflectionTestUtils.setField(svc, "enabled", true);
        ReflectionTestUtils.setField(svc, "fromName", "MediConnect");
        ReflectionTestUtils.setField(svc, "fromAddress", "noreply@mediconnect.tn");

        svc.sendEmail("a@b.com", "sub", "welcome.html", Map.of("firstName", "A"));

        verify(templateEngine, times(1)).process(eq("email/welcome.html"), any());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
