package ms.chatbot.dias.application.usecase;

import ms.chatbot.dias.application.service.ChatEventPublisher;
import ms.chatbot.dias.application.service.FlowEngineService;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.enums.SessionStatus;
import ms.chatbot.dias.domain.exception.CompanyNotFoundException;
import ms.chatbot.dias.domain.model.IncomingMessage;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.domain.port.MessageRepository;
import ms.chatbot.dias.domain.port.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class ProcessMessageUseCaseTest {

    @Mock CompanyRepository companyRepository;
    @Mock SessionRepository sessionRepository;
    @Mock FlowEngineService flowEngineService;
    @Mock MessageRepository messageRepository;
    @Mock ChatEventPublisher eventPublisher;

    @InjectMocks
    ProcessMessageUseCase useCase;

    Company activeCompany;

    @BeforeEach
    void setUp() {
        activeCompany = Company.builder()
            .id(UUID.randomUUID())
            .name("Barbearia Teste")
            .evolutionInstanceName("barbearia-teste")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .active(true)
            .build();
    }

    @Test
    void execute_ignora_mensagem_com_texto_vazio() {
        IncomingMessage msg = new IncomingMessage("barbearia-teste", "5511999999999", "João", "", ChannelType.BAILEYS);
        useCase.execute(msg);
        verifyNoInteractions(companyRepository, sessionRepository, flowEngineService);
    }

    @Test
    void execute_ignora_mensagem_com_texto_null() {
        IncomingMessage msg = new IncomingMessage("barbearia-teste", "5511999999999", "João", null, ChannelType.BAILEYS);
        useCase.execute(msg);
        verifyNoInteractions(companyRepository, sessionRepository, flowEngineService);
    }

    @Test
    void execute_lanca_excecao_quando_empresa_nao_encontrada() {
        when(companyRepository.findByInstanceName("desconhecida")).thenReturn(Optional.empty());
        IncomingMessage msg = new IncomingMessage("desconhecida", "5511999999999", "João", "Olá", ChannelType.BAILEYS);
        assertThatThrownBy(() -> useCase.execute(msg))
            .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void execute_lanca_excecao_quando_empresa_esta_inativa() {
        Company inactiveCompany = Company.builder()
            .id(UUID.randomUUID())
            .name("Inativa")
            .evolutionInstanceName("inativa")
            .active(false)
            .build();
        when(companyRepository.findByInstanceName("inativa")).thenReturn(Optional.of(inactiveCompany));

        IncomingMessage msg = new IncomingMessage("inativa", "5511999999999", "João", "Olá", ChannelType.BAILEYS);
        assertThatThrownBy(() -> useCase.execute(msg))
            .isInstanceOf(CompanyNotFoundException.class);
    }

    @Test
    void execute_sessao_nova_salva_e_envia_boas_vindas() {
        when(companyRepository.findByInstanceName("barbearia-teste"))
            .thenReturn(Optional.of(activeCompany));
        when(sessionRepository.findByCompanyIdAndPhoneNumber(any(), any()))
            .thenReturn(Optional.empty());
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IncomingMessage msg = new IncomingMessage("barbearia-teste", "5511999999999", "João", "Olá", ChannelType.BAILEYS);
        useCase.execute(msg);

        verify(sessionRepository).save(any());
        verify(flowEngineService).sendWelcome(any(), eq(activeCompany), eq("João"));
        verify(flowEngineService, never()).process(any(), any(), any(), any());
    }

    @Test
    void execute_sessao_ativa_chama_process() {
        Session activeSession = Session.newSession(activeCompany.getId(), "5511999999999", "MAIN_MENU");
        activeSession.activate();

        when(companyRepository.findByInstanceName("barbearia-teste"))
            .thenReturn(Optional.of(activeCompany));
        when(sessionRepository.findByCompanyIdAndPhoneNumber(any(), any()))
            .thenReturn(Optional.of(activeSession));

        IncomingMessage msg = new IncomingMessage("barbearia-teste", "5511999999999", "João", "1", ChannelType.BAILEYS);
        useCase.execute(msg);

        verify(flowEngineService).process(eq(activeSession), eq(activeCompany), eq("1"), eq("João"));
        verify(flowEngineService, never()).sendWelcome(any(), any(), any());
    }

    @Test
    void execute_sessao_em_handoff_apenas_grava_mensagem_sem_processar_flow() {
        Session handoffSession = Session.newSession(activeCompany.getId(), "5511999999999", "COLLECT_NAME");
        handoffSession.activate();
        handoffSession.handoff();

        when(companyRepository.findByInstanceName("barbearia-teste"))
            .thenReturn(Optional.of(activeCompany));
        when(sessionRepository.findByCompanyIdAndPhoneNumber(any(), any()))
            .thenReturn(Optional.of(handoffSession));

        IncomingMessage msg = new IncomingMessage("barbearia-teste", "5511999999999", "João", "Oi", ChannelType.BAILEYS);
        useCase.execute(msg);

        verify(messageRepository).save(any());
        verifyNoInteractions(flowEngineService);
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void execute_sessao_concluida_reinicia_e_envia_boas_vindas() {
        Session completedSession = Session.newSession(activeCompany.getId(), "5511999999999", "MAIN_MENU");
        completedSession.complete();

        when(companyRepository.findByInstanceName("barbearia-teste"))
            .thenReturn(Optional.of(activeCompany));
        when(sessionRepository.findByCompanyIdAndPhoneNumber(any(), any()))
            .thenReturn(Optional.of(completedSession));
        when(sessionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IncomingMessage msg = new IncomingMessage("barbearia-teste", "5511999999999", "João", "Oi", ChannelType.BAILEYS);
        useCase.execute(msg);

        assertThat(completedSession.getCurrentStepKey()).isEqualTo("MAIN_MENU");
        assertThat(completedSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        verify(flowEngineService).sendWelcome(eq(completedSession), eq(activeCompany), eq("João"));
        verify(flowEngineService, never()).process(any(), any(), any(), any());
    }
}
