package ms.chatbot.dias.infrastructure.web.normalizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.model.IncomingMessage;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessPayloadNormalizerTest {

    private final BusinessPayloadNormalizer normalizer = new BusinessPayloadNormalizer();
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode parse(String json) throws Exception {
        return mapper.readTree(json);
    }

    @Test
    void normalize_extrai_mensagem_de_texto() throws Exception {
        String payload = """
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "messages": [{
                      "from": "5511999999999",
                      "type": "text",
                      "text": { "body": "Quero agendar" }
                    }],
                    "contacts": [{ "profile": { "name": "Ana" } }]
                  }
                }]
              }]
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "waba-instancia");

        assertThat(result).isPresent();
        assertThat(result.get().text()).isEqualTo("Quero agendar");
        assertThat(result.get().senderPhone()).isEqualTo("5511999999999");
        assertThat(result.get().senderName()).isEqualTo("Ana");
        assertThat(result.get().channelType()).isEqualTo(ChannelType.CLOUD_API);
        assertThat(result.get().companyInstanceName()).isEqualTo("waba-instancia");
    }

    @Test
    void normalize_ignora_tipo_nao_texto() throws Exception {
        String payload = """
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "messages": [{
                      "from": "5511999999999",
                      "type": "image",
                      "image": {}
                    }]
                  }
                }]
              }]
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "waba-instancia");
        assertThat(result).isEmpty();
    }

    @Test
    void normalize_retorna_vazio_quando_messages_ausente() throws Exception {
        String payload = """
            {
              "entry": [{
                "changes": [{
                  "value": {}
                }]
              }]
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "waba-instancia");
        assertThat(result).isEmpty();
    }

    @Test
    void normalize_ignora_texto_em_branco() throws Exception {
        String payload = """
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "messages": [{
                      "from": "5511999999999",
                      "type": "text",
                      "text": { "body": "" }
                    }]
                  }
                }]
              }]
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "waba-instancia");
        assertThat(result).isEmpty();
    }

    @Test
    void normalize_aceita_contato_sem_nome() throws Exception {
        String payload = """
            {
              "entry": [{
                "changes": [{
                  "value": {
                    "messages": [{
                      "from": "5511999999999",
                      "type": "text",
                      "text": { "body": "Oi" }
                    }],
                    "contacts": []
                  }
                }]
              }]
            }
            """;

        Optional<IncomingMessage> result = normalizer.normalize(parse(payload), "waba-instancia");

        assertThat(result).isPresent();
        assertThat(result.get().senderName()).isNull();
    }

    @Test
    void normalize_retorna_vazio_para_payload_vazio() throws Exception {
        Optional<IncomingMessage> result = normalizer.normalize(parse("{}"), "waba-instancia");
        assertThat(result).isEmpty();
    }
}
