package ms.chatbot.dias.infrastructure.web.normalizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.model.IncomingMessage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BaileysPayloadNormalizerTest {

    private final BaileysPayloadNormalizer normalizer = new BaileysPayloadNormalizer();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode parse(String json) throws Exception {
        return mapper.readTree(json);
    }

    @Test
    void normalize_extrai_conversation_text() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": false, "remoteJid": "5511999999999@s.whatsapp.net" },
                "pushName": "Maria",
                "message": { "conversation": "Olá" }
              }
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "salao-teste");

        assertThat(result).isPresent();
        assertThat(result.get().text()).isEqualTo("Olá");
        assertThat(result.get().senderPhone()).isEqualTo("5511999999999");
        assertThat(result.get().senderName()).isEqualTo("Maria");
        assertThat(result.get().channelType()).isEqualTo(ChannelType.BAILEYS);
        assertThat(result.get().companyInstanceName()).isEqualTo("salao-teste");
    }

    @Test
    void normalize_extrai_extendedTextMessage() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": false, "remoteJid": "5511999999999@s.whatsapp.net" },
                "pushName": "João",
                "message": { "extendedTextMessage": { "text": "mensagem longa" } }
              }
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "salao-teste");

        assertThat(result).isPresent();
        assertThat(result.get().text()).isEqualTo("mensagem longa");
    }

    @Test
    void normalize_ignora_mensagens_fromMe() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": true, "remoteJid": "5511999999999@s.whatsapp.net" },
                "message": { "conversation": "resposta do bot" }
              }
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "salao-teste");
        assertThat(result).isEmpty();
    }

    @Test
    void normalize_ignora_mensagens_sem_texto() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": false, "remoteJid": "5511999999999@s.whatsapp.net" },
                "message": {}
              }
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "salao-teste");
        assertThat(result).isEmpty();
    }

    @Test
    void normalize_remove_sufixo_jid_do_telefone() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "data": {
                "key": { "fromMe": false, "remoteJid": "5511987654321@s.whatsapp.net" },
                "message": { "conversation": "oi" }
              }
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "salao-teste");

        assertThat(result).isPresent();
        assertThat(result.get().senderPhone()).isEqualTo("5511987654321");
    }

    @Test
    void normalize_resolve_lid_via_senderPn() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "senderPn": "5511999999999@s.whatsapp.net",
              "data": {
                "key": { "fromMe": false, "remoteJid": "abc123@lid" },
                "message": { "conversation": "oi" }
              }
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "salao-teste");

        assertThat(result).isPresent();
        assertThat(result.get().senderPhone()).isEqualTo("5511999999999");
    }

    @Test
    void normalize_resolve_lid_via_sender_quando_senderPn_ausente() throws Exception {
        String payload = """
            {
              "event": "messages.upsert",
              "sender": "5511888888888@s.whatsapp.net",
              "data": {
                "key": { "fromMe": false, "remoteJid": "abc123@lid" },
                "message": { "conversation": "oi" }
              }
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "salao-teste");

        assertThat(result).isPresent();
        assertThat(result.get().senderPhone()).isEqualTo("5511888888888");
    }

    @Test
    void normalize_retorna_vazio_para_payload_invalido() throws Exception {
        String payload = "{}";
        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "salao-teste");
        assertThat(result).isEmpty();
    }
}
