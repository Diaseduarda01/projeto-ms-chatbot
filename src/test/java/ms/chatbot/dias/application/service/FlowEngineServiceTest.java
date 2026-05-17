package ms.chatbot.dias.application.service;

import ms.chatbot.dias.application.service.ChatEventPublisher;
import ms.chatbot.dias.application.service.ErpActionExecutor;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.FlowTransition;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.ActionType;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.enums.InputType;
import ms.chatbot.dias.domain.enums.SessionStatus;
import ms.chatbot.dias.domain.enums.StepType;
import ms.chatbot.dias.domain.port.FlowStepRepository;
import ms.chatbot.dias.domain.port.MessageRepository;
import ms.chatbot.dias.domain.port.MessagingGateway;
import ms.chatbot.dias.domain.port.SessionRepository;
import ms.chatbot.dias.infrastructure.evolution.MessagingGatewayFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlowEngineServiceTest {

    @Mock FlowStepRepository flowStepRepository;
    @Mock SessionRepository sessionRepository;
    @Mock MessagingGatewayFactory gatewayFactory;
    @Mock InputValidator inputValidator;
    @Mock TemplateRenderer templateRenderer;
    @Mock MessagingGateway messagingGateway;
    @Mock MessageRepository messageRepository;
    @Mock ChatEventPublisher eventPublisher;
    @Mock ErpActionExecutor erpActionExecutor;

    @InjectMocks
    FlowEngineService flowEngineService;

    Company company;
    Session session;

    @BeforeEach
    void setUp() {
        company = Company.builder()
            .id(UUID.randomUUID())
            .name("Salão Teste")
            .evolutionInstanceName("salao-teste")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .build();

        session = Session.newSession(company.getId(), "5511999999999", "MAIN_MENU");
        session.activate();

        when(gatewayFactory.getGateway(any())).thenReturn(messagingGateway);
        lenient().when(templateRenderer.render(anyString(), any(), any())).thenReturn("mensagem renderizada");
    }

    @Test
    void process_menu_avanca_para_transicao_correta() {
        FlowStep menuStep = buildMenuStep("MAIN_MENU",
            List.of(transition("1", "AGENDAMENTO")));
        FlowStep nextStep = buildStep("AGENDAMENTO", StepType.INPUT, "Informe seu nome:");

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "MAIN_MENU"))
            .thenReturn(Optional.of(menuStep));
        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "AGENDAMENTO"))
            .thenReturn(Optional.of(nextStep));

        flowEngineService.process(session, company, "1", "Maria");

        assertThat(session.getCurrentStepKey()).isEqualTo("AGENDAMENTO");
        verify(messagingGateway).sendText(eq("5511999999999"), anyString(), eq(company));
        verify(sessionRepository).save(session);
    }

    @Test
    void process_menu_envia_mensagem_de_opcao_invalida_para_trigger_desconhecido() {
        FlowStep menuStep = buildMenuStep("MAIN_MENU",
            List.of(transition("1", "AGENDAMENTO")));

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "MAIN_MENU"))
            .thenReturn(Optional.of(menuStep));

        flowEngineService.process(session, company, "9", "Maria");

        assertThat(session.getCurrentStepKey()).isEqualTo("MAIN_MENU");
        verify(messagingGateway).sendText(
            eq("5511999999999"),
            eq("Opção inválida. Por favor, escolha uma das opções disponíveis."),
            eq(company)
        );
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void process_input_armazena_dado_e_avanca_com_entrada_valida() {
        FlowStep inputStep = FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey("COLLECT_NAME")
            .type(StepType.INPUT)
            .messageTemplate("Informe seu nome:")
            .inputType(InputType.TEXT)
            .sessionDataKey("nome")
            .defaultNextStepKey("CONFIRM")
            .transitions(List.of())
            .build();
        FlowStep confirmStep = buildStep("CONFIRM", StepType.MENU, "Confirmar?");

        session.setCurrentStepKey("COLLECT_NAME");

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "COLLECT_NAME"))
            .thenReturn(Optional.of(inputStep));
        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "CONFIRM"))
            .thenReturn(Optional.of(confirmStep));
        when(inputValidator.validate(InputType.TEXT, "João")).thenReturn(true);
        when(inputValidator.normalize(InputType.TEXT, "João")).thenReturn("João");

        flowEngineService.process(session, company, "João", null);

        assertThat(session.getCurrentStepKey()).isEqualTo("CONFIRM");
        assertThat(session.getData("nome")).isEqualTo("João");
    }

    @Test
    void process_input_rejeita_entrada_invalida_e_envia_erro() {
        FlowStep inputStep = FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey("COLLECT_NAME")
            .type(StepType.INPUT)
            .messageTemplate("Informe seu nome:")
            .inputType(InputType.TEXT)
            .sessionDataKey("nome")
            .defaultNextStepKey("CONFIRM")
            .transitions(List.of())
            .build();

        session.setCurrentStepKey("COLLECT_NAME");

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "COLLECT_NAME"))
            .thenReturn(Optional.of(inputStep));
        when(inputValidator.validate(InputType.TEXT, "x")).thenReturn(false);
        when(inputValidator.errorMessage(InputType.TEXT)).thenReturn("Texto inválido.");

        flowEngineService.process(session, company, "x", null);

        assertThat(session.getCurrentStepKey()).isEqualTo("COLLECT_NAME");
        verify(messagingGateway).sendText(eq("5511999999999"), eq("Texto inválido."), eq(company));
    }

    @Test
    void process_step_END_completa_sessao() {
        FlowStep menuStep = buildMenuStep("MAIN_MENU",
            List.of(transition("1", "FIM")));
        FlowStep endStep = buildStep("FIM", StepType.END, "Até logo!");

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "MAIN_MENU"))
            .thenReturn(Optional.of(menuStep));
        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "FIM"))
            .thenReturn(Optional.of(endStep));

        flowEngineService.process(session, company, "1", "Maria");

        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        verify(sessionRepository, times(2)).save(session);
    }

    @Test
    void sendWelcome_ativa_sessao_e_envia_mensagem() {
        FlowStep welcomeStep = buildStep("MAIN_MENU", StepType.MENU, "Bem-vindo, {{nome}}!");
        Session newSession = Session.newSession(company.getId(), "5511999999999", "MAIN_MENU");

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "MAIN_MENU"))
            .thenReturn(Optional.of(welcomeStep));

        flowEngineService.sendWelcome(newSession, company, "Ana");

        assertThat(newSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        verify(sessionRepository).save(newSession);
        verify(messagingGateway).sendText(eq("5511999999999"), anyString(), eq(company));
    }

    @Test
    void process_menu_trigger_menu_usa_defaultNextStep() {
        FlowStep menuStep = FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey("MAIN_MENU")
            .type(StepType.MENU)
            .messageTemplate("Menu principal")
            .defaultNextStepKey("MAIN_MENU")
            .transitions(List.of(
                FlowTransition.builder().trigger("menu").nextStepKey("MAIN_MENU").sortOrder(0).build()
            ))
            .build();

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "MAIN_MENU"))
            .thenReturn(Optional.of(menuStep));

        flowEngineService.process(session, company, "menu", "Maria");

        assertThat(session.getCurrentStepKey()).isEqualTo("MAIN_MENU");
    }

    @Test
    void process_action_executa_acao_e_avanca_ao_proximo_step() {
        FlowStep menuStep = buildMenuStep("MAIN_MENU",
            List.of(transition("1", "ACTION_LISTAR")));
        FlowStep actionStep = FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey("ACTION_LISTAR")
            .type(StepType.ACTION)
            .messageTemplate("Buscando serviços...")
            .actionType(ActionType.LISTAR_SERVICOS)
            .defaultNextStepKey("ESCOLHA_SERVICO")
            .transitions(List.of())
            .build();
        FlowStep resultStep = buildStep("ESCOLHA_SERVICO", StepType.MENU, "{{servicos_menu}}");

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "MAIN_MENU"))
            .thenReturn(Optional.of(menuStep));
        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "ACTION_LISTAR"))
            .thenReturn(Optional.of(actionStep));
        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "ESCOLHA_SERVICO"))
            .thenReturn(Optional.of(resultStep));
        when(erpActionExecutor.execute(eq(actionStep), any(), eq(company))).thenReturn(true);

        flowEngineService.process(session, company, "1", "Maria");

        verify(erpActionExecutor).execute(eq(actionStep), any(), eq(company));
        assertThat(session.getCurrentStepKey()).isEqualTo("ESCOLHA_SERVICO");
        verify(messagingGateway, times(2)).sendText(eq("5511999999999"), anyString(), eq(company));
    }

    @Test
    void process_action_reverte_step_anterior_e_envia_erro_quando_acao_falha() {
        FlowStep menuStep = buildMenuStep("MAIN_MENU",
            List.of(transition("1", "ACTION_LISTAR")));
        FlowStep actionStep = FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey("ACTION_LISTAR")
            .type(StepType.ACTION)
            .messageTemplate("Buscando serviços...")
            .actionType(ActionType.LISTAR_SERVICOS)
            .defaultNextStepKey("ESCOLHA_SERVICO")
            .transitions(List.of())
            .build();

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "MAIN_MENU"))
            .thenReturn(Optional.of(menuStep));
        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "ACTION_LISTAR"))
            .thenReturn(Optional.of(actionStep));
        when(erpActionExecutor.execute(eq(actionStep), any(), eq(company))).thenReturn(false);

        flowEngineService.process(session, company, "1", "Maria");

        verify(erpActionExecutor).execute(eq(actionStep), any(), eq(company));
        assertThat(session.getCurrentStepKey()).isEqualTo("MAIN_MENU");
        verify(messagingGateway, times(2)).sendText(eq("5511999999999"), anyString(), eq(company));
    }

    @Test
    void process_menu_com_session_data_key_armazena_opcao_na_sessao() {
        FlowStep menuDinamico = FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey("MAIN_MENU")
            .type(StepType.MENU)
            .messageTemplate("{{servicos_menu}}")
            .sessionDataKey("servico_opcao")
            .defaultNextStepKey("ACTION_VERIFICAR")
            .transitions(List.of())
            .build();
        FlowStep actionStep = buildStep("ACTION_VERIFICAR", StepType.INPUT, "Informe a data:");

        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "MAIN_MENU"))
            .thenReturn(Optional.of(menuDinamico));
        when(flowStepRepository.findByCompanyIdAndStepKey(company.getId(), "ACTION_VERIFICAR"))
            .thenReturn(Optional.of(actionStep));

        flowEngineService.process(session, company, "2", "Maria");

        assertThat(session.getData("servico_opcao")).isEqualTo("2");
        assertThat(session.getCurrentStepKey()).isEqualTo("ACTION_VERIFICAR");
    }

    // helpers

    private FlowStep buildMenuStep(String stepKey, List<FlowTransition> transitions) {
        return FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey(stepKey)
            .type(StepType.MENU)
            .messageTemplate("Escolha uma opção:")
            .transitions(transitions)
            .build();
    }

    private FlowStep buildStep(String stepKey, StepType type, String template) {
        return FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey(stepKey)
            .type(type)
            .messageTemplate(template)
            .transitions(List.of())
            .build();
    }

    private FlowTransition transition(String trigger, String nextStepKey) {
        return FlowTransition.builder()
            .trigger(trigger)
            .nextStepKey(nextStepKey)
            .sortOrder(0)
            .build();
    }
}
