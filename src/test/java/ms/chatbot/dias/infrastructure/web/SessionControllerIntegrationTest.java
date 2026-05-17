package ms.chatbot.dias.infrastructure.web;

import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.Session;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.enums.MessageDirection;
import ms.chatbot.dias.domain.enums.SessionStatus;
import ms.chatbot.dias.infrastructure.evolution.EvolutionHttpClient;
import ms.chatbot.dias.infrastructure.persistence.jpa.CompanyJpaRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.MessageJpaRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.SessionJpaRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired SessionJpaRepository sessionJpaRepository;
    @Autowired CompanyJpaRepository companyJpaRepository;
    @Autowired MessageJpaRepository messageJpaRepository;
    @MockBean EvolutionHttpClient evolutionHttpClient;
    @MockBean SimpMessagingTemplate messagingTemplate;

    Company company;
    Session session;

    @BeforeEach
    void setUp() {
        company = companyJpaRepository.save(Company.builder()
            .name("Barbearia Handoff")
            .evolutionInstanceName("barbearia-handoff-instance")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .active(true)
            .build());

        session = sessionJpaRepository.save(Session.newSession(
            company.getId(), "5511999999999", "MAIN_MENU"
        ));
        session.activate();
        session = sessionJpaRepository.save(session);
    }

    @AfterEach
    void tearDown() {
        messageJpaRepository.deleteAll();
        sessionJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    @Test
    void handoff_muda_status_para_HANDOFF_e_retorna_204() throws Exception {
        mockMvc.perform(patch("/api/sessions/{id}/handoff", session.getId()))
            .andExpect(status().isNoContent());

        Session updated = sessionJpaRepository.findById(session.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SessionStatus.HANDOFF);
    }

    @Test
    void reativar_muda_status_para_ACTIVE_e_retorna_204() throws Exception {
        session.handoff();
        sessionJpaRepository.save(session);

        mockMvc.perform(patch("/api/sessions/{id}/reativar", session.getId()))
            .andExpect(status().isNoContent());

        Session updated = sessionJpaRepository.findById(session.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void reativar_mantem_step_atual_inalterado() throws Exception {
        session.setCurrentStepKey("COLLECT_NAME");
        session.handoff();
        sessionJpaRepository.save(session);

        mockMvc.perform(patch("/api/sessions/{id}/reativar", session.getId()))
            .andExpect(status().isNoContent());

        Session updated = sessionJpaRepository.findById(session.getId()).orElseThrow();
        assertThat(updated.getCurrentStepKey()).isEqualTo("COLLECT_NAME");
    }

    @Test
    void handoff_retorna_404_para_sessao_inexistente() throws Exception {
        mockMvc.perform(patch("/api/sessions/{id}/handoff", UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    void reativar_retorna_404_para_sessao_inexistente() throws Exception {
        mockMvc.perform(patch("/api/sessions/{id}/reativar", UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    void listByCompany_retorna_sessoes_com_status_correto() throws Exception {
        mockMvc.perform(get("/api/sessions").param("companyId", company.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].phoneNumber").value("5511999999999"))
            .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    // --- POST /{id}/mensagem ---

    @Test
    void enviarMensagem_retorna_204_e_grava_outbound_quando_sessao_em_handoff() throws Exception {
        session.handoff();
        sessionJpaRepository.save(session);

        mockMvc.perform(post("/api/sessions/{id}/mensagem", session.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "texto": "Olá! Em que posso ajudar?" }
                    """))
            .andExpect(status().isNoContent());

        var messages = messageJpaRepository.findAll();
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).getDirection()).isEqualTo(MessageDirection.OUTBOUND);
        assertThat(messages.get(0).getText()).isEqualTo("Olá! Em que posso ajudar?");
    }

    @Test
    void enviarMensagem_retorna_409_quando_sessao_nao_esta_em_handoff() throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/mensagem", session.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "texto": "Mensagem não autorizada" }
                    """))
            .andExpect(status().isConflict());
    }

    @Test
    void enviarMensagem_retorna_404_para_sessao_inexistente() throws Exception {
        mockMvc.perform(post("/api/sessions/{id}/mensagem", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "texto": "oi" }
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void enviarMensagem_retorna_400_quando_texto_esta_vazio() throws Exception {
        session.handoff();
        sessionJpaRepository.save(session);

        mockMvc.perform(post("/api/sessions/{id}/mensagem", session.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    { "texto": "" }
                    """))
            .andExpect(status().isBadRequest());
    }
}
