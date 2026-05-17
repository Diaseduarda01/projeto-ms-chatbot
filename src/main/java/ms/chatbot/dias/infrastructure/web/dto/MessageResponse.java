package ms.chatbot.dias.infrastructure.web.dto;

import ms.chatbot.dias.domain.entity.Message;
import ms.chatbot.dias.domain.enums.MessageDirection;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
    UUID id,
    MessageDirection direction,
    String text,
    LocalDateTime createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
            message.getId(),
            message.getDirection(),
            message.getText(),
            message.getCreatedAt()
        );
    }
}
