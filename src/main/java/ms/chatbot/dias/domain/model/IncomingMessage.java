package ms.chatbot.dias.domain.model;

import ms.chatbot.dias.domain.enums.ChannelType;

public record IncomingMessage(
    String companyInstanceName,
    String senderPhone,
    String senderName,
    String text,
    ChannelType channelType
) {}
