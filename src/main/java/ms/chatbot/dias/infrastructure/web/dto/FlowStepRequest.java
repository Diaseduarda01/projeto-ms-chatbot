package ms.chatbot.dias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ms.chatbot.dias.domain.enums.ActionType;
import ms.chatbot.dias.domain.enums.InputType;
import ms.chatbot.dias.domain.enums.StepType;

import java.util.List;

public record FlowStepRequest(
    @NotBlank String stepKey,
    @NotNull StepType type,
    @NotBlank String messageTemplate,
    InputType inputType,
    String sessionDataKey,
    String defaultNextStepKey,
    ActionType actionType,
    List<FlowTransitionRequest> transitions
) {
    public record FlowTransitionRequest(
        @NotBlank String trigger,
        @NotBlank String nextStepKey,
        int sortOrder
    ) {}
}
