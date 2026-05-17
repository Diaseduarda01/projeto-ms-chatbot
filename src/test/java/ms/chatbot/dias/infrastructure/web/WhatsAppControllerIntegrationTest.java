package ms.chatbot.dias.infrastructure.web;

import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.infrastructure.evolution.EvolutionConnectResult;
import ms.chatbot.dias.infrastructure.evolution.EvolutionHttpClient;
import ms.chatbot.dias.infrastructure.evolution.EvolutionInstanceSettings;
import ms.chatbot.dias.infrastructure.persistence.jpa.CompanyJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WhatsAppControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CompanyJpaRepository companyJpaRepository;
    @MockBean EvolutionHttpClient evolutionHttpClient;
    @MockBean SimpMessagingTemplate messagingTemplate;

    Company company;

    @BeforeEach
    void setUp() {
        company = companyJpaRepository.save(Company.builder()
            .name("Barbearia WhatsApp")
            .evolutionInstanceName("barbearia-ws-instance")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .active(true)
            .build());
    }

    @AfterEach
    void tearDown() {
        companyJpaRepository.deleteAll();
    }

    // --- GET /api/whatsapp/status ---

    @Test
    void status_retorna_estado_open_da_instancia() throws Exception {
        when(evolutionHttpClient.getConnectionState("barbearia-ws-instance", null))
            .thenReturn("open");

        mockMvc.perform(get("/api/whatsapp/status")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").value("open"));
    }

    @Test
    void status_retorna_403_sem_internal_key() throws Exception {
        mockMvc.perform(get("/api/whatsapp/status")
                .param("companyId", company.getId().toString()))
            .andExpect(status().isForbidden());

        verifyNoInteractions(evolutionHttpClient);
    }

    @Test
    void status_retorna_403_com_chave_errada() throws Exception {
        mockMvc.perform(get("/api/whatsapp/status")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "chave-errada"))
            .andExpect(status().isForbidden());

        verifyNoInteractions(evolutionHttpClient);
    }

    @Test
    void status_retorna_404_para_empresa_inexistente() throws Exception {
        mockMvc.perform(get("/api/whatsapp/status")
                .param("companyId", UUID.randomUUID().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isNotFound());
    }

    // --- POST /api/whatsapp/connect ---

    @Test
    void connect_retorna_qrcode_quando_instancia_desconectada() throws Exception {
        when(evolutionHttpClient.connectInstance("barbearia-ws-instance", null))
            .thenReturn(new EvolutionConnectResult(false, "data:image/png;base64,abc123", "AAAA-BBBB"));

        mockMvc.perform(post("/api/whatsapp/connect")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connected").value(false))
            .andExpect(jsonPath("$.qrCode").value("data:image/png;base64,abc123"))
            .andExpect(jsonPath("$.pairingCode").value("AAAA-BBBB"));
    }

    @Test
    void connect_retorna_connected_true_quando_ja_conectado() throws Exception {
        when(evolutionHttpClient.connectInstance("barbearia-ws-instance", null))
            .thenReturn(new EvolutionConnectResult(true, null, null));

        mockMvc.perform(post("/api/whatsapp/connect")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connected").value(true));
    }

    @Test
    void connect_retorna_403_sem_internal_key() throws Exception {
        mockMvc.perform(post("/api/whatsapp/connect")
                .param("companyId", company.getId().toString()))
            .andExpect(status().isForbidden());
    }

    // --- GET /api/whatsapp/qrcode ---

    @Test
    void qrcode_retorna_base64_quando_disponivel() throws Exception {
        when(evolutionHttpClient.fetchQrCode("barbearia-ws-instance", null))
            .thenReturn(new EvolutionConnectResult(false, "data:image/png;base64,xyz", null));

        mockMvc.perform(get("/api/whatsapp/qrcode")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.connected").value(false))
            .andExpect(jsonPath("$.qrCode").value("data:image/png;base64,xyz"));
    }

    // --- POST /api/whatsapp/disconnect ---

    @Test
    void disconnect_retorna_204_e_chama_evolution() throws Exception {
        mockMvc.perform(post("/api/whatsapp/disconnect")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isNoContent());

        verify(evolutionHttpClient).disconnectInstance("barbearia-ws-instance", null);
    }

    @Test
    void disconnect_retorna_403_sem_internal_key() throws Exception {
        mockMvc.perform(post("/api/whatsapp/disconnect")
                .param("companyId", company.getId().toString()))
            .andExpect(status().isForbidden());
    }

    // --- POST /api/whatsapp/restart ---

    @Test
    void restart_retorna_204_e_chama_evolution() throws Exception {
        mockMvc.perform(post("/api/whatsapp/restart")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isNoContent());

        verify(evolutionHttpClient).restartInstance("barbearia-ws-instance", null);
    }

    @Test
    void restart_retorna_404_para_empresa_inexistente() throws Exception {
        mockMvc.perform(post("/api/whatsapp/restart")
                .param("companyId", UUID.randomUUID().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isNotFound());
    }

    // --- DELETE /api/whatsapp/instance ---

    @Test
    void deleteInstance_retorna_204_e_chama_evolution() throws Exception {
        mockMvc.perform(delete("/api/whatsapp/instance")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isNoContent());

        verify(evolutionHttpClient).deleteInstance("barbearia-ws-instance", null);
    }

    // --- GET /api/whatsapp/settings ---

    @Test
    void getSettings_retorna_configuracoes_da_instancia() throws Exception {
        when(evolutionHttpClient.getSettings("barbearia-ws-instance", null))
            .thenReturn(new EvolutionInstanceSettings(true, "Não atendemos chamadas", true));

        mockMvc.perform(get("/api/whatsapp/settings")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rejectCall").value(true))
            .andExpect(jsonPath("$.msgCall").value("Não atendemos chamadas"))
            .andExpect(jsonPath("$.groupsIgnore").value(true));
    }

    // --- POST /api/whatsapp/settings ---

    @Test
    void setSettings_salva_e_retorna_configuracoes() throws Exception {
        when(evolutionHttpClient.setSettings("barbearia-ws-instance", null, true, "Sem chamadas", true))
            .thenReturn(new EvolutionInstanceSettings(true, "Sem chamadas", true));

        mockMvc.perform(post("/api/whatsapp/settings")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"rejectCall":true,"msgCall":"Sem chamadas","groupsIgnore":true}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rejectCall").value(true))
            .andExpect(jsonPath("$.msgCall").value("Sem chamadas"));
    }

    // --- POST /api/whatsapp/check-number ---

    @Test
    void checkNumber_retorna_true_quando_numero_existe() throws Exception {
        when(evolutionHttpClient.checkNumber("barbearia-ws-instance", null, "11999999999"))
            .thenReturn(true);

        mockMvc.perform(post("/api/whatsapp/check-number")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"number":"11999999999"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.number").value("11999999999"))
            .andExpect(jsonPath("$.exists").value(true));
    }

    @Test
    void checkNumber_retorna_false_quando_numero_nao_existe() throws Exception {
        when(evolutionHttpClient.checkNumber("barbearia-ws-instance", null, "11000000000"))
            .thenReturn(false);

        mockMvc.perform(post("/api/whatsapp/check-number")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"number":"11000000000"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exists").value(false));
    }

    // --- GET /api/whatsapp/profile ---

    @Test
    void getProfile_retorna_dados_do_perfil() throws Exception {
        when(evolutionHttpClient.getProfile("barbearia-ws-instance", null, "11999999999"))
            .thenReturn(Map.of("name", "Barbearia Silva", "status", "Atendemos seg-sáb", "picture", "https://pic.url"));

        mockMvc.perform(get("/api/whatsapp/profile")
                .param("companyId", company.getId().toString())
                .param("number", "11999999999")
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Barbearia Silva"))
            .andExpect(jsonPath("$.status").value("Atendemos seg-sáb"))
            .andExpect(jsonPath("$.pictureUrl").value("https://pic.url"));
    }

    // --- PUT /api/whatsapp/profile ---

    @Test
    void updateProfile_retorna_204_e_chama_metodos_correspondentes() throws Exception {
        mockMvc.perform(put("/api/whatsapp/profile")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"Novo Nome","status":"Novo status","pictureUrl":"https://nova.url/foto.jpg"}
                    """))
            .andExpect(status().isNoContent());

        verify(evolutionHttpClient).updateProfileName("barbearia-ws-instance", null, "Novo Nome");
        verify(evolutionHttpClient).updateProfileStatus("barbearia-ws-instance", null, "Novo status");
        verify(evolutionHttpClient).updateProfilePicture("barbearia-ws-instance", null, "https://nova.url/foto.jpg");
    }

    // --- POST /api/whatsapp/mensagem ---

    @Test
    void mensagemProativa_retorna_204_e_envia_texto() throws Exception {
        mockMvc.perform(post("/api/whatsapp/mensagem")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"number":"11988887777","texto":"Olá, lembrete de agendamento!"}
                    """))
            .andExpect(status().isNoContent());

        verify(evolutionHttpClient).sendText("barbearia-ws-instance", null,
            "11988887777", "Olá, lembrete de agendamento!");
    }

    // --- GET /api/whatsapp/chats ---

    @Test
    void findChats_retorna_lista_de_chats() throws Exception {
        when(evolutionHttpClient.findChats("barbearia-ws-instance", null))
            .thenReturn(List.of(Map.of("id", "5511999@s.whatsapp.net", "name", "João")));

        mockMvc.perform(get("/api/whatsapp/chats")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("João"));
    }

    // --- GET /api/whatsapp/privacy ---

    @Test
    void getPrivacy_retorna_configuracoes_de_privacidade() throws Exception {
        when(evolutionHttpClient.getPrivacy("barbearia-ws-instance", null))
            .thenReturn(Map.of(
                "readreceipts", "all",
                "profile", "contacts",
                "status", "all",
                "online", "all",
                "last", "contacts",
                "groupadd", "all"
            ));

        mockMvc.perform(get("/api/whatsapp/privacy")
                .param("companyId", company.getId().toString())
                .header("X-Internal-Key", "test-internal-key"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.readreceipts").value("all"))
            .andExpect(jsonPath("$.profile").value("contacts"));
    }
}
