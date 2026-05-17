package ms.chatbot.dias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckNumberRequest(@NotBlank String number) {}
