package ms.chatbot.dias.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.port.FlowStepRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.FlowStepJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FlowStepJpaAdapter implements FlowStepRepository {

    private final FlowStepJpaRepository jpa;

    @Override
    public Optional<FlowStep> findByCompanyIdAndStepKey(UUID companyId, String stepKey) {
        return jpa.findByCompanyIdAndStepKey(companyId, stepKey);
    }

    @Override
    public FlowStep save(FlowStep step) {
        return jpa.save(step);
    }

    @Override
    public Optional<FlowStep> findById(UUID id) {
        return jpa.findById(id);
    }

    @Override
    public List<FlowStep> findAllByCompanyId(UUID companyId) {
        return jpa.findAllByCompanyId(companyId);
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }
}
