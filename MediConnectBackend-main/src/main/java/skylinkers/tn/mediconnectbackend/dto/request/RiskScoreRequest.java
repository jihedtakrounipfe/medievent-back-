package skylinkers.tn.mediconnectbackend.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RiskScoreRequest(
        @JsonProperty("user_id")            String  userId,
        @JsonProperty("hour")               int     hour,
        @JsonProperty("is_weekend")         int     isWeekend,
        @JsonProperty("known_device")       int     knownDevice,
        @JsonProperty("known_ip")           int     knownIp,
        @JsonProperty("failed_attempts_15min") int  failedAttempts15min,
        @JsonProperty("time_anomaly")       double  timeAnomaly,
        @JsonProperty("user_base_risk")     double  userBaseRisk
) {}
