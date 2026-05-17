package ms.chatbot.dias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import ms.chatbot.dias.domain.enums.ChannelType;

public record UpdateCompanyRequest(
    @NotBlank String name,
    String welcomeStepKey,
    Boolean active,
    String erpEmpresaId,
    String evolutionApiKey,
    ChannelType channelType,
    String endereco,
    String horarioFuncionamento,
    String telefoneContato
) {}
