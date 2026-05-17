package ms.chatbot.dias.infrastructure.web.dto;

import ms.chatbot.dias.domain.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record InboxEvent(
    UUID sessionId,
    String phoneNumber,
    SessionStatus status,
    String currentStepKey,
    LocalDateTime lastActivity,
    String eventType
) {
    public static final String SESSION_STARTED  = "SESSION_STARTED";
    public static final String MESSAGE_RECEIVED = "MESSAGE_RECEIVED";
    public static final String STATUS_CHANGED   = "STATUS_CHANGED";
}
