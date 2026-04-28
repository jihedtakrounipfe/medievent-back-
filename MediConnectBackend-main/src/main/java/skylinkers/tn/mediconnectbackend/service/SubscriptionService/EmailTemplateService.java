package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailTemplateService {

    private final TemplateEngine templateEngine;

    /**
     * Render an email template with the given variables
     * @param templateName - name of the template file (e.g., "payment-success.html")
     * @param variables - map of variable values to inject into template
     * @return rendered HTML string
     */
    public String renderTemplate(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            return templateEngine.process("email/" + templateName, context);
        } catch (Exception e) {
            log.error("Failed to render email template: {}", templateName, e);
            throw new RuntimeException("Unable to render email template", e);
        }
    }
}
