package skylinkers.tn.mediconnectbackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.risk-scorer")
public record RiskScoringProperties(
        String url,
        int timeoutMs,
        boolean enabled,
        String fallbackDecision
) {}
