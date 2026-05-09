package ms.chatbot.dias.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.FlowTransition;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.StepType;
import ms.chatbot.dias.domain.exception.FlowStepNotFoundException;
import ms.chatbot.dias.domain.port.FlowStepRepository;
import ms.chatbot.dias.domain.port.MessagingGateway;
import ms.chatbot.dias.domain.port.SessionRepository;
import ms.chatbot.dias.infrastructure.evolution.MessagingGatewayFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlowEngineService {

    private final FlowStepRepository flowStepRepository;
    private final SessionRepository sessionRepository;
    private final MessagingGatewayFactory gatewayFactory;
    private final InputValidator inputValidator;
    private final TemplateRenderer templateRenderer;

    @Transactional
    public void process(Session session, Company company, String userInput, String senderName) {
        FlowStep currentStep = loadStep(company, session.getCurrentStepKey());

        String nextStepKey = resolveNextStep(currentStep, userInput, session, company);

        if (nextStepKey == null) {
            sendInvalidOptionMessage(session, company);
            return;
        }

        if (currentStep.getType() == StepType.INPUT
                && currentStep.getSessionDataKey() != null
                && currentStep.getInputType() != null) {
            String normalized = inputValidator.normalize(currentStep.getInputType(), userInput);
            session.storeData(currentStep.getSessionDataKey(), normalized);
        }

        session.setCurrentStepKey(nextStepKey);
        session.activate();
        sessionRepository.save(session);

        FlowStep nextStep = loadStep(company, nextStepKey);
        sendStep(nextStep, session, company, senderName);

        if (nextStep.getType() == StepType.END) {
            session.complete();
            sessionRepository.save(session);
        }
    }

    @Transactional
    public void sendWelcome(Session session, Company company, String senderName) {
        FlowStep welcomeStep = loadStep(company, session.getCurrentStepKey());
        session.activate();
        sessionRepository.save(session);
        sendStep(welcomeStep, session, company, senderName);
    }

    private String resolveNextStep(FlowStep step, String userInput, Session session, Company company) {
        String input = userInput == null ? "" : userInput.trim();

        return switch (step.getType()) {
            case MENU -> resolveMenuTransition(step, input);
            case INPUT -> resolveInputTransition(step, input);
            case ACTION, END -> step.getDefaultNextStepKey();
        };
    }

    private String resolveMenuTransition(FlowStep step, String input) {
        String normalized = input.toLowerCase();

        if (normalized.equals("menu") || normalized.equals("0")) {
            return step.getTransitions().stream()
                .filter(t -> t.getTrigger().equalsIgnoreCase(normalized))
                .findFirst()
                .map(FlowTransition::getNextStepKey)
                .orElse(step.getDefaultNextStepKey());
        }

        return step.getTransitions().stream()
            .filter(t -> t.getTrigger().equalsIgnoreCase(normalized))
            .findFirst()
            .map(FlowTransition::getNextStepKey)
            .orElse(null);
    }

    private String resolveInputTransition(FlowStep step, String input) {
        if (step.getInputType() == null) {
            return step.getDefaultNextStepKey();
        }

        if (inputValidator.validate(step.getInputType(), input)) {
            return step.getDefaultNextStepKey();
        }

        return null;
    }

    private void sendStep(FlowStep step, Session session, Company company, String senderName) {
        String message = templateRenderer.render(step.getMessageTemplate(), session, senderName);
        MessagingGateway gateway = gatewayFactory.getGateway(company.getChannelType());
        gateway.sendText(session.getPhoneNumber(), message, company);
        log.info("Mensagem enviada para {} | step: {}", session.getPhoneNumber(), step.getStepKey());
    }

    private void sendInvalidOptionMessage(Session session, Company company) {
        FlowStep currentStep = loadStep(company, session.getCurrentStepKey());

        String errorMsg = switch (currentStep.getType()) {
            case INPUT -> currentStep.getInputType() != null
                ? inputValidator.errorMessage(currentStep.getInputType())
                : "Entrada inválida. Tente novamente.";
            default -> "Opção inválida. Por favor, escolha uma das opções disponíveis.";
        };

        MessagingGateway gateway = gatewayFactory.getGateway(company.getChannelType());
        gateway.sendText(session.getPhoneNumber(), errorMsg, company);
    }

    private FlowStep loadStep(Company company, String stepKey) {
        return flowStepRepository.findByCompanyIdAndStepKey(company.getId(), stepKey)
            .orElseThrow(() -> new FlowStepNotFoundException(company.getId(), stepKey));
    }
}
