package skylinkers.tn.mediconnectbackend.controller.AppointmentController;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "http://localhost:4200")
public class AiProxyController {

    @Value("${groq.api.key}")
    private String apiKey;

    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody Map<String, Object> body) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            String systemPrompt = (String) body.getOrDefault("system", "");
            List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");

            List<Map<String, Object>> groqMessages = new ArrayList<>();

            if (!systemPrompt.isEmpty()) {
                groqMessages.add(Map.of("role", "system", "content", systemPrompt));
            }
            groqMessages.addAll(messages);

            Map<String, Object> groqBody = Map.of(
                    "model", "llama-3.3-70b-versatile",
                    "messages", groqMessages,
                    "max_tokens", 1024
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(groqBody, headers);

            return restTemplate.postForEntity(
                    "https://api.groq.com/openai/v1/chat/completions",
                    request,
                    String.class
            );
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}