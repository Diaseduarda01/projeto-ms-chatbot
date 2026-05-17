package ms.chatbot.dias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EnviarMensagemRequest(
    @NotBlank String texto
) {}
