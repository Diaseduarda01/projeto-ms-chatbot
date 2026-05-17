package ms.chatbot.dias.domain.port;

import ms.chatbot.dias.domain.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface MessageRepository {
    Message save(Message message);
    Page<Message> findBySessionId(UUID sessionId, Pageable pageable);
    Optional<Message> findLastBySessionId(UUID sessionId);
}
