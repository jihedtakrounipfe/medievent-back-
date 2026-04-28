package skylinkers.tn.mediconnectbackend.dto.SubscriptionDto;

import lombok.Data;
import java.util.List;

@Data
public class PlanChatRequestDTO {
    private Long userId;
    private List<ChatMessage> messages;
}