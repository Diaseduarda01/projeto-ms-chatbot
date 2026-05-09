package ms.chatbot.dias.domain.exception;

import java.util.UUID;

public class FlowStepNotFoundException extends RuntimeException {
    public FlowStepNotFoundException(UUID companyId, String stepKey) {
        super("Step não encontrado: companyId=" + companyId + ", stepKey=" + stepKey);
    }
}
