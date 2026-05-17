package ms.chatbot.dias.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Value("${chatbot.internal-api-key}")
    private String internalApiKey;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String provided = accessor.getFirstNativeHeader("X-Internal-Key");
            if (!internalApiKey.equals(provided)) {
                log.warn("WebSocket CONNECT rejeitado: chave interna inválida ou ausente");
                throw new MessageDeliveryException(message, "Chave interna inválida ou ausente");
            }
            log.debug("WebSocket CONNECT autenticado com sucesso");
        }

        return message;
    }
}
