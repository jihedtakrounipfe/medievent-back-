package skylinkers.tn.mediconnectbackend.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppNotificationDTO {
    private Long id;
    private String title;
    private String message;
    private Long eventId;
    private String type;
    private LocalDateTime timestamp;
    private boolean isRead;
}
