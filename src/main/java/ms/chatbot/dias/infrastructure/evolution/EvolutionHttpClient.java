package ms.chatbot.dias.infrastructure.evolution;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import ms.chatbot.dias.infrastructure.config.EvolutionProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EvolutionHttpClient {

    private final RestClient restClient;
    private final EvolutionProperties properties;

    public void sendText(String instanceName, String apiKey, String phoneNumber, String text) {
        String url = properties.getBaseUrl() + "/message/sendText/" + instanceName;
        String key = resolveApiKey(apiKey);

        var body = Map.of(
            "number", sanitizeNumber(phoneNumber),
            "text", text,
            "options", Map.of("delay", 1200, "presence", "composing")
        );

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                restClient.post()
                    .uri(url)
                    .header("apikey", key)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
                return;
            } catch (Exception e) {
                log.warn("Tentativa {}/{} falhou para {}: {}", attempt, maxRetries, phoneNumber, e.getMessage());
                if (attempt < maxRetries) sleep(500L * attempt);
            }
        }

        log.error("Falha ao enviar mensagem para {} após {} tentativas", phoneNumber, maxRetries);
    }

    private String resolveApiKey(String instanceApiKey) {
        return (instanceApiKey != null && !instanceApiKey.isBlank())
            ? instanceApiKey
            : properties.getGlobalApiKey();
    }

    private String sanitizeNumber(String phone) {
        return phone.replaceAll("[^0-9]", "");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
