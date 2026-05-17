package ms.chatbot.dias.application.usecase;

import ms.chatbot.dias.application.service.ChatEventPublisher;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.Message;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.enums.MessageDirection;
import ms.chatbot.dias.domain.exception.SessionNotInHandoffException;
import ms.chatbot.dias.domain.exception.SessionNotFoundException;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.domain.port.MessageRepository;
import ms.chatbot.dias.domain.port.MessagingGateway;
import ms.chatbot.dias.domain.port.SessionRepository;
import ms.chatbot.dias.infrastructure.evolution.MessagingGatewayFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendManualMessageUseCaseTest {

    @Mock SessionRepository sessionRepository;
    @Mock CompanyRepository companyRepository;
    @Mock MessagingGatewayFactory gatewayFactory;
    @Mock MessageRepository messageRepository;
    @Mock ChatEventPublisher eventPublisher;
    @Mock MessagingGateway messagingGateway;

    @InjectMocks
    SendManualMessageUseCase useCase;

    Company company;
    Session session;

    @BeforeEach
    void setUp() {
        company = Company.builder()
            .id(UUID.randomUUID())
            .name("Barbearia Teste")
            .evolutionInstanceName("barbearia-teste")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .active(true)
            .build();

        session = Session.newSession(company.getId(), "5511999999999", "COLLECT_NAME");
        session.activate();
        session.handoff();

    }

    @Test
    void execute_envia_mensagem_e_grava_outbound_quando_sessao_em_handoff() {
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(companyRepository.findById(company.getId())).thenReturn(Optional.of(company));
        when(gatewayFactory.getGateway(ChannelType.BAILEYS)).thenReturn(messagingGateway);
        when(messageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(session.getId(), "Olá! Pode me informar o endereço?");

        verify(messagingGateway).sendText("5511999999999", "Olá! Pode me informar o endereço?", company);

        var captor = ArgumentCaptor.forClass(Message.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getDirection()).isEqualTo(MessageDirection.OUTBOUND);
        assertThat(captor.getValue().getText()).isEqualTo("Olá! Pode me informar o endereço?");
        assertThat(captor.getValue().getPhoneNumber()).isEqualTo("5511999999999");

        verify(eventPublisher).publishMessage(captor.getValue());
    }

    @Test
    void execute_lanca_SessionNotFoundException_quando_sessao_nao_existe() {
        UUID id = UUID.randomUUID();
        when(sessionRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(id, "oi"))
            .isInstanceOf(SessionNotFoundException.class);

        verifyNoInteractions(messagingGateway, messageRepository);
    }

    @Test
    void execute_lanca_SessionNotInHandoffException_quando_sessao_esta_ativa() {
        Session activeSession = Session.newSession(company.getId(), "5511999999999", "MAIN_MENU");
        activeSession.activate();

        when(sessionRepository.findById(activeSession.getId())).thenReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> useCase.execute(activeSession.getId(), "oi"))
            .isInstanceOf(SessionNotInHandoffException.class);

        verifyNoInteractions(messagingGateway, messageRepository);
    }

    @Test
    void execute_lanca_SessionNotInHandoffException_quando_sessao_esta_concluida() {
        Session completedSession = Session.newSession(company.getId(), "5511999999999", "MAIN_MENU");
        completedSession.complete();

        when(sessionRepository.findById(completedSession.getId())).thenReturn(Optional.of(completedSession));

        assertThatThrownBy(() -> useCase.execute(completedSession.getId(), "oi"))
            .isInstanceOf(SessionNotInHandoffException.class);

        verifyNoInteractions(messagingGateway, messageRepository);
    }

    @Test
    void execute_nao_chama_gateway_se_sessao_nao_encontrada() {
        when(sessionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(UUID.randomUUID(), "oi"))
            .isInstanceOf(SessionNotFoundException.class);

        verifyNoInteractions(gatewayFactory);
    }
}
