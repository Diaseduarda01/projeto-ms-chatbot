package ms.chatbot.dias.infrastructure.web.dto;

public record WhatsAppConnectResponse(boolean connected, String qrCode, String pairingCode) {}
