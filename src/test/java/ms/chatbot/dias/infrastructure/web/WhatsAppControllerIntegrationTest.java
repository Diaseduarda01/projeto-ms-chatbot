package ms.chatbot.dias.infrastructure.web;

import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.infrastructure.evolution.EvolutionConnectResult;
import ms.chatbot.dias.infrastructure.evolution.EvolutionHttpClient;
import ms.chatbot.dias.infrastructure.persistence.jpa.CompanyJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

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
        when(evolutionHttpClient.connectInstance("barbearia-ws-instance", null))
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
}
