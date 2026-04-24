package skylinkers.tn.mediconnectbackend.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Controller
public class ChatController {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatMessage {
        private Long senderId;
        private String sender;
        private String content;
        private String timestamp;
        private String role;
        private String replyTo;
        private String replyToSender;
    }

    /**
     * Broadcasts a message to all participants in a specific event room.
     */
    @MessageMapping("/chat/{eventId}/send")
    @SendTo("/topic/chat/{eventId}")
    public ChatMessage sendMessage(@DestinationVariable String eventId, @Payload ChatMessage message) {
        message.setTimestamp(LocalDateTime.now().toString());
        return message;
    }
}
