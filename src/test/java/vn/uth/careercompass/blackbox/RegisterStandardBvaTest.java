package vn.uth.careercompass.blackbox;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import vn.uth.careercompass.kernel.web.dto.request.RegisterFormDTO;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KỸ THUẬT: Phân tích giá trị biên trình bày theo ĐÚNG mẫu slide 23 và 24.
 *
 * <p>Khác với {@code RegisterFormDTOBvaTest} (kiểm từng trường riêng bằng
 * {@code validateProperty}), lớp này làm theo mô hình của thầy: <b>mỗi test case là một
 * BỘ ĐẦY ĐỦ n biến</b>, trong đó đúng MỘT biến đặt ở giá trị biên, các biến còn lại giữ
 * ở giá trị nominal. Kiểm bằng {@code validate(dto)} trên cả form.
 *
 * <p>Slide 26 nêu rõ nguyên tắc: <i>"Tại một thời điểm, BVA chỉ test giá trị biên của 1 biến"</i>.
 *
 * <p><b>Định nghĩa giá trị biên từng biến</b> (tương ứng phần ghi chú bên trái slide 23):
 * <pre>
 *   fullName (số ký tự)     email (số ký tự)      password (số ký tự)
 *   min  = 1                min  = 6              min  = 6
 *   min+ = 2                min+ = 7              min+ = 7
 *   nom  = 50               nom  = 75             nom  = 18
 *   max- = 99               max- = 149            max- = 29
 *   max  = 100              max  = 150            max  = 30
 * </pre>
 *
 * <p>Ràng buộc gốc trong {@link RegisterFormDTO}:
 * {@code fullName @NotBlank @Size(max=100)} · {@code email @NotBlank @Email @Size(max=150)}
 * · {@code password @NotBlank @Size(min=6, max=30)}.
 *
 * <p>LƯU Ý về biên dưới của email: {@code @Size} chỉ khai {@code max}, không khai {@code min}.
 * Giá trị min = 6 là <b>biên vận hành</b> — độ dài của email quy ước ngắn nhất "a@b.co".
 * Xem test {@code robustnessBva} case 16 để thấy hệ quả.
 *
 * <p>Số test case: Standard BVA = 4n + 1 = 4×3 + 1 = <b>13</b>.
 * Robustness BVA = 6n + 1 = 6×3 + 1 = <b>19</b>.
 */
