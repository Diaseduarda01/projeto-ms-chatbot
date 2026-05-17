package ms.chatbot.dias.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.domain.entity.Message;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.infrastructure.web.dto.InboxEvent;
import ms.chatbot.dias.infrastructure.web.dto.MessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEventPublisher {

    private final SimpMessagingTemplate messaging;

    public void publishMessage(Message message) {
        String topic = "/topic/sessions/" + message.getSessionId();
        messaging.convertAndSend(topic, MessageResponse.from(message));
        log.debug("Evento publicado em {} | direção: {}", topic, message.getDirection());
    }

    public void publishInboxEvent(Session session, String eventType) {
        InboxEvent event = new InboxEvent(
            session.getId(),
            session.getPhoneNumber(),
            session.getStatus(),
            session.getCurrentStepKey(),
            session.getLastActivity(),
            eventType
        );
        messaging.convertAndSend("/topic/inbox", event);
        log.debug("Evento de inbox publicado | sessão: {} | tipo: {}", session.getId(), eventType);
    }
}
