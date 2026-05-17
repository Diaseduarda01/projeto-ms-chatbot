package ms.chatbot.dias.application.service;

import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.ActionType;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.enums.StepType;
import ms.chatbot.dias.infrastructure.erp.ErpClient;
import ms.chatbot.dias.infrastructure.erp.dto.AgendamentoResult;
import ms.chatbot.dias.infrastructure.erp.dto.ServicoItem;
import ms.chatbot.dias.infrastructure.erp.dto.SlotItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ErpActionExecutorTest {

    @Mock ErpClient erpClient;

    @InjectMocks
    ErpActionExecutor executor;

    Company company;
    Session session;

    @BeforeEach
    void setUp() {
        company = Company.builder()
            .id(UUID.randomUUID())
            .name("Barbearia")
            .evolutionInstanceName("barbearia-instance")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .erpEmpresaId("empresa-123")
            .active(true)
            .build();

        session = Session.newSession(company.getId(), "5511999999999", "MAIN_MENU");
        session.activate();
    }

    @Test
    void execute_listar_servicos_popula_menu_e_ids_na_sessao() {
        when(erpClient.listarServicos("empresa-123")).thenReturn(List.of(
            new ServicoItem("id-corte", "Corte", 30, 35.0),
            new ServicoItem("id-barba", "Barba", 20, 25.0)
        ));

        FlowStep step = buildActionStep(ActionType.LISTAR_SERVICOS);
        boolean result = executor.execute(step, session, company);

        assertThat(result).isTrue();
        assertThat(session.getData("servico_id_1")).isEqualTo("id-corte");
        assertThat(session.getData("servico_id_2")).isEqualTo("id-barba");
        assertThat(session.getData("servicos_menu")).contains("1. Corte");
        assertThat(session.getData("servicos_menu")).contains("2. Barba");
        assertThat(session.getData("servicos_total")).isEqualTo("2");
    }

    @Test
    void execute_buscar_ou_criar_cliente_popula_cliente_id_na_sessao() {
        session.storeData("nome", "João Silva");
        when(erpClient.buscarOuCriarCliente("empresa-123", "João Silva", "5511999999999", null))
            .thenReturn("cliente-uuid-456");

        FlowStep step = buildActionStep(ActionType.BUSCAR_OU_CRIAR_CLIENTE);
        boolean result = executor.execute(step, session, company);

        assertThat(result).isTrue();
        assertThat(session.getData("cliente_id")).isEqualTo("cliente-uuid-456");
    }

    @Test
    void execute_verificar_disponibilidade_popula_slots_na_sessao() {
        session.storeData("servico_opcao", "1");
        session.storeData("servico_id_1", "id-corte");
        session.storeData("data_agendamento", "2024-06-15");
        when(erpClient.listarDisponibilidade("empresa-123", "id-corte", "2024-06-15")).thenReturn(List.of(
            new SlotItem("09:00"),
            new SlotItem("14:00")
        ));

        FlowStep step = buildActionStep(ActionType.VERIFICAR_DISPONIBILIDADE);
        boolean result = executor.execute(step, session, company);

        assertThat(result).isTrue();
        assertThat(session.getData("slot_horario_1")).isEqualTo("09:00");
        assertThat(session.getData("slot_horario_2")).isEqualTo("14:00");
        assertThat(session.getData("slots_menu")).contains("1. 09:00");
        assertThat(session.getData("slots_menu")).contains("2. 14:00");
    }

    @Test
    void execute_criar_agendamento_popula_confirmacao_na_sessao() {
        session.storeData("cliente_id", "cliente-uuid-456");
        session.storeData("servico_opcao", "1");
        session.storeData("servico_id_1", "id-corte");
        session.storeData("data_agendamento", "2024-06-15");
        session.storeData("slot_opcao", "2");
        session.storeData("slot_horario_2", "14:00");
        when(erpClient.criarAgendamento("empresa-123", "cliente-uuid-456", "id-corte", "2024-06-15", "14:00"))
            .thenReturn(new AgendamentoResult("ag-uuid-789", "AG-20240615-001"));

        FlowStep step = buildActionStep(ActionType.CRIAR_AGENDAMENTO);
        boolean result = executor.execute(step, session, company);

        assertThat(result).isTrue();
        assertThat(session.getData("confirmacao")).isEqualTo("AG-20240615-001");
        assertThat(session.getData("agendamento_id")).isEqualTo("ag-uuid-789");
    }

    @Test
    void execute_retorna_false_quando_erp_lanca_excecao() {
        when(erpClient.listarServicos("empresa-123")).thenThrow(new RuntimeException("ERP indisponível"));

        FlowStep step = buildActionStep(ActionType.LISTAR_SERVICOS);
        boolean result = executor.execute(step, session, company);

        assertThat(result).isFalse();
    }

    @Test
    void execute_retorna_false_quando_empresa_sem_erp_empresa_id() {
        Company semErp = Company.builder()
            .id(UUID.randomUUID())
            .name("Sem ERP")
            .evolutionInstanceName("sem-erp")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .active(true)
            .build();

        FlowStep step = buildActionStep(ActionType.LISTAR_SERVICOS);
        boolean result = executor.execute(step, session, semErp);

        assertThat(result).isFalse();
        verifyNoInteractions(erpClient);
    }

    @Test
    void execute_carregar_dados_empresa_popula_dados_na_sessao() {
        Company comDados = Company.builder()
            .id(UUID.randomUUID())
            .name("Barbearia Top")
            .evolutionInstanceName("barbearia-top")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .endereco("Rua das Flores, 123 - Centro")
            .horarioFuncionamento("Seg-Sáb 9h-19h")
            .telefoneContato("(11) 99999-0000")
            .active(true)
            .build();

        FlowStep step = buildActionStep(ActionType.CARREGAR_DADOS_EMPRESA);
        boolean result = executor.execute(step, session, comDados);

        assertThat(result).isTrue();
        assertThat(session.getData("endereco")).isEqualTo("Rua das Flores, 123 - Centro");
        assertThat(session.getData("horario")).isEqualTo("Seg-Sáb 9h-19h");
        assertThat(session.getData("telefone_contato")).isEqualTo("(11) 99999-0000");
        assertThat(session.getData("nome_empresa")).isEqualTo("Barbearia Top");
        verifyNoInteractions(erpClient);
    }

    @Test
    void execute_carregar_dados_empresa_funciona_sem_erp_empresa_id() {
        Company semErpId = Company.builder()
            .id(UUID.randomUUID())
            .name("Studio")
            .evolutionInstanceName("studio-instance")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .endereco("Av. Central, 456")
            .active(true)
            .build();

        FlowStep step = buildActionStep(ActionType.CARREGAR_DADOS_EMPRESA);
        boolean result = executor.execute(step, session, semErpId);

        assertThat(result).isTrue();
        assertThat(session.getData("endereco")).isEqualTo("Av. Central, 456");
        verifyNoInteractions(erpClient);
    }

    private FlowStep buildActionStep(ActionType actionType) {
        return FlowStep.builder()
            .id(UUID.randomUUID())
            .companyId(company.getId())
            .stepKey("ACTION_STEP")
            .type(StepType.ACTION)
            .messageTemplate("Processando...")
            .actionType(actionType)
            .defaultNextStepKey("NEXT_STEP")
            .transitions(List.of())
            .build();
    }
}
