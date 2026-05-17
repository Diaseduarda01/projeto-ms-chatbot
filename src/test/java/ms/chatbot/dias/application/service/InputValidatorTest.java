package ms.chatbot.dias.application.service;

import ms.chatbot.dias.domain.enums.InputType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class InputValidatorTest {

    private final InputValidator validator = new InputValidator();

    @ParameterizedTest
    @ValueSource(strings = {"ab", "João", "nome completo"})
    void validate_text_valid(String value) {
        assertThat(validator.validate(InputType.TEXT, value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "", " "})
    void validate_text_invalid(String value) {
        assertThat(validator.validate(InputType.TEXT, value)).isFalse();
    }

    @Test
    void validate_text_null_returns_false() {
        assertThat(validator.validate(InputType.TEXT, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678901", "123.456.789-01"})
    void validate_cpf_valid(String value) {
        assertThat(validator.validate(InputType.CPF, value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567890", "123456789012", "abc"})
    void validate_cpf_invalid(String value) {
        assertThat(validator.validate(InputType.CPF, value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1198765432", "11987654321", "(11) 98765-4321"})
    void validate_phone_valid(String value) {
        assertThat(validator.validate(InputType.PHONE, value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456789", "123456789012"})
    void validate_phone_invalid(String value) {
        assertThat(validator.validate(InputType.PHONE, value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"user@example.com", "a@b.c"})
    void validate_email_valid(String value) {
        assertThat(validator.validate(InputType.EMAIL, value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"userexample.com", "user@example"})
    void validate_email_invalid(String value) {
        assertThat(validator.validate(InputType.EMAIL, value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "42", "1000"})
    void validate_number_valid(String value) {
        assertThat(validator.validate(InputType.NUMBER, value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "12a", "1.5"})
    void validate_number_invalid(String value) {
        assertThat(validator.validate(InputType.NUMBER, value)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"01/01/2024", "31/12/2023"})
    void validate_date_valid(String value) {
        assertThat(validator.validate(InputType.DATE, value)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2024-01-01", "01/01/24", "abcde"})
    void validate_date_invalid(String value) {
        assertThat(validator.validate(InputType.DATE, value)).isFalse();
    }

    @Test
    void normalize_cpf_strips_non_digits() {
        assertThat(validator.normalize(InputType.CPF, "123.456.789-01")).isEqualTo("12345678901");
    }

    @Test
    void normalize_phone_strips_non_digits() {
        assertThat(validator.normalize(InputType.PHONE, "(11) 98765-4321")).isEqualTo("11987654321");
    }

    @Test
    void normalize_number_strips_non_digits() {
        assertThat(validator.normalize(InputType.NUMBER, "007")).isEqualTo("007");
    }

    @Test
    void normalize_email_lowercases_and_trims() {
        assertThat(validator.normalize(InputType.EMAIL, "  USER@EXAMPLE.COM  ")).isEqualTo("user@example.com");
    }

    @Test
    void normalize_text_trims() {
        assertThat(validator.normalize(InputType.TEXT, "  João  ")).isEqualTo("João");
    }

    @Test
    void normalize_date_trims() {
        assertThat(validator.normalize(InputType.DATE, " 01/01/2024 ")).isEqualTo("01/01/2024");
    }

    @Test
    void errorMessage_returns_non_blank_for_all_types() {
        for (InputType type : InputType.values()) {
            assertThat(validator.errorMessage(type)).isNotBlank();
        }
    }
}
