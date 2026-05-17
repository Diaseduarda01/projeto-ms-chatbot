package ms.chatbot.dias.infrastructure.web.dto;

public record InstanceSettingsResponse(boolean rejectCall, String msgCall, boolean groupsIgnore) {}
