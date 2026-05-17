package ms.chatbot.dias.domain.entity;

import ms.chatbot.dias.domain.enums.SessionStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    @Test
    void newSession_cria_com_status_NEW() {
        UUID companyId = UUID.randomUUID();
        Session session = Session.newSession(companyId, "5511999999999", "MAIN_MENU");

        assertThat(session.getStatus()).isEqualTo(SessionStatus.NEW);
        assertThat(session.getCompanyId()).isEqualTo(companyId);
        assertThat(session.getPhoneNumber()).isEqualTo("5511999999999");
        assertThat(session.getCurrentStepKey()).isEqualTo("MAIN_MENU");
        assertThat(session.getId()).isNull();
    }

    @Test
    void activate_muda_status_para_ACTIVE() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        session.activate();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
    }

    @Test
    void complete_muda_status_para_COMPLETED() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        session.complete();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
    }

    @Test
    void storeData_persiste_par_chave_valor() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        session.storeData("nome", "João");
        assertThat(session.getData("nome")).isEqualTo("João");
    }

    @Test
    void storeData_sobrescreve_valor_existente() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        session.storeData("cpf", "00000000000");
        session.storeData("cpf", "12345678901");
        assertThat(session.getData("cpf")).isEqualTo("12345678901");
    }

    @Test
    void storeData_suporta_multiplas_chaves() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        session.storeData("nome", "Ana");
        session.storeData("data", "10/06/2025");
        assertThat(session.getCollectedDataAsMap())
            .containsEntry("nome", "Ana")
            .containsEntry("data", "10/06/2025");
    }

    @Test
    void getData_retorna_null_para_chave_inexistente() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        assertThat(session.getData("inexistente")).isNull();
    }

    @Test
    void getCollectedDataAsMap_retorna_mapa_vazio_por_padrao() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        assertThat(session.getCollectedDataAsMap()).isEmpty();
    }

    @Test
    void getCollectedDataAsMap_retorna_mapa_vazio_para_json_invalido() {
        Session session = Session.builder()
            .companyId(UUID.randomUUID())
            .phoneNumber("5511999999999")
            .currentStepKey("MAIN_MENU")
            .collectedData("json_invalido")
            .build();
        assertThat(session.getCollectedDataAsMap()).isEmpty();
    }

    @Test
    void handoff_muda_status_para_HANDOFF() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        session.activate();
        session.handoff();
        assertThat(session.getStatus()).isEqualTo(SessionStatus.HANDOFF);
    }

    @Test
    void reativar_muda_status_para_ACTIVE_mantendo_step_atual() {
        Session session = Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
        session.activate();
        session.setCurrentStepKey("COLLECT_NAME");
        session.handoff();

        session.reativar();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getCurrentStepKey()).isEqualTo("COLLECT_NAME");
    }
}
