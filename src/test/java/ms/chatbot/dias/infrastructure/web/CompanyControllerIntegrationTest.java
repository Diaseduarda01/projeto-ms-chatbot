package ms.chatbot.dias.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.enums.StepType;
import ms.chatbot.dias.infrastructure.evolution.EvolutionHttpClient;
import ms.chatbot.dias.infrastructure.persistence.jpa.CompanyJpaRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.FlowStepJpaRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.SessionJpaRepository;
import ms.chatbot.dias.infrastructure.web.dto.CompanyRequest;
import ms.chatbot.dias.infrastructure.web.dto.FlowStepRequest;
import ms.chatbot.dias.infrastructure.web.dto.UpdateCompanyRequest;
import ms.chatbot.dias.infrastructure.web.dto.UpdateFlowStepRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CompanyControllerIntegrationTest {

    static final String INTERNAL_KEY = "test-internal-key";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CompanyJpaRepository companyJpaRepository;
    @Autowired FlowStepJpaRepository flowStepJpaRepository;
    @Autowired SessionJpaRepository sessionJpaRepository;
    @MockBean EvolutionHttpClient evolutionHttpClient;

    @AfterEach
    void tearDown() {
        sessionJpaRepository.deleteAll();
        flowStepJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    // ─── POST /api/companies ───────────────────────────────────────────────

    @Test
    void createCompany_retorna_empresa_criada() throws Exception {
        CompanyRequest request = new CompanyRequest(
            "Salão Beleza", "salao-beleza-instance", "api-key-123", ChannelType.BAILEYS, "MAIN_MENU", null, null, null, null
        );

        MvcResult result = mockMvc.perform(post("/api/companies")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Salão Beleza"))
            .andExpect(jsonPath("$.evolutionInstanceName").value("salao-beleza-instance"))
            .andExpect(jsonPath("$.channelType").value("BAILEYS"))
            .andExpect(jsonPath("$.welcomeStepKey").value("MAIN_MENU"))
            .andExpect(jsonPath("$.id").exists())
            .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("salao-beleza-instance");
    }

    @Test
    void createCompany_retorna_400_quando_name_ausente() throws Exception {
        String request = """
            {
              "evolutionInstanceName": "instancia-x",
              "channelType": "BAILEYS"
            }
            """;

        mockMvc.perform(post("/api/companies")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createCompany_retorna_400_quando_instanceName_ausente() throws Exception {
        String request = """
            {
              "name": "Empresa Sem Instância",
              "channelType": "BAILEYS"
            }
            """;

        mockMvc.perform(post("/api/companies")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createCompany_usa_valores_padrao_quando_nao_informados() throws Exception {
        CompanyRequest request = new CompanyRequest(
            "Barbearia", "barbearia-default-instance", null, null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/companies")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.channelType").value("BAILEYS"))
            .andExpect(jsonPath("$.welcomeStepKey").value("MAIN_MENU"))
            .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createCompany_retorna_403_sem_chave() throws Exception {
        CompanyRequest request = new CompanyRequest(
            "Salão", "salao-instance", null, ChannelType.BAILEYS, "MAIN_MENU", null, null, null, null
        );

        mockMvc.perform(post("/api/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden());
    }

    // ─── GET /api/companies/{id} ───────────────────────────────────────────

    @Test
    void getCompany_retorna_empresa_por_id() throws Exception {
        String companyId = criarEmpresa("Clínica Teste", "clinica-get-instance");

        mockMvc.perform(get("/api/companies/{id}", companyId)
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Clínica Teste"))
            .andExpect(jsonPath("$.id").value(companyId));
    }

    @Test
    void getCompany_retorna_404_para_empresa_inexistente() throws Exception {
        mockMvc.perform(get("/api/companies/{id}", "00000000-0000-0000-0000-000000000000")
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isNotFound());
    }

    // ─── PUT /api/companies/{id} ───────────────────────────────────────────

    @Test
    void updateCompany_atualiza_dados_da_empresa() throws Exception {
        String companyId = criarEmpresa("Nome Antigo", "update-company-instance");

        UpdateCompanyRequest request = new UpdateCompanyRequest(
            "Nome Novo", "WELCOME", true, "erp-123", null, null, null, null, null
        );

        mockMvc.perform(put("/api/companies/{id}", companyId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Nome Novo"))
            .andExpect(jsonPath("$.welcomeStepKey").value("WELCOME"))
            .andExpect(jsonPath("$.erpEmpresaId").value("erp-123"));
    }

    @Test
    void updateCompany_retorna_400_quando_name_ausente() throws Exception {
        String companyId = criarEmpresa("Nome", "update-noname-instance");

        mockMvc.perform(put("/api/companies/{id}", companyId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"welcomeStepKey\": \"MAIN_MENU\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateCompany_retorna_404_para_empresa_inexistente() throws Exception {
        UpdateCompanyRequest request = new UpdateCompanyRequest("Nome", null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/companies/{id}", "00000000-0000-0000-0000-000000000000")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    // ─── POST /api/companies/{id}/steps ───────────────────────────────────

    @Test
    void addStep_retorna_passo_criado() throws Exception {
        String companyId = criarEmpresa("Clínica", "clinica-step-instance");

        FlowStepRequest stepRequest = new FlowStepRequest(
            "MAIN_MENU", StepType.MENU, "Olá {{nome}}! Escolha:",
            null, null, "AGENDAR", null,
            List.of()
        );

        mockMvc.perform(post("/api/companies/{id}/steps", companyId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stepRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.stepKey").value("MAIN_MENU"))
            .andExpect(jsonPath("$.type").value("MENU"))
            .andExpect(jsonPath("$.defaultNextStepKey").value("AGENDAR"))
            .andExpect(jsonPath("$.transitions").isArray());
    }

    // ─── GET /api/companies/{id}/steps ────────────────────────────────────

    @Test
    void listSteps_retorna_todos_os_passos_da_empresa() throws Exception {
        String companyId = criarEmpresa("Studio", "studio-list-instance");

        FlowStepRequest stepA = new FlowStepRequest("MAIN_MENU", StepType.MENU, "Menu principal", null, null, null, null, List.of());
        FlowStepRequest stepB = new FlowStepRequest("FIM", StepType.END, "Até logo!", null, null, null, null, List.of());

        mockMvc.perform(post("/api/companies/{id}/steps", companyId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stepA)));

        mockMvc.perform(post("/api/companies/{id}/steps", companyId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(stepB)));

        mockMvc.perform(get("/api/companies/{id}/steps", companyId)
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void listSteps_retorna_lista_vazia_para_empresa_sem_passos() throws Exception {
        String companyId = criarEmpresa("Sem Passos", "sem-passos-instance");

        mockMvc.perform(get("/api/companies/{id}/steps", companyId)
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    // ─── PUT /api/companies/{companyId}/steps/{stepId} ────────────────────

    @Test
    void updateStep_atualiza_passo_existente() throws Exception {
        String companyId = criarEmpresa("Empresa", "empresa-update-step-instance");
        String stepId = criarStep(companyId, "MAIN_MENU", StepType.MENU, "Texto antigo");
        criarStep(companyId, "PROXIMO", StepType.END, "Fim");

        UpdateFlowStepRequest request = new UpdateFlowStepRequest(
            StepType.MENU, "Texto novo", null, null, "PROXIMO", null, List.of()
        );

        mockMvc.perform(put("/api/companies/{companyId}/steps/{stepId}", companyId, stepId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.messageTemplate").value("Texto novo"))
            .andExpect(jsonPath("$.defaultNextStepKey").value("PROXIMO"));
    }

    @Test
    void updateStep_retorna_404_quando_defaultNextStepKey_nao_existe() throws Exception {
        String companyId = criarEmpresa("Empresa", "empresa-invalid-ref-instance");
        String stepId = criarStep(companyId, "MAIN_MENU", StepType.MENU, "Menu");

        UpdateFlowStepRequest request = new UpdateFlowStepRequest(
            StepType.MENU, "Menu", null, null, "STEP_INEXISTENTE", null, List.of()
        );

        mockMvc.perform(put("/api/companies/{companyId}/steps/{stepId}", companyId, stepId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateStep_retorna_404_para_step_inexistente() throws Exception {
        String companyId = criarEmpresa("Empresa", "empresa-notstep-instance");

        UpdateFlowStepRequest request = new UpdateFlowStepRequest(
            StepType.MENU, "Menu", null, null, null, null, List.of()
        );

        mockMvc.perform(put("/api/companies/{companyId}/steps/{stepId}", companyId, "00000000-0000-0000-0000-000000000000")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound());
    }

    // ─── DELETE /api/companies/{companyId}/steps/{stepId} ─────────────────

    @Test
    void deleteStep_remove_passo_com_sucesso() throws Exception {
        String companyId = criarEmpresa("Empresa", "empresa-delete-step-instance");
        String stepId = criarStep(companyId, "MAIN_MENU", StepType.MENU, "Menu");

        mockMvc.perform(delete("/api/companies/{companyId}/steps/{stepId}", companyId, stepId)
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/companies/{id}/steps", companyId)
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void deleteStep_retorna_404_para_step_inexistente() throws Exception {
        String companyId = criarEmpresa("Empresa", "empresa-delete-notfound-instance");

        mockMvc.perform(delete("/api/companies/{companyId}/steps/{stepId}", companyId, "00000000-0000-0000-0000-000000000000")
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteStep_retorna_404_step_de_outra_empresa() throws Exception {
        String companyA = criarEmpresa("Empresa A", "empresa-a-instance");
        String companyB = criarEmpresa("Empresa B", "empresa-b-instance");
        String stepIdA = criarStep(companyA, "MAIN_MENU", StepType.MENU, "Menu A");

        mockMvc.perform(delete("/api/companies/{companyId}/steps/{stepId}", companyB, stepIdA)
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isNotFound());
    }

    // ─── informações da empresa ───────────────────────────────────────────

    @Test
    void createCompany_persiste_e_retorna_informacoes_da_empresa() throws Exception {
        CompanyRequest request = new CompanyRequest(
            "Barbearia Tops", "barbearia-tops-instance", null, ChannelType.BAILEYS, "MAIN_MENU", null,
            "Rua das Flores, 123 - Centro", "Seg-Sáb 9h-19h", "(11) 99999-0000"
        );

        mockMvc.perform(post("/api/companies")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endereco").value("Rua das Flores, 123 - Centro"))
            .andExpect(jsonPath("$.horarioFuncionamento").value("Seg-Sáb 9h-19h"))
            .andExpect(jsonPath("$.telefoneContato").value("(11) 99999-0000"));
    }

    @Test
    void updateCompany_atualiza_informacoes_da_empresa() throws Exception {
        String companyId = criarEmpresa("Studio", "studio-info-instance");

        UpdateCompanyRequest request = new UpdateCompanyRequest(
            "Studio Atualizado", null, null, null, null, null,
            "Av. Paulista, 1000", "Ter-Dom 10h-20h", "(11) 88888-0000"
        );

        mockMvc.perform(put("/api/companies/{id}", companyId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endereco").value("Av. Paulista, 1000"))
            .andExpect(jsonPath("$.horarioFuncionamento").value("Ter-Dom 10h-20h"))
            .andExpect(jsonPath("$.telefoneContato").value("(11) 88888-0000"));
    }

    @Test
    void getCompany_retorna_informacoes_da_empresa() throws Exception {
        CompanyRequest request = new CompanyRequest(
            "Clínica Info", "clinica-info-instance", null, ChannelType.BAILEYS, "MAIN_MENU", null,
            "Rua A, 10", "Seg-Sex 8h-18h", "(11) 77777-0000"
        );

        MvcResult created = mockMvc.perform(post("/api/companies")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String companyId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/companies/{id}", companyId)
                .header("X-Internal-Key", INTERNAL_KEY))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.endereco").value("Rua A, 10"))
            .andExpect(jsonPath("$.horarioFuncionamento").value("Seg-Sex 8h-18h"))
            .andExpect(jsonPath("$.telefoneContato").value("(11) 77777-0000"));
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    private String criarEmpresa(String name, String instanceName) throws Exception {
        CompanyRequest request = new CompanyRequest(name, instanceName, null, ChannelType.BAILEYS, "MAIN_MENU", null, null, null, null);
        MvcResult result = mockMvc.perform(post("/api/companies")
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String criarStep(String companyId, String stepKey, StepType type, String template) throws Exception {
        FlowStepRequest request = new FlowStepRequest(stepKey, type, template, null, null, null, null, List.of());
        MvcResult result = mockMvc.perform(post("/api/companies/{id}/steps", companyId)
                .header("X-Internal-Key", INTERNAL_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
