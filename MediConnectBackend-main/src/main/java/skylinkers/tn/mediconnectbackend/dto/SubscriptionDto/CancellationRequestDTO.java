package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CancellationRequestDTO {
    private Long subscriptionId;

    @Size(max = 500, message = "reason must not exceed 500 characters")
    private String reason; // optional, can be empty
}

