package ms.chatbot.dias.infrastructure.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.application.usecase.ProcessMessageUseCase;
import ms.chatbot.dias.domain.model.IncomingMessage;
import ms.chatbot.dias.infrastructure.web.normalizer.BaileysPayloadNormalizer;
import ms.chatbot.dias.infrastructure.web.normalizer.BusinessPayloadNormalizer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final ProcessMessageUseCase processMessageUseCase;
    private final BaileysPayloadNormalizer baileysNormalizer;
    private final BusinessPayloadNormalizer businessNormalizer;
    private final ObjectMapper objectMapper;
    private final WebhookRateLimiter rateLimiter;

    @PostMapping("/{instanceName}")
    public ResponseEntity<Void> receive(
            @PathVariable String instanceName,
            @RequestBody String rawPayload) {

        log.debug("Webhook recebido | instância: {}", instanceName);

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            String event = payload.path("event").asText("").toUpperCase().replace('.', '_');

            if (!event.equals("MESSAGES_UPSERT")) {
                log.debug("Evento ignorado: {}", event);
                return ResponseEntity.ok().build();
            }

            Optional<IncomingMessage> message = tryNormalize(payload, instanceName);
            if (message.isPresent()) {
                if (rateLimiter.allow(message.get().senderPhone())) {
                    processMessageUseCase.execute(message.get());
                } else {
                    log.debug("Rate limit: mensagem ignorada de {} | instância: {}", message.get().senderPhone(), instanceName);
                }
            }

        } catch (Exception e) {
            log.error("Erro ao processar webhook da instância {}: {}", instanceName, e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    @PostMapping("/business/{instanceName}")
    public ResponseEntity<Void> receiveBusinessApi(
            @PathVariable String instanceName,
            @RequestBody String rawPayload) {

        log.debug("Webhook Business recebido | instância: {}", instanceName);

        try {
            JsonNode payload = objectMapper.readTree(rawPayload);
            businessNormalizer.normalize(payload, instanceName)
                .ifPresent(processMessageUseCase::execute);
        } catch (Exception e) {
            log.error("Erro ao processar webhook Business da instância {}: {}", instanceName, e.getMessage(), e);
        }

        return ResponseEntity.ok().build();
    }

    private Optional<IncomingMessage> tryNormalize(JsonNode payload, String instanceName) {
        return baileysNormalizer.normalize(payload, instanceName);
    }
}
