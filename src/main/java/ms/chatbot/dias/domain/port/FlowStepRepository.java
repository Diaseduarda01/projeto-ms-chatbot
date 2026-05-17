package ms.chatbot.dias.domain.port;

import ms.chatbot.dias.domain.entity.FlowStep;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlowStepRepository {
    Optional<FlowStep> findByCompanyIdAndStepKey(UUID companyId, String stepKey);
    Optional<FlowStep> findById(UUID id);
    FlowStep save(FlowStep step);
    List<FlowStep> saveAll(List<FlowStep> steps);
    List<FlowStep> findAllByCompanyId(UUID companyId);
    void deleteById(UUID id);
    void deleteAllByCompanyId(UUID companyId);
}
