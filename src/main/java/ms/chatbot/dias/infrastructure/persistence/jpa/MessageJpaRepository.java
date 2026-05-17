package ms.chatbot.dias.infrastructure.persistence.jpa;

import ms.chatbot.dias.domain.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageJpaRepository extends JpaRepository<Message, UUID> {
    Page<Message> findBySessionIdOrderByCreatedAtAsc(UUID sessionId, Pageable pageable);
    Optional<Message> findTopBySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
