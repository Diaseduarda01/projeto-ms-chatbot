package ms.chatbot.dias.domain.exception;

import java.util.UUID;

public class SessionNotInHandoffException extends RuntimeException {
    public SessionNotInHandoffException(UUID sessionId) {
        super("Sessão " + sessionId + " não está em HANDOFF — só é possível enviar mensagens manualmente neste status");
    }
}
