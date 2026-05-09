package ms.chatbot.dias.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.application.service.FlowEngineService;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.SessionStatus;
import ms.chatbot.dias.domain.exception.CompanyNotFoundException;
import ms.chatbot.dias.domain.model.IncomingMessage;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.domain.port.SessionRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessMessageUseCase {

    private final CompanyRepository companyRepository;
    private final SessionRepository sessionRepository;
    private final FlowEngineService flowEngineService;

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
            sessionRepository.save(session);
            flowEngineService.sendWelcome(session, company, message.senderName());
            return;
        }

        if (session.getStatus() == SessionStatus.COMPLETED) {
            session.setCurrentStepKey(company.getWelcomeStepKey());
            session.activate();
            sessionRepository.save(session);
            flowEngineService.sendWelcome(session, company, message.senderName());
            return;
        }

        flowEngineService.process(session, company, message.text(), message.senderName());
    }
}
