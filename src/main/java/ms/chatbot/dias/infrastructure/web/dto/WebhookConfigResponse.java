package ms.chatbot.dias.infrastructure.web.dto;

import java.util.List;

public record WebhookConfigResponse(String url, boolean enabled, List<String> events) {}
