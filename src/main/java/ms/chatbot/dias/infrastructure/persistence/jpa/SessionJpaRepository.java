package ms.chatbot.dias.infrastructure.persistence.jpa;

import ms.chatbot.dias.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionJpaRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByCompanyIdAndPhoneNumber(UUID companyId, String phoneNumber);
}
