package ms.chatbot.dias.infrastructure.web.dto;

public record UpdatePrivacyRequest(
    String readreceipts,
    String profile,
    String status,
    String online,
    String last,
    String groupadd
) {}
