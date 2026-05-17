package ms.chatbot.dias.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.ActionType;
import ms.chatbot.dias.infrastructure.erp.ErpClient;
import ms.chatbot.dias.infrastructure.erp.dto.AgendamentoResult;
import ms.chatbot.dias.infrastructure.erp.dto.ServicoItem;
import ms.chatbot.dias.infrastructure.erp.dto.SlotItem;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErpActionExecutor {

    private final ErpClient erpClient;

    public boolean execute(FlowStep step, Session session, Company company) {
        if (step.getActionType() == null) {
            log.warn("ACTION step {} sem actionType definido", step.getStepKey());
            return false;
        }

        try {
            if (step.getActionType() == ActionType.CARREGAR_DADOS_EMPRESA) {
                executarCarregarDadosEmpresa(session, company);
                return true;
            }

            String empresaId = company.getErpEmpresaId();
            if (empresaId == null || empresaId.isBlank()) {
                log.error("Company {} não tem erpEmpresaId configurado para ACTION {}", company.getId(), step.getActionType());
                return false;
            }

            switch (step.getActionType()) {
                case LISTAR_SERVICOS -> executarListarServicos(session, empresaId);
                case BUSCAR_OU_CRIAR_CLIENTE -> executarBuscarOuCriarCliente(session, empresaId);
                case VERIFICAR_DISPONIBILIDADE -> executarVerificarDisponibilidade(session, empresaId);
                case CRIAR_AGENDAMENTO -> executarCriarAgendamento(session, empresaId);
                default -> throw new IllegalStateException("ActionType não tratado: " + step.getActionType());
            }
            return true;
        } catch (Exception e) {
            log.error("Erro ao executar ação {} para sessão {}: {}", step.getActionType(), session.getId(), e.getMessage());
            return false;
        }
    }

    private void executarCarregarDadosEmpresa(Session session, Company company) {
        if (company.getEndereco() != null) session.storeData("endereco", company.getEndereco());
        if (company.getHorarioFuncionamento() != null) session.storeData("horario", company.getHorarioFuncionamento());
        if (company.getTelefoneContato() != null) session.storeData("telefone_contato", company.getTelefoneContato());
        session.storeData("nome_empresa", company.getName());
    }

    private void executarListarServicos(Session session, String empresaId) {
        List<ServicoItem> servicos = erpClient.listarServicos(empresaId);
        if (servicos == null || servicos.isEmpty()) {
            throw new IllegalStateException("Nenhum serviço disponível");
        }

        StringBuilder menu = new StringBuilder();
        for (int i = 0; i < servicos.size(); i++) {
            ServicoItem s = servicos.get(i);
            menu.append(String.format(Locale.US, "%d. %s (%dmin) - R$ %.2f%n", i + 1, s.nome(), s.duracao(), s.preco()));
            session.storeData("servico_id_" + (i + 1), s.id());
        }
        session.storeData("servicos_menu", menu.toString().trim());
        session.storeData("servicos_total", String.valueOf(servicos.size()));
    }

    private void executarBuscarOuCriarCliente(Session session, String empresaId) {
        String nome = session.getData("nome");
        String email = session.getData("email");
        String clienteId = erpClient.buscarOuCriarCliente(empresaId, nome, session.getPhoneNumber(), email);
        if (clienteId == null) throw new IllegalStateException("ID do cliente não retornado pelo ERP");
        session.storeData("cliente_id", clienteId);
    }

    private void executarVerificarDisponibilidade(Session session, String empresaId) {
        String servicoOpcao = session.getData("servico_opcao");
        String servicoId = session.getData("servico_id_" + servicoOpcao);
        if (servicoId == null) throw new IllegalStateException("Opção de serviço inválida: " + servicoOpcao);

        String data = session.getData("data_agendamento");
        List<SlotItem> slots = erpClient.listarDisponibilidade(empresaId, servicoId, data);
        if (slots == null || slots.isEmpty()) {
            throw new IllegalStateException("Nenhum horário disponível para a data informada");
        }

        StringBuilder menu = new StringBuilder();
        for (int i = 0; i < slots.size(); i++) {
            menu.append(String.format("%d. %s%n", i + 1, slots.get(i).horario()));
            session.storeData("slot_horario_" + (i + 1), slots.get(i).horario());
        }
        session.storeData("slots_menu", menu.toString().trim());
        session.storeData("slots_total", String.valueOf(slots.size()));
    }

    private void executarCriarAgendamento(Session session, String empresaId) {
        String clienteId = session.getData("cliente_id");
        String servicoOpcao = session.getData("servico_opcao");
        String servicoId = session.getData("servico_id_" + servicoOpcao);
        String data = session.getData("data_agendamento");
        String slotOpcao = session.getData("slot_opcao");
        String horaInicio = session.getData("slot_horario_" + slotOpcao);

        if (servicoId == null) throw new IllegalStateException("Serviço não encontrado na sessão");
        if (horaInicio == null) throw new IllegalStateException("Horário não encontrado na sessão");

        AgendamentoResult result = erpClient.criarAgendamento(empresaId, clienteId, servicoId, data, horaInicio);
        if (result == null) throw new IllegalStateException("Agendamento não retornou resultado");

        session.storeData("confirmacao", result.codigo());
        session.storeData("agendamento_id", result.id());
    }
}
