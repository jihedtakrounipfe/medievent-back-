package skylinkers.tn.mediconnectbackend.utils;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import skylinkers.tn.mediconnectbackend.exception.RecaptchaException;

import java.util.List;

/**
 * Verifies Google reCAPTCHA v2 tokens submitted by the frontend.
 *
 * Set google.recaptcha.enabled=false in application.yml to bypass during development.
 * In production, always set RECAPTCHA_SECRET_KEY as an environment variable.
 */
@Slf4j
@Service
public class RecaptchaService {

    private record RecaptchaResponse(
            boolean success,
            @JsonProperty("challenge_ts") String challengeTs,
            String hostname,
            @JsonProperty("error-codes") List<String> errorCodes
    ) {}

    private final RestClient restClient;
    private final String recaptchaSecret;
    private final String verifyUrl;
    private final boolean enabled;

    public RecaptchaService(
            @Value("${google.recaptcha.secret}") String recaptchaSecret,
            @Value("${google.recaptcha.verify-url}") String verifyUrl,
            @Value("${google.recaptcha.enabled:true}") boolean enabled
    ) {
        this.restClient = RestClient.builder().build();
        this.recaptchaSecret = recaptchaSecret;
        this.verifyUrl = verifyUrl;
        this.enabled = enabled;
    }

    /**
     * Verify the reCAPTCHA token from the frontend.
     *
     * @param token the g-recaptcha-response token from the client
     * @throws RecaptchaException if the token is missing or invalid
     */
    public void verify(String token) {
        if (!enabled) {
            log.warn("[RECAPTCHA] Verification disabled — skipping");
            return;
        }

        if (token == null || token.isBlank()) {
            throw new RecaptchaException("Le captcha est requis");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", recaptchaSecret);
        form.add("response", token);

        RecaptchaResponse response = restClient.post()
                .uri(verifyUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(RecaptchaResponse.class);

        if (response == null || !response.success()) {
            String errors = response != null && response.errorCodes() != null
                    ? String.join(", ", response.errorCodes())
                    : "unknown";
            log.warn("[RECAPTCHA] Verification failed: {}", errors);
            throw new RecaptchaException("Vérification reCAPTCHA échouée. Veuillez réessayer.");
        }

        log.debug("[RECAPTCHA] Token verified successfully for hostname={}", response.hostname());
    }
}
