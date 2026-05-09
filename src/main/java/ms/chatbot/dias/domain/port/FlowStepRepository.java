package ms.chatbot.dias.domain.port;

import ms.chatbot.dias.domain.entity.FlowStep;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FlowStepRepository {
    Optional<FlowStep> findByCompanyIdAndStepKey(UUID companyId, String stepKey);
    FlowStep save(FlowStep step);
    List<FlowStep> findAllByCompanyId(UUID companyId);
}
