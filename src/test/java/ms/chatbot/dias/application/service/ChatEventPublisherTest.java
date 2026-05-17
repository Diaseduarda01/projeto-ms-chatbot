package ms.chatbot.dias.application.service;

import ms.chatbot.dias.domain.entity.Message;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.MessageDirection;
import ms.chatbot.dias.domain.enums.SessionStatus;
import ms.chatbot.dias.infrastructure.web.dto.InboxEvent;
import ms.chatbot.dias.infrastructure.web.dto.MessageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ChatEventPublisherTest {

    @Mock SimpMessagingTemplate messaging;

    @InjectMocks
    ChatEventPublisher publisher;

    @Test
    void publishMessage_envia_para_topico_correto_com_payload_correto() {
        UUID sessionId = UUID.randomUUID();
        Message message = Message.builder()
            .sessionId(sessionId)
            .companyId(UUID.randomUUID())
            .phoneNumber("5511999999999")
            .direction(MessageDirection.INBOUND)
            .text("Olá")
            .build();

        publisher.publishMessage(message);

        var topicCaptor = ArgumentCaptor.forClass(String.class);
        var payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messaging).convertAndSend(topicCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("/topic/sessions/" + sessionId);
        assertThat(payloadCaptor.getValue()).isInstanceOf(MessageResponse.class);

        MessageResponse response = (MessageResponse) payloadCaptor.getValue();
        assertThat(response.direction()).isEqualTo(MessageDirection.INBOUND);
        assertThat(response.text()).isEqualTo("Olá");
    }

    @Test
    void publishInboxEvent_envia_para_topico_inbox_com_payload_correto() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        session.activate();

        publisher.publishInboxEvent(session, InboxEvent.SESSION_STARTED);

        var topicCaptor = ArgumentCaptor.forClass(String.class);
        var payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messaging).convertAndSend(topicCaptor.capture(), payloadCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo("/topic/inbox");
        assertThat(payloadCaptor.getValue()).isInstanceOf(InboxEvent.class);

        InboxEvent event = (InboxEvent) payloadCaptor.getValue();
        assertThat(event.sessionId()).isEqualTo(session.getId());
        assertThat(event.phoneNumber()).isEqualTo("5511999999999");
        assertThat(event.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(event.eventType()).isEqualTo(InboxEvent.SESSION_STARTED);
    }
}
