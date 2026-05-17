package ms.chatbot.dias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ProativaMessageRequest(
    @NotBlank String number,
    @NotBlank String texto
) {}
