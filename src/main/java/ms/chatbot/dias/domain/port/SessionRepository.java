package ms.chatbot.dias.domain.port;

import ms.chatbot.dias.domain.entity.Session;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    Optional<Session> findByCompanyIdAndPhoneNumber(UUID companyId, String phoneNumber);
    Optional<Session> findById(UUID id);
    List<Session> findAllByCompanyId(UUID companyId);
    Session save(Session session);
}
