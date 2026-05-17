package ms.chatbot.dias.infrastructure.web.dto;

import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.FlowTransition;
import ms.chatbot.dias.domain.enums.ActionType;
import ms.chatbot.dias.domain.enums.InputType;
import ms.chatbot.dias.domain.enums.StepType;

import java.util.List;

public record FlowStepExport(
    String stepKey,
    StepType type,
    String messageTemplate,
    InputType inputType,
    String sessionDataKey,
    String defaultNextStepKey,
    ActionType actionType,
    List<TransitionExport> transitions
) {
    public record TransitionExport(String trigger, String nextStepKey, int sortOrder) {}

    public static FlowStepExport from(FlowStep step) {
        List<TransitionExport> trans = step.getTransitions() == null ? List.of() :
            step.getTransitions().stream()
                .map(t -> new TransitionExport(t.getTrigger(), t.getNextStepKey(), t.getSortOrder()))
                .toList();
        return new FlowStepExport(
            step.getStepKey(), step.getType(), step.getMessageTemplate(),
            step.getInputType(), step.getSessionDataKey(), step.getDefaultNextStepKey(),
            step.getActionType(), trans
        );
    }
}
