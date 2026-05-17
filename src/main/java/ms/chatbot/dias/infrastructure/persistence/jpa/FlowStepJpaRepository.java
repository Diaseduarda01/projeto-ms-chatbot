package ms.chatbot.dias.infrastructure.persistence.jpa;

import ms.chatbot.dias.domain.entity.FlowStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlowStepJpaRepository extends JpaRepository<FlowStep, UUID> {
    Optional<FlowStep> findByCompanyIdAndStepKey(UUID companyId, String stepKey);
    List<FlowStep> findAllByCompanyId(UUID companyId);
    void deleteAllByCompanyId(UUID companyId);
}
