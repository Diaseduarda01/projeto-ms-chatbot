package ms.chatbot.dias.application.service;

import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.FlowTransition;
import ms.chatbot.dias.domain.port.FlowStepRepository;
import ms.chatbot.dias.infrastructure.web.dto.UpdateFlowStepRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FlowBuilderService {

    private final FlowStepRepository flowStepRepository;

    @Transactional
    public FlowStep updateStep(UUID stepId, UpdateFlowStepRequest request) {
        FlowStep step = flowStepRepository.findById(stepId)
            .orElseThrow(() -> new RuntimeException("Step não encontrado: " + stepId));

        step.setType(request.type());
        step.setMessageTemplate(request.messageTemplate());
        step.setInputType(request.inputType());
        step.setSessionDataKey(request.sessionDataKey());
        step.setDefaultNextStepKey(request.defaultNextStepKey());
        step.setActionType(request.actionType());

        List<FlowTransition> newTransitions = request.transitions() == null ? List.of() :
            request.transitions().stream()
                .map(t -> FlowTransition.builder()
                    .trigger(t.trigger())
                    .nextStepKey(t.nextStepKey())
                    .sortOrder(t.sortOrder())
                    .build())
                .toList();

        step.getTransitions().clear();
        step.getTransitions().addAll(newTransitions);

        return flowStepRepository.save(step);
    }
}
