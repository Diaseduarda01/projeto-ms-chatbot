package ms.chatbot.dias.infrastructure.erp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "erp")
@Data
public class ErpProperties {
    private String baseUrl;
    private String internalApiKey;
}
