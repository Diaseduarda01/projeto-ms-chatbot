package ms.chatbot.dias.infrastructure.web.normalizer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.model.IncomingMessage;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class BusinessPayloadNormalizer implements PayloadNormalizer {

    @Override
    public Optional<IncomingMessage> normalize(JsonNode payload, String instanceName) {
        try {
            JsonNode entry = payload.path("entry").path(0);
            JsonNode changes = entry.path("changes").path(0);
            JsonNode value = changes.path("value");
            JsonNode message = value.path("messages").path(0);

            if (message.isMissingNode()) return Optional.empty();

            String type = message.path("type").asText("");
            if (!type.equals("text")) return Optional.empty();

            String phone = message.path("from").asText(null);
            String text = message.path("text").path("body").asText(null);

            if (phone == null || text == null || text.isBlank()) return Optional.empty();

            String name = value.path("contacts").path(0)
                .path("profile").path("name").asText(null);

            return Optional.of(new IncomingMessage(instanceName, phone, name, text, ChannelType.CLOUD_API));
        } catch (Exception e) {
            log.error("Erro ao normalizar payload Business: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
