package ms.chatbot.dias.infrastructure.web.normalizer;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.model.IncomingMessage;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class BaileysPayloadNormalizer implements PayloadNormalizer {

    @Override
    public Optional<IncomingMessage> normalize(JsonNode payload, String instanceName) {
        try {
            JsonNode data = payload.path("data");
            JsonNode key = data.path("key");

            boolean fromMe = key.path("fromMe").asBoolean(false);
            if (fromMe) return Optional.empty();

            String senderJid = extractSenderJid(payload, data, key);
            if (senderJid == null) return Optional.empty();

            String text = extractText(data);
            if (text == null || text.isBlank()) return Optional.empty();

            String phone = normalizePhone(senderJid);
            String name = data.path("pushName").asText(null);

            return Optional.of(new IncomingMessage(instanceName, phone, name, text, ChannelType.BAILEYS));
        } catch (Exception e) {
            log.error("Erro ao normalizar payload Baileys: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private String extractSenderJid(JsonNode root, JsonNode data, JsonNode key) {
        String jid = key.path("remoteJid").asText(null);

        if (jid != null && jid.endsWith("@lid")) {
            jid = tryResolveLid(root, jid);
        }

        return jid;
    }

    private String tryResolveLid(JsonNode root, String fallback) {
        String senderPn = root.path("senderPn").asText(null);
        if (senderPn != null && senderPn.endsWith("@s.whatsapp.net")) return senderPn;

        String sender = root.path("sender").asText(null);
        if (sender != null && sender.endsWith("@s.whatsapp.net")) return sender;

        return fallback;
    }

    private String extractText(JsonNode data) {
        JsonNode msg = data.path("message");
        if (msg.has("conversation")) return msg.path("conversation").asText(null);
        if (msg.has("extendedTextMessage"))
            return msg.path("extendedTextMessage").path("text").asText(null);
        return null;
    }

    private String normalizePhone(String jid) {
        return jid.replaceAll("@.*", "").replaceAll("[^0-9]", "");
    }
}
