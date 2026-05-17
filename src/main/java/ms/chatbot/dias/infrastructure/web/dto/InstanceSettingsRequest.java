package ms.chatbot.dias.infrastructure.web.dto;

public record InstanceSettingsRequest(boolean rejectCall, String msgCall, boolean groupsIgnore) {}
