package ms.chatbot.dias.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.port.SessionRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.SessionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SessionJpaAdapter implements SessionRepository {

    private final SessionJpaRepository jpa;

    @Override
    public Optional<Session> findByCompanyIdAndPhoneNumber(UUID companyId, String phoneNumber) {
        return jpa.findByCompanyIdAndPhoneNumber(companyId, phoneNumber);
    }

    @Override
    public Session save(Session session) {
        return jpa.save(session);
    }
}
