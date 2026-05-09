package ms.chatbot.dias.infrastructure.evolution;

import lombok.RequiredArgsConstructor;
import ms.chatbot.dias.domain.enums.ChannelType;
import ms.chatbot.dias.domain.port.MessagingGateway;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessagingGatewayFactory {

    private final BaileysGateway baileysGateway;
    private final BusinessGateway businessGateway;

    public MessagingGateway getGateway(ChannelType channelType) {
        return switch (channelType) {
            case BAILEYS -> baileysGateway;
            case CLOUD_API -> businessGateway;
        };
    }
}
