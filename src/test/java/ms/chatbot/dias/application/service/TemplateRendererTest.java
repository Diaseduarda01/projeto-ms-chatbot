package ms.chatbot.dias.application.service;

import ms.chatbot.dias.domain.entity.Session;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateRendererTest {

    private final TemplateRenderer renderer = new TemplateRenderer();

    private Session newSession() {
        return Session.newSession(UUID.randomUUID(), "5511999999999", "MAIN_MENU");
    }

    @Test
    void render_substitui_placeholder_nome() {
        Session session = newSession();
        String result = renderer.render("Olá, {{nome}}!", session, "Maria");
        assertThat(result).isEqualTo("Olá, Maria!");
    }

    @Test
    void render_substitui_nome_vazio_quando_name_e_null() {
        Session session = newSession();
        String result = renderer.render("Olá, {{nome}}!", session, null);
        assertThat(result).isEqualTo("Olá, !");
    }

    @Test
    void render_substitui_nome_vazio_quando_name_e_blank() {
        Session session = newSession();
        String result = renderer.render("Olá, {{nome}}!", session, "  ");
        assertThat(result).isEqualTo("Olá, !");
    }

    @Test
    void render_substitui_dados_da_sessao() {
        Session session = newSession();
        session.storeData("servico", "Corte");
        String result = renderer.render("Você escolheu: {{servico}}", session, null);
        assertThat(result).isEqualTo("Você escolheu: Corte");
    }

    @Test
    void render_substitui_multiplos_placeholders() {
        Session session = newSession();
        session.storeData("data", "10/06/2025");
        session.storeData("hora", "14:00");
        String result = renderer.render("Agendado para {{data}} às {{hora}}, {{nome}}!", session, "Ana");
        assertThat(result).isEqualTo("Agendado para 10/06/2025 às 14:00, Ana!");
    }

    @Test
    void render_mantem_placeholder_desconhecido_inalterado() {
        Session session = newSession();
        String result = renderer.render("Valor: {{desconhecido}}", session, null);
        assertThat(result).isEqualTo("Valor: {{desconhecido}}");
    }

    @Test
    void render_template_sem_placeholders_retorna_inalterado() {
        Session session = newSession();
        String result = renderer.render("Até logo!", session, "João");
        assertThat(result).isEqualTo("Até logo!");
    }
}
