package ms.chatbot.dias.domain.exception;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {
    public SessionNotFoundException(UUID id) {
        super("Sessão não encontrada: " + id);
    }
}
