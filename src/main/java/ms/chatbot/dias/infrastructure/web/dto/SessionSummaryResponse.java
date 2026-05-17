package ms.chatbot.dias.infrastructure.web.dto;

import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.SessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SessionSummaryResponse(
    UUID id,
    String phoneNumber,
    SessionStatus status,
    String currentStepKey,
    LocalDateTime lastActivity,
    MessageResponse lastMessage
) {
    public static SessionSummaryResponse from(Session session, MessageResponse lastMessage) {
        return new SessionSummaryResponse(
            session.getId(),
            session.getPhoneNumber(),
            session.getStatus(),
            session.getCurrentStepKey(),
            session.getLastActivity(),
            lastMessage
        );
    }
}
