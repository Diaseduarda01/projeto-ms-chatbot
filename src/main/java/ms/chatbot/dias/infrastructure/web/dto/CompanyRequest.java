package ms.chatbot.dias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import ms.chatbot.dias.domain.enums.ChannelType;

public record CompanyRequest(
    @NotBlank String name,
    @NotBlank String evolutionInstanceName,
    String evolutionApiKey,
    ChannelType channelType,
    String welcomeStepKey,
    String erpEmpresaId,
    String endereco,
    String horarioFuncionamento,
    String telefoneContato
) {}
