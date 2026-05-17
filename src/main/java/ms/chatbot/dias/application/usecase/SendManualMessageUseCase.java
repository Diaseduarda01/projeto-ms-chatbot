package ms.chatbot.dias.application.usecase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.Message;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.MessageDirection;
import ms.chatbot.dias.domain.enums.SessionStatus;
import ms.chatbot.dias.domain.exception.CompanyNotFoundException;
import ms.chatbot.dias.domain.exception.SessionNotInHandoffException;
import ms.chatbot.dias.domain.exception.SessionNotFoundException;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.domain.port.MessageRepository;
import ms.chatbot.dias.domain.port.MessagingGateway;
import ms.chatbot.dias.domain.port.SessionRepository;
import ms.chatbot.dias.application.service.ChatEventPublisher;
import ms.chatbot.dias.infrastructure.evolution.MessagingGatewayFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SendManualMessageUseCase {

    private final SessionRepository sessionRepository;
    private final CompanyRepository companyRepository;
    private final MessagingGatewayFactory gatewayFactory;
    private final MessageRepository messageRepository;
    private final ChatEventPublisher eventPublisher;

    public void execute(UUID sessionId, String texto) {
        Session session = sessionRepository.findById(sessionId)
            .orElseThrow(() -> new SessionNotFoundException(sessionId));

        if (session.getStatus() != SessionStatus.HANDOFF) {
            throw new SessionNotInHandoffException(sessionId);
        }

        Company company = companyRepository.findById(session.getCompanyId())
            .orElseThrow(() -> new CompanyNotFoundException(session.getCompanyId().toString()));

        MessagingGateway gateway = gatewayFactory.getGateway(company.getChannelType());
        gateway.sendText(session.getPhoneNumber(), texto, company);

        Message saved = messageRepository.save(Message.builder()
            .sessionId(session.getId())
            .companyId(company.getId())
            .phoneNumber(session.getPhoneNumber())
            .direction(MessageDirection.OUTBOUND)
            .text(texto)
            .build());
        eventPublisher.publishMessage(saved);

        log.info("Mensagem manual enviada para {} | sessão: {}", session.getPhoneNumber(), sessionId);
    }
}
