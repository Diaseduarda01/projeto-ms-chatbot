package ms.chatbot.dias.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.FlowTransition;
import ms.chatbot.dias.domain.entity.Message;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.MessageDirection;
import ms.chatbot.dias.domain.enums.StepType;
import ms.chatbot.dias.domain.exception.FlowStepNotFoundException;
import ms.chatbot.dias.domain.port.FlowStepRepository;
import ms.chatbot.dias.domain.port.MessageRepository;
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
    private final MessageRepository messageRepository;
    private final ChatEventPublisher eventPublisher;
    private final ErpActionExecutor erpActionExecutor;

    @Transactional
    public void process(Session session, Company company, String userInput, String senderName) {
        FlowStep currentStep = loadStep(company, session.getCurrentStepKey());

        String nextStepKey = resolveNextStep(currentStep, userInput, session, company);

        if (nextStepKey == null) {
            sendInvalidOptionMessage(session, company);
            return;
        }

        // Persiste dado do usuário na sessão
        if (currentStep.getType() == StepType.INPUT
                && currentStep.getSessionDataKey() != null
                && currentStep.getInputType() != null) {
            String normalized = inputValidator.normalize(currentStep.getInputType(), userInput);
            session.storeData(currentStep.getSessionDataKey(), normalized);
        } else if (currentStep.getType() == StepType.MENU
                && currentStep.getSessionDataKey() != null) {
            session.storeData(currentStep.getSessionDataKey(), userInput.trim());
        }

        FlowStep nextStep = loadStep(company, nextStepKey);

        if (nextStep.getType() == StepType.ACTION) {
            processActionStep(currentStep, nextStep, session, company, senderName);
        } else {
            session.setCurrentStepKey(nextStepKey);
            session.activate();
            sessionRepository.save(session);

            sendStep(nextStep, session, company, senderName);

            if (nextStep.getType() == StepType.END) {
                session.complete();
                sessionRepository.save(session);
            }
        }
    }

    private void processActionStep(FlowStep previousStep, FlowStep actionStep,
                                   Session session, Company company, String senderName) {
        // Avança para o ACTION step e envia mensagem de espera
        session.setCurrentStepKey(actionStep.getStepKey());
        session.activate();
        sessionRepository.save(session);
        sendStep(actionStep, session, company, senderName);

        // Executa a ação no ERP
        if (!erpActionExecutor.execute(actionStep, session, company)) {
            sendActionErrorMessage(session, company);
            // Reverte para o step anterior para o usuário poder tentar novamente
            session.setCurrentStepKey(previousStep.getStepKey());
            sessionRepository.save(session);
            return;
        }

        // Salva dados populados pela ação e avança ao próximo step
        sessionRepository.save(session);

        String afterKey = actionStep.getDefaultNextStepKey();
        if (afterKey == null) {
            log.warn("ACTION step {} sem defaultNextStepKey — fluxo interrompido", actionStep.getStepKey());
            return;
        }

        session.setCurrentStepKey(afterKey);
        sessionRepository.save(session);

        FlowStep afterStep = loadStep(company, afterKey);
        sendStep(afterStep, session, company, senderName);

        if (afterStep.getType() == StepType.END) {
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

        // Tenta encontrar transição explícita
        var fromTransition = step.getTransitions().stream()
            .filter(t -> t.getTrigger().equalsIgnoreCase(normalized))
            .findFirst()
            .map(FlowTransition::getNextStepKey);

        if (fromTransition.isPresent()) {
            return fromTransition.get();
        }

        // Menu dinâmico ou misto: aceita qualquer número positivo sem transição explícita → defaultNextStepKey
        if (step.getDefaultNextStepKey() != null
                && step.getSessionDataKey() != null
                && isPositiveInteger(normalized)) {
            return step.getDefaultNextStepKey();
        }

        return null;
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
        String text = templateRenderer.render(step.getMessageTemplate(), session, senderName);
        MessagingGateway gateway = gatewayFactory.getGateway(company.getChannelType());
        gateway.sendText(session.getPhoneNumber(), text, company);
        Message saved = saveOutbound(session, company, text);
        eventPublisher.publishMessage(saved);
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
        Message saved = saveOutbound(session, company, errorMsg);
        eventPublisher.publishMessage(saved);
    }

    private void sendActionErrorMessage(Session session, Company company) {
        String msg = "Ocorreu um erro ao processar sua solicitação. Por favor, tente novamente.";
        MessagingGateway gateway = gatewayFactory.getGateway(company.getChannelType());
        gateway.sendText(session.getPhoneNumber(), msg, company);
        Message saved = saveOutbound(session, company, msg);
        eventPublisher.publishMessage(saved);
    }

    private Message saveOutbound(Session session, Company company, String text) {
        Message message = Message.builder()
            .sessionId(session.getId())
            .companyId(company.getId())
            .phoneNumber(session.getPhoneNumber())
            .direction(MessageDirection.OUTBOUND)
            .text(text)
            .build();
        return messageRepository.save(message);
    }

    private FlowStep loadStep(Company company, String stepKey) {
        return flowStepRepository.findByCompanyIdAndStepKey(company.getId(), stepKey)
            .orElseThrow(() -> new FlowStepNotFoundException(company.getId(), stepKey));
    }

    private boolean isPositiveInteger(String s) {
        try {
            return Integer.parseInt(s) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
