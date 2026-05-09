package ms.chatbot.dias.application.service;

import ms.chatbot.dias.domain.enums.InputType;
import org.springframework.stereotype.Component;

@Component
public class InputValidator {

    public boolean validate(InputType type, String value) {
        if (value == null || value.isBlank()) return false;
        return switch (type) {
            case TEXT -> value.trim().length() >= 2;
            case CPF -> value.replaceAll("[^0-9]", "").length() == 11;
            case PHONE -> {
                String digits = value.replaceAll("[^0-9]", "");
                yield digits.length() >= 10 && digits.length() <= 11;
            }
            case EMAIL -> value.contains("@") && value.contains(".");
            case NUMBER -> value.trim().matches("\\d+");
            case DATE -> value.trim().matches("\\d{2}/\\d{2}/\\d{4}");
        };
    }

    public String normalize(InputType type, String value) {
        return switch (type) {
            case CPF, PHONE, NUMBER -> value.replaceAll("[^0-9]", "");
            case EMAIL -> value.trim().toLowerCase();
            default -> value.trim();
        };
    }

    public String errorMessage(InputType type) {
        return switch (type) {
            case TEXT -> "Por favor, informe um texto válido.";
            case CPF -> "CPF inválido. Digite apenas os 11 números.";
            case PHONE -> "Telefone inválido. Digite DDD + número (ex: 11987654321).";
            case EMAIL -> "E-mail inválido. Digite um e-mail válido.";
            case NUMBER -> "Por favor, informe um número válido.";
            case DATE -> "Data inválida. Use o formato DD/MM/AAAA.";
        };
    }
}
