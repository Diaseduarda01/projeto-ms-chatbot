package ms.chatbot.dias.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.entity.Message;
import ms.chatbot.dias.domain.port.MessageRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.MessageJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageJpaAdapter implements MessageRepository {

    private final MessageJpaRepository jpa;

    @Override
    public Message save(Message message) {
        return jpa.save(message);
    }

    @Override
    public Page<Message> findBySessionId(UUID sessionId, Pageable pageable) {
        return jpa.findBySessionIdOrderByCreatedAtAsc(sessionId, pageable);
    }

    @Override
    public Optional<Message> findLastBySessionId(UUID sessionId) {
        return jpa.findTopBySessionIdOrderByCreatedAtDesc(sessionId);
    }
}
