package ms.chatbot.dias.infrastructure.web.normalizer;

import com.fasterxml.jackson.databind.JsonNode;
import ms.chatbot.dias.domain.model.IncomingMessage;

import java.util.Optional;

public interface PayloadNormalizer {
    Optional<IncomingMessage> normalize(JsonNode payload, String instanceName);
}
