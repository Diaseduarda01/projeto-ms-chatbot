package ms.chatbot.dias.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chatbot.evolution")
@Data
public class EvolutionProperties {
    private String baseUrl;
    private String globalApiKey;
}
