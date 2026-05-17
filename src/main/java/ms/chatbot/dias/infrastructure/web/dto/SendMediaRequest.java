package ms.chatbot.dias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SendMediaRequest(
    @NotBlank String mediaType,
    @NotBlank String media,
    String caption,
    String fileName
) {}
