package ms.chatbot.dias.infrastructure.web;

import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.entity.FlowStep;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.enums.StepType;
import ms.chatbot.dias.domain.port.CompanyRepository;
import ms.chatbot.dias.domain.port.FlowStepRepository;
import ms.chatbot.dias.infrastructure.evolution.EvolutionHttpClient;
import ms.chatbot.dias.infrastructure.persistence.jpa.CompanyJpaRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.FlowStepJpaRepository;
import ms.chatbot.dias.infrastructure.persistence.jpa.SessionJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WebhookControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired CompanyRepository companyRepository;
    @Autowired FlowStepRepository flowStepRepository;
    @Autowired SessionJpaRepository sessionJpaRepository;
    @Autowired FlowStepJpaRepository flowStepJpaRepository;
    @Autowired CompanyJpaRepository companyJpaRepository;
    @MockBean EvolutionHttpClient evolutionHttpClient;

    Company baileysCompany;

    @BeforeEach
    void setUp() {
        doNothing().when(evolutionHttpClient).sendText(any(), any(), any(), any());

        baileysCompany = companyRepository.save(Company.builder()
            .name("Webhook Test Empresa")
            .evolutionInstanceName("webhook-baileys-instance")
            .evolutionApiKey("test-api-key")
            .channelType(ChannelType.BAILEYS)
            .welcomeStepKey("MAIN_MENU")
            .build());

        flowStepRepository.save(FlowStep.builder()
            .companyId(baileysCompany.getId())
            .stepKey("MAIN_MENU")
            .type(StepType.MENU)
            .messageTemplate("Bem-vindo! Escolha: 1-Agendar")
            .transitions(List.of())
            .build());
    }

    @AfterEach
    void tearDown() {
        sessionJpaRepository.deleteAll();
        flowStepJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
    }

    @Test
    void webhook_retorna_ok_para_evento_nao_upsert() throws Exception {
        String payload = """
            {
              "event": "connection.update",
              "data": {}
            }
            """;

        mockMvc.perform(post("/webhook/webhook-baileys-instance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verifyNoInteractions(evolutionHttpClient);
    }

    @Test
    void webhook_retorna_ok_para_instancia_desconhecida() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": false, "remoteJid": "5511999999999@s.whatsapp.net" },
                "pushName": "Teste",
                "message": { "conversation": "Oi" }
              }
            }
            """;

        mockMvc.perform(post("/webhook/instancia-inexistente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verifyNoInteractions(evolutionHttpClient);
    }

    @Test
    void webhook_processa_nova_sessao_e_envia_boas_vindas() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": false, "remoteJid": "5511888888881@s.whatsapp.net" },
                "pushName": "Cliente Novo",
                "message": { "conversation": "Oi" }
              }
            }
            """;

        mockMvc.perform(post("/webhook/webhook-baileys-instance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verify(evolutionHttpClient).sendText(
            eq("webhook-baileys-instance"),
            eq("test-api-key"),
            eq("5511888888881"),
            any()
        );
    }

    @Test
    void webhook_ignora_mensagens_fromMe() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": true, "remoteJid": "5511999999999@s.whatsapp.net" },
                "pushName": "Bot",
                "message": { "conversation": "Mensagem do bot" }
              }
            }
            """;

        mockMvc.perform(post("/webhook/webhook-baileys-instance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verifyNoInteractions(evolutionHttpClient);
    }

    @Test
    void webhook_ignora_mensagem_vazia() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": false, "remoteJid": "5511999999999@s.whatsapp.net" },
                "message": {}
              }
            }
            """;

        mockMvc.perform(post("/webhook/webhook-baileys-instance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verifyNoInteractions(evolutionHttpClient);
    }

    @Test
    void webhook_business_processa_mensagem_e_envia_resposta() throws Exception {
        Company businessCompany = companyRepository.save(Company.builder()
            .name("Business Empresa")
            .evolutionInstanceName("business-waba-instance")
            .evolutionApiKey("waba-key")
            .channelType(ChannelType.CLOUD_API)
            .welcomeStepKey("MAIN_MENU")
            .build());

        flowStepRepository.save(FlowStep.builder()
            .companyId(businessCompany.getId())
            .stepKey("MAIN_MENU")
            .type(StepType.MENU)
            .messageTemplate("Olá via WABA!")
            .transitions(List.of())
            .build());

        String payload = """
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "messages": [{
                      "from": "5511777777771",
                      "type": "text",
                      "text": { "body": "Quero agendar" }
                    }],
                    "contacts": [{ "profile": { "name": "Cliente WABA" } }]
                  }
                }]
              }]
            }
            """;

        mockMvc.perform(post("/webhook/business/business-waba-instance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verify(evolutionHttpClient).sendText(
            eq("business-waba-instance"),
            eq("waba-key"),
            eq("5511777777771"),
            any()
        );
    }

    @Test
    void webhook_business_retorna_ok_para_tipo_nao_texto() throws Exception {
        String payload = """
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "messages": [{
                      "from": "5511777777777",
                      "type": "image",
                      "image": {}
                    }]
                  }
                }]
              }]
            }
            """;

        mockMvc.perform(post("/webhook/business/webhook-baileys-instance")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
            .andExpect(status().isOk());

        verifyNoInteractions(evolutionHttpClient);
    }
}
