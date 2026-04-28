package skylinkers.tn.mediconnectbackend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RiskScoreResponse(
        @JsonProperty("risk_score")     int    riskScore,
        @JsonProperty("decision")       String decision,
        @JsonProperty("model_version")  String modelVersion
) {}
