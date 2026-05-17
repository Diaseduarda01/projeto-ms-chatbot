package ms.chatbot.dias.infrastructure.web.dto;

import java.util.List;

public record WebhookConfigRequest(String url, List<String> events) {}
