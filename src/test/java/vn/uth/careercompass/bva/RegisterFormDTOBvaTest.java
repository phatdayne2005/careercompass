package vn.uth.careercompass.bva;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.uth.careercompass.kernel.web.dto.request.RegisterFormDTO;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BVA 5 điểm cho các ràng buộc đăng ký.
 *
 * <p>Mỗi trường có đúng bộ normal BVA: min, min+1, medium, max-1, max.
 * Các giá trị rỗng/null và vượt biên được tách thành robust/negative cases.
 */
class RegisterFormDTOBvaTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    static Stream<Arguments> fullNameNormalBva() {
        return Stream.of(
                Arguments.of("min", "x"),
                Arguments.of("min+1", repeat('x', 2)),
                Arguments.of("medium", repeat('x', 50)),
                Arguments.of("max-1", repeat('x', 99)),
                Arguments.of("max", repeat('x', 100))
        );
    }

    @ParameterizedTest(name = "fullName {0}")
    @MethodSource("fullNameNormalBva")
    void fullName_normalBva_isValid(String boundary, String value) {
        RegisterFormDTO dto = validDto();
        dto.setFullName(value);

        assertThat(validator.validateProperty(dto, "fullName"))
                .as("fullName boundary %s, length=%s", boundary, value.length())
                .isEmpty();
    }

    static Stream<Arguments> fullNameRobustAndNegative() {
        return Stream.of(
                Arguments.of("min-1", ""),
                Arguments.of("blank", "   "),
                Arguments.of("null", null),
                Arguments.of("max+1", repeat('x', 101))
        );
    }

    @ParameterizedTest(name = "fullName {0}")
    @MethodSource("fullNameRobustAndNegative")
    void fullName_robustAndNegative_isInvalid(String boundary, String value) {
        RegisterFormDTO dto = validDto();
        dto.setFullName(value);

        assertThat(validator.validateProperty(dto, "fullName"))
                .as("fullName negative boundary %s", boundary)
                .isNotEmpty();
    }

    static Stream<Arguments> emailNormalBva() {
        return Stream.of(
                // a@b.co là email hợp lệ ngắn nhất dùng làm operational minimum của test.
                Arguments.of("min", validEmail(6)),
                Arguments.of("min+1", validEmail(7)),
                Arguments.of("medium", validEmail(75)),
                Arguments.of("max-1", validEmail(149)),
                Arguments.of("max", validEmail(150))
        );
    }

    @ParameterizedTest(name = "email {0}")
    @MethodSource("emailNormalBva")
    void email_normalBva_isValid(String boundary, String value) {
        RegisterFormDTO dto = validDto();
        dto.setEmail(value);

        assertThat(validator.validateProperty(dto, "email"))
                .as("email boundary %s, length=%s", boundary, value.length())
                .isEmpty();
    }

    static Stream<Arguments> emailRobustAndNegative() {
        return Stream.of(
                Arguments.of("null", null),
                Arguments.of("empty", ""),
                Arguments.of("invalid format", "invalid-email"),
                Arguments.of("max+1", validEmail(151))
        );
    }

    @ParameterizedTest(name = "email {0}")
    @MethodSource("emailRobustAndNegative")
    void email_robustAndNegative_isInvalid(String boundary, String value) {
        RegisterFormDTO dto = validDto();
        dto.setEmail(value);

        assertThat(validator.validateProperty(dto, "email"))
                .as("email negative boundary %s", boundary)
                .isNotEmpty();
    }

    static Stream<Arguments> passwordNormalBva() {
        return Stream.of(
                Arguments.of("min", repeat('p', 6)),
                Arguments.of("min+1", repeat('p', 7)),
                Arguments.of("medium", repeat('p', 18)),
                Arguments.of("max-1", repeat('p', 29)),
                Arguments.of("max", repeat('p', 30))
        );
    }

    @ParameterizedTest(name = "password {0}")
    @MethodSource("passwordNormalBva")
    void password_normalBva_isValid(String boundary, String value) {
        RegisterFormDTO dto = validDto();
        dto.setPassword(value);

        assertThat(validator.validateProperty(dto, "password"))
                .as("password boundary %s, length=%s", boundary, value.length())
                .isEmpty();
    }

    static Stream<Arguments> passwordRobustAndNegative() {
        return Stream.of(
                Arguments.of("min-1", repeat('p', 5)),
                Arguments.of("max+1", repeat('p', 31)),
                Arguments.of("null", null),
                Arguments.of("empty", "")
        );
    }

    @ParameterizedTest(name = "password {0}")
    @MethodSource("passwordRobustAndNegative")
    void password_robustAndNegative_isInvalid(String boundary, String value) {
        RegisterFormDTO dto = validDto();
        dto.setPassword(value);

        assertThat(validator.validateProperty(dto, "password"))
                .as("password negative boundary %s", boundary)
                .isNotEmpty();
    }

    private static RegisterFormDTO validDto() {
        RegisterFormDTO dto = new RegisterFormDTO();
        dto.setFullName("Valid Name");
        dto.setEmail("valid@example.com");
        dto.setPassword("validpw");
        return dto;
    }

    private static String validEmail(int length) {
        if (length < 6) {
            throw new IllegalArgumentException("Email test length must be at least 6");
        }
        // Giữ local-part <= 64 ký tự và chia domain thành các label <= 63 ký tự
        // để Hibernate Validator vẫn coi email rất dài là email hợp lệ.
        int localLength = Math.min(64, length - 5);
        int domainLength = length - localLength - 1;
        int prefixLength = domainLength - 3; // trừ ".co"
        String prefix;
        if (prefixLength > 63) {
            // Dấu chấm phân tách thêm một ký tự, nên giảm độ dài raw prefix đi 1.
            String rawPrefix = repeat('b', prefixLength - 1);
            prefix = rawPrefix.substring(0, 63) + "." + rawPrefix.substring(63);
        } else {
            prefix = repeat('b', prefixLength);
        }
        return repeat('a', localLength) + "@" + prefix + ".co";
    }

    private static String repeat(char value, int count) {
        return String.valueOf(value).repeat(count);
    }
}
