package ms.chatbot.dias.infrastructure.evolution;

import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.entity.Company;
import ms.chatbot.dias.domain.port.MessagingGateway;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BaileysGateway implements MessagingGateway {

    private final EvolutionHttpClient evolutionClient;

    @Override
    public void sendText(String phoneNumber, String text, Company company) {
        evolutionClient.sendText(
            company.getEvolutionInstanceName(),
            company.getEvolutionApiKey(),
            phoneNumber,
            text
        );
    }
}
