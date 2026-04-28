package skylinkers.tn.mediconnectbackend.service.risk;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import skylinkers.tn.mediconnectbackend.config.RiskScoringProperties;
import skylinkers.tn.mediconnectbackend.dto.request.RiskScoreRequest;
import skylinkers.tn.mediconnectbackend.dto.response.RiskScoreResponse;

@Slf4j
@Service
public class RiskScoringClientImpl implements RiskScoringClient {

    private final RestTemplate restTemplate;
    private final RiskScoringProperties props;

    public RiskScoringClientImpl(
            @Qualifier("riskScoringRestTemplate") RestTemplate restTemplate,
            RiskScoringProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }

    @Override
    public RiskScoreResponse score(RiskScoreRequest request) {
        if (!props.enabled()) {
            log.debug("[RISK] Scorer disabled — returning fallback {}", props.fallbackDecision());
            return fallback();
        }

        try {
            String url = props.url() + "/api/v1/risk-score";
            return restTemplate.postForObject(url, request, RiskScoreResponse.class);
        } catch (Exception ex) {
            log.warn("[RISK] Risk scorer unavailable: {} — falling back to {}",
                    ex.getMessage(), props.fallbackDecision());
            return fallback();
        }
    }

    private RiskScoreResponse fallback() {
        return new RiskScoreResponse(50, props.fallbackDecision(), "fallback");
    }
}
