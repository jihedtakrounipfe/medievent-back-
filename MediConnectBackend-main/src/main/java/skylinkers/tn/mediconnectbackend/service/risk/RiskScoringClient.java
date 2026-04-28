package skylinkers.tn.mediconnectbackend.service.risk;

import skylinkers.tn.mediconnectbackend.dto.request.RiskScoreRequest;
import skylinkers.tn.mediconnectbackend.dto.response.RiskScoreResponse;

public interface RiskScoringClient {
    RiskScoreResponse score(RiskScoreRequest request);
}
