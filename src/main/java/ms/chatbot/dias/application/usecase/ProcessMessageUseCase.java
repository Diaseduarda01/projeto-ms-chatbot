package ms.chatbot.dias.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.application.service.ChatEventPublisher;
import ms.chatbot.dias.application.service.FlowEngineService;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.Message;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.MessageDirection;
import ms.chatbot.dias.domain.enums.SessionStatus;
import ms.chatbot.dias.domain.exception.CompanyNotFoundException;
import ms.chatbot.dias.domain.model.IncomingMessage;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.domain.port.MessageRepository;
import ms.chatbot.dias.domain.port.SessionRepository;
import ms.chatbot.dias.infrastructure.web.dto.InboxEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessMessageUseCase {

    private final CompanyRepository companyRepository;
    private final SessionRepository sessionRepository;
    private final FlowEngineService flowEngineService;
    private final MessageRepository messageRepository;
    private final ChatEventPublisher eventPublisher;

    public void execute(IncomingMessage message) {
        if (message.text() == null || message.text().isBlank()) {
            log.debug("Mensagem vazia ignorada de {}", message.senderPhone());
            return;
        }

        log.info("Processando mensagem de {} | instância: {} | texto: {}",
            message.senderPhone(), message.companyInstanceName(), message.text());

        Company company = companyRepository.findByInstanceName(message.companyInstanceName())
            .filter(Company::isActive)
            .orElseThrow(() -> new CompanyNotFoundException(message.companyInstanceName()));

        Session session = sessionRepository
            .findByCompanyIdAndPhoneNumber(company.getId(), message.senderPhone())
            .orElseGet(() -> Session.newSession(
                company.getId(),
                message.senderPhone(),
                company.getWelcomeStepKey()
            ));

        if (session.getStatus() == SessionStatus.NEW) {
            session = sessionRepository.save(session);
            Message saved = saveInbound(session, company, message.text());
            eventPublisher.publishMessage(saved);
            eventPublisher.publishInboxEvent(session, InboxEvent.SESSION_STARTED);
            flowEngineService.sendWelcome(session, company, message.senderName());
            return;
        }

        Message saved = saveInbound(session, company, message.text());
        eventPublisher.publishMessage(saved);

        if (session.getStatus() == SessionStatus.HANDOFF) {
            log.info("Sessão {} em HANDOFF — mensagem gravada, aguardando atendente", session.getId());
            eventPublisher.publishInboxEvent(session, InboxEvent.MESSAGE_RECEIVED);
            return;
        }

        if (session.getStatus() == SessionStatus.COMPLETED) {
            session.setCurrentStepKey(company.getWelcomeStepKey());
            session.activate();
            sessionRepository.save(session);
            eventPublisher.publishInboxEvent(session, InboxEvent.SESSION_STARTED);
            flowEngineService.sendWelcome(session, company, message.senderName());
            return;
        }

        flowEngineService.process(session, company, message.text(), message.senderName());
    }

    private Message saveInbound(Session session, Company company, String text) {
        Message message = Message.builder()
            .sessionId(session.getId())
            .companyId(company.getId())
            .phoneNumber(session.getPhoneNumber())
            .direction(MessageDirection.INBOUND)
            .text(text)
            .build();
        return messageRepository.save(message);
    }
}
