package ms.chatbot.dias.domain.port;

import ms.chatbot.dias.domain.entity.Company;

public interface MessagingGateway {
    void sendText(String phoneNumber, String text, Company company);
    void sendMedia(String phoneNumber, String mediaType, String media,
                   String caption, String fileName, Company company);
}
