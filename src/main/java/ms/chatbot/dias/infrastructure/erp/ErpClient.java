package ms.chatbot.dias.infrastructure.erp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.infrastructure.erp.dto.AgendamentoResult;
import ms.chatbot.dias.infrastructure.erp.dto.ServicoItem;
import ms.chatbot.dias.infrastructure.erp.dto.SlotItem;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ErpClient {

    private final RestClient restClient;
    private final ErpProperties properties;

    public List<ServicoItem> listarServicos(String empresaId) {
        String url = properties.getBaseUrl() + "/api/servicos?empresaId=" + empresaId;
        log.debug("ERP listarServicos: {}", url);
        return restClient.get()
            .uri(url)
            .header("X-Internal-Key", properties.getInternalApiKey())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    public String buscarOuCriarCliente(String empresaId, String nome, String telefone, String email) {
        String url = properties.getBaseUrl() + "/api/clientes/buscar-ou-criar";
        Map<String, Object> body = new HashMap<>();
        body.put("empresaId", empresaId);
        body.put("nome", nome);
        body.put("telefone", telefone);
        if (email != null && !email.isBlank()) body.put("email", email);
        log.debug("ERP buscarOuCriarCliente para: {}", telefone);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = restClient.post()
            .uri(url)
            .header("X-Internal-Key", properties.getInternalApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);

        if (response == null) throw new IllegalStateException("Resposta vazia ao buscar/criar cliente");
        return (String) response.get("id");
    }

    public List<SlotItem> listarDisponibilidade(String empresaId, String servicoId, String data) {
        String url = properties.getBaseUrl()
            + "/api/disponibilidade?empresaId=" + empresaId
            + "&servicoId=" + servicoId
            + "&data=" + data;
        log.debug("ERP listarDisponibilidade: {}", url);
        return restClient.get()
            .uri(url)
            .header("X-Internal-Key", properties.getInternalApiKey())
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    }

    public AgendamentoResult criarAgendamento(String empresaId, String clienteId,
                                               String servicoId, String data, String horaInicio) {
        String url = properties.getBaseUrl() + "/api/agendamentos";
        Map<String, Object> body = Map.of(
            "empresaId", empresaId,
            "clienteId", clienteId,
            "servicoId", servicoId,
            "data", data,
            "horaInicio", horaInicio
        );
        log.debug("ERP criarAgendamento: cliente={} servico={} data={} hora={}", clienteId, servicoId, data, horaInicio);
        return restClient.post()
            .uri(url)
            .header("X-Internal-Key", properties.getInternalApiKey())
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(AgendamentoResult.class);
    }
}
