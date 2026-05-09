package ms.chatbot.dias.domain.port;

import ms.chatbot.dias.domain.entity.Session;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository {
    Optional<Session> findByCompanyIdAndPhoneNumber(UUID companyId, String phoneNumber);
    Session save(Session session);
}