@DisplayName("Giá trị biên — trình bày theo mẫu slide 23 (bộ đầy đủ n biến)")
// Giữ đúng thứ tự Standard rồi mới Robustness, để log chạy test đọc theo
// đúng trình tự Bảng 2 -> Bảng 3 của báo cáo.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegisterStandardBvaTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    // Giá trị nominal của từng biến — dùng khi biến đó KHÔNG phải biến đang xét.
    private static final int FULLNAME_NOM = 50;
    private static final int EMAIL_NOM = 75;
    private static final int PASSWORD_NOM = 18;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    // ================================================================
    // STANDARD BVA — 4n + 1 = 13 case, TẤT CẢ đều hợp lệ (clean test cases)
    // ================================================================

    /**
     * Cột: số hiệu case · độ dài fullName · độ dài email · độ dài password · biến đang xét.
     * Đọc theo hàng giống bảng "Boundary Value Analysis Test Cases" ở slide 23.
     */
    static Stream<Arguments> standardBvaCases() {
        return Stream.of(
                // --- Biến đang xét: password (5 giá trị, gồm cả ô tất-cả-nominal) ---
                Arguments.of(1, FULLNAME_NOM, EMAIL_NOM, 6, "password = min"),
                Arguments.of(2, FULLNAME_NOM, EMAIL_NOM, 7, "password = min+"),
                Arguments.of(3, FULLNAME_NOM, EMAIL_NOM, 18, "TẤT CẢ nominal"),
                Arguments.of(4, FULLNAME_NOM, EMAIL_NOM, 29, "password = max-"),
                Arguments.of(5, FULLNAME_NOM, EMAIL_NOM, 30, "password = max"),
                // --- Biến đang xét: email (4 giá trị, bỏ nominal vì case 3 đã có) ---
                Arguments.of(6, FULLNAME_NOM, 6, PASSWORD_NOM, "email = min"),
                Arguments.of(7, FULLNAME_NOM, 7, PASSWORD_NOM, "email = min+"),
                Arguments.of(8, FULLNAME_NOM, 149, PASSWORD_NOM, "email = max-"),
                Arguments.of(9, FULLNAME_NOM, 150, PASSWORD_NOM, "email = max"),
                // --- Biến đang xét: fullName (4 giá trị) ---
                Arguments.of(10, 1, EMAIL_NOM, PASSWORD_NOM, "fullName = min"),
                Arguments.of(11, 2, EMAIL_NOM, PASSWORD_NOM, "fullName = min+"),
                Arguments.of(12, 99, EMAIL_NOM, PASSWORD_NOM, "fullName = max-"),
                Arguments.of(13, 100, EMAIL_NOM, PASSWORD_NOM, "fullName = max")
        );
    }

    @ParameterizedTest(name = "TC{0} · {4} · (fullName={1}, email={2}, password={3})")
    @MethodSource("standardBvaCases")
    @Order(1)
    @DisplayName("Standard BVA · 4n+1 = 13 case · mọi bộ đều phải HỢP LỆ")
    void standardBva_moiBoDeuHopLe(int tc, int fullNameLen, int emailLen,
                                   int passwordLen, String bienDangXet) {
        RegisterFormDTO dto = buildForm(fullNameLen, emailLen, passwordLen);

        assertThat(validator.validate(dto))
                .as("TC%d — %s", tc, bienDangXet)
                .isEmpty();
    }

    // ================================================================
    // ROBUSTNESS BVA — thêm min- và max+ cho mỗi biến (dirty test cases)
    // ================================================================

    static Stream<Arguments> robustnessBvaCases() {
        return Stream.of(
                // Biến đang xét: password
                Arguments.of(14, FULLNAME_NOM, EMAIL_NOM, 5, "password = min-", false),
                Arguments.of(15, FULLNAME_NOM, EMAIL_NOM, 31, "password = max+", false),
                // Biến đang xét: email
                // Case 16 KỲ VỌNG HỢP LỆ: @Size của email không khai min, nên email 5 ký tự
                // (a@b.c) vẫn được chấp nhận. Đây là PHÁT HIỆN, không phải lỗi test.
                Arguments.of(16, FULLNAME_NOM, 5, PASSWORD_NOM, "email = min- (biên dưới KHÔNG ràng buộc)", true),
                Arguments.of(17, FULLNAME_NOM, 151, PASSWORD_NOM, "email = max+", false),
                // Biến đang xét: fullName
                Arguments.of(18, 0, EMAIL_NOM, PASSWORD_NOM, "fullName = min- (chuỗi rỗng)", false),
                Arguments.of(19, 101, EMAIL_NOM, PASSWORD_NOM, "fullName = max+", false)
        );
    }

    @ParameterizedTest(name = "TC{0} · {4} · kỳ vọng hợp lệ = {5}")
    @MethodSource("robustnessBvaCases")
    @Order(2)
    @DisplayName("Robustness BVA · 6n+1 = 19 case · thêm min- và max+")
    void robustnessBva_giaTriNgoaiBien(int tc, int fullNameLen, int emailLen,
                                       int passwordLen, String bienDangXet, boolean hopLe) {
        RegisterFormDTO dto = buildForm(fullNameLen, emailLen, passwordLen);

        assertThat(validator.validate(dto).isEmpty())
                .as("TC%d — %s", tc, bienDangXet)
                .isEqualTo(hopLe);
    }

    // ================================================================
    // Sinh dữ liệu theo độ dài yêu cầu
    // ================================================================

    private RegisterFormDTO buildForm(int fullNameLen, int emailLen, int passwordLen) {
        RegisterFormDTO dto = new RegisterFormDTO();
        dto.setFullName("x".repeat(fullNameLen));
        dto.setEmail(emailOfLength(emailLen));
        dto.setPassword("p".repeat(passwordLen));
        return dto;
    }

    /**
     * Tạo email đúng định dạng với độ dài cho trước.
     * Giữ phần local ≤ 64 ký tự và mỗi nhãn miền ≤ 63 ký tự để Hibernate Validator
     * vẫn coi là email hợp lệ kể cả khi chuỗi rất dài.
     */
    private static String emailOfLength(int length) {
        if (length == 5) {
            return "a@b.c";                       // email 5 ký tự, dùng cho case min-
        }
        int localLen = Math.min(64, length - 5);  // trừ '@' và ".co" và tối thiểu 1 ký tự miền
        int domainLen = length - localLen - 1;    // phần sau dấu '@'
        int prefixLen = domainLen - 3;            // trừ ".co"
        String prefix;
        if (prefixLen > 63) {
            String raw = "b".repeat(prefixLen - 1);
            prefix = raw.substring(0, 63) + "." + raw.substring(63);
        } else {
            prefix = "b".repeat(prefixLen);
        }
        return "a".repeat(localLen) + "@" + prefix + ".co";
    }
}
