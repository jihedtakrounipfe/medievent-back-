package skylinkers.tn.mediconnectbackend.service.SubscriptionService;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqService {

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.1-8b-instant";
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("PRICE", "FEATURES", "UX", "OTHER");

    private final RestTemplate restTemplate;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    public String classifyCancellationReason(String userText) {
        if (userText == null || userText.trim().isEmpty()) {
            return "OTHER";
        }

        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            log.warn("Groq API key is missing; falling back to OTHER");
            return "OTHER";
        }

        String prompt = "Classify this subscription cancellation reason into exactly one of: PRICE, FEATURES, UX, OTHER. Reply with one word only. Text: "
                + userText;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(groqApiKey.trim());

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", MODEL);
            requestBody.addProperty("temperature", 0);

            JsonArray messages = new JsonArray();
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);
            messages.add(message);
            requestBody.add("messages", messages);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String response = restTemplate.postForObject(GROQ_URL, request, String.class);

            return extractCategory(response);
        } catch (Exception ex) {
            log.warn("Groq classification failed: {}", ex.getMessage());
            return "OTHER";
        }
    }

    private String extractCategory(String response) {
        if (response == null || response.isBlank()) {
            return "OTHER";
        }

        try {
            JsonObject root = JsonParser.parseString(response).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "OTHER";
            }

            JsonObject firstChoice = choices.get(0).getAsJsonObject();
            JsonObject message = firstChoice.has("message") ? firstChoice.getAsJsonObject("message") : null;
            if (message == null || !message.has("content")) {
                return "OTHER";
            }

            String text = message.get("content").getAsString();
            String normalized = text.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z]", "");

            return ALLOWED_CATEGORIES.contains(normalized) ? normalized : "OTHER";
        } catch (Exception ex) {
            log.warn("Failed to parse Groq response: {}", ex.getMessage());
            return "OTHER";
        }
    }
}