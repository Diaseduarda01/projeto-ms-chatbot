package ms.chatbot.dias.infrastructure.evolution;

import java.util.List;

public record EvolutionWebhookConfig(String url, boolean enabled, List<String> events) {}
