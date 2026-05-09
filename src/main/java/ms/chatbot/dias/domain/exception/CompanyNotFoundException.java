package ms.chatbot.dias.domain.exception;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException(String instanceName) {
        super("Empresa não encontrada para a instância: " + instanceName);
    }
}
