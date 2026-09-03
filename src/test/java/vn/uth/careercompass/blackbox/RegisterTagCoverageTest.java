package vn.uth.careercompass.blackbox;

import jakarta.validation.ConstraintViolation;
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

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KỸ THUẬT: thiết kế test case bằng cách GỘP TAG, theo mẫu slide 33 (ví dụ Loan application).
 *
 * <p>Slide 32 liệt kê mọi lớp tương đương và giá trị biên rồi gắn tag {@code V}, {@code X},
 * {@code B}. Slide 33 gộp các tag đó lại thành số test case tối thiểu: mỗi case là MỘT lần
 * điền form đầy đủ, cột <i>New Tags Covered</i> ghi những tag case đó phủ lần đầu.
 *
 * <p>Lớp này thi hành TC1–TC16 của <b>Bảng 5</b> trong báo cáo — phần thuộc form đăng ký.
 * TC17–TC22 (dung lượng tệp bảng điểm) nằm ở {@code OnboardingFileSizeBvaTest} vì đó là
 * chức năng onboarding, không cùng một màn hình nhập liệu.
 *
 * <p><b>Nguyên tắc gộp</b>:
 * <ul>
 *   <li>Case HỢP LỆ được gộp nhiều tag cùng lúc — cả ba trường đều hợp lệ nên không trường
 *       nào che kết quả của trường nào. TC1 phủ một lúc 7 tag.</li>
 *   <li>Case KHÔNG hợp lệ chỉ được đặt MỘT vi phạm mỗi lần. Nếu vừa để password quá ngắn vừa
 *       để email sai định dạng thì form vẫn báo lỗi, nhưng không biết nó bắt được vi phạm
 *       nào — hiện tượng che lỗi (masking). Vì vậy 11 tag {@code X} phải trải ra 11 case.</li>
 * </ul>
 *
 * <p>Để chứng minh không có che lỗi, mỗi case không hợp lệ ở đây khẳng định
 * <b>đúng một</b> vi phạm, và vi phạm đó nằm trên <b>đúng trường</b> đang bị làm sai.
 *
 * <p>Khác {@code RegisterStandardBvaTest}: lớp kia giữ hai trường ở nom để cô lập một biên
 * theo công thức {@code 4n+1}; lớp này đổi cả ba trường cùng lúc nhằm phủ nhiều tag nhất
 * trong một lần submit.
 */
@DisplayName("Gop tag thanh test case - Bang 5, mau slide 33")
// Giữ đúng thứ tự TC1-TC5 rồi TC6-TC16 như Bảng 5 của báo cáo.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegisterTagCoverageTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    // Giá trị nominal, dùng cho các trường KHÔNG phải trọng tâm của case không hợp lệ.
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
    // TC1–TC5 · case HỢP LỆ · mỗi case gộp nhiều tag
    // ================================================================

    /** Cột: số hiệu case · độ dài fullName · độ dài email · độ dài password · tag phủ lần đầu. */
    static Stream<Arguments> caseHopLe() {
        return Stream.of(
                Arguments.of(1, 1, 75, 6, "V1, V2, V3, V4, B1, B8, B15"),
                Arguments.of(2, 2, 149, 7, "B2, B9, B16"),
                Arguments.of(3, 50, 150, 18, "B3, B10, B17"),
                Arguments.of(4, 99, 75, 29, "B4, B11"),
                Arguments.of(5, 100, 75, 30, "B5, B12")
        );
    }

    @ParameterizedTest(name = "TC{0} | fullName={1}, email={2}, password={3} | phu {4}")
    @MethodSource("caseHopLe")
    @Order(1)
    @DisplayName("TC1-TC5 | dang ky thanh cong | gop nhieu tag trong mot lan submit")
    void tagCoverage_caseHopLe_dangKyThanhCong(int tc, int fullNameLen, int emailLen,
                                               int passwordLen, String tagsPhuLanDau) {
        RegisterFormDTO dto = buildForm(
                "x".repeat(fullNameLen), emailOfLength(emailLen), "p".repeat(passwordLen));

        assertThat(validator.validate(dto))
                .as("TC%d phu %s", tc, tagsPhuLanDau)
                .isEmpty();
    }

    // ================================================================
    // TC6–TC16 · case KHÔNG hợp lệ · mỗi case đúng MỘT vi phạm
    // ================================================================

    /** Cột: số hiệu case · fullName · email · password · trường bị làm sai · tag phủ lần đầu. */
    static Stream<Arguments> caseKhongHopLe() {
        String emailNom = emailOfLength(EMAIL_NOM);
        String fullNameNom = "x".repeat(FULLNAME_NOM);
        String passwordNom = "p".repeat(PASSWORD_NOM);

        return Stream.of(
                // --- password ---
                Arguments.of(6, fullNameNom, emailNom, "p".repeat(5), "password", "X1, B6"),
                Arguments.of(7, fullNameNom, emailNom, "p".repeat(31), "password", "X2, B7"),
                Arguments.of(8, fullNameNom, emailNom, " ".repeat(8), "password", "X3"),
                Arguments.of(9, fullNameNom, emailNom, null, "password", "X4"),
                // --- fullName ---
                Arguments.of(10, "x".repeat(101), emailNom, passwordNom, "fullName", "X5, B14"),
                Arguments.of(11, " ".repeat(5), emailNom, passwordNom, "fullName", "X6"),
                Arguments.of(12, null, emailNom, passwordNom, "fullName", "X7"),
                Arguments.of(13, "", emailNom, passwordNom, "fullName", "B13"),
                // --- email ---
                Arguments.of(14, fullNameNom, "khong-phai-email", passwordNom, "email", "X8"),
                Arguments.of(15, fullNameNom, emailOfLength(151), passwordNom, "email", "X9, B18"),
                Arguments.of(16, fullNameNom, null, passwordNom, "email", "X10")
        );
    }

    @ParameterizedTest(name = "TC{0} | sai o {4} | phu {5}")
    @MethodSource("caseKhongHopLe")
    @Order(2)
    @DisplayName("TC6-TC16 | form bi tu choi | dung mot vi pham, khong che loi")
    void tagCoverage_caseKhongHopLe_biTuChoi(int tc, String fullName, String email,
                                             String password, String truongBiLamSai,
                                             String tagsPhuLanDau) {
        RegisterFormDTO dto = buildForm(fullName, email, password);

        Set<ConstraintViolation<RegisterFormDTO>> viPham = validator.validate(dto);

        assertThat(viPham)
                .as("TC%d phu %s - form phai bi tu choi", tc, tagsPhuLanDau)
                .isNotEmpty();
        // Đúng MỘT vi phạm, trên ĐÚNG trường đang bị làm sai: chứng minh hai trường còn lại
        // vẫn hợp lệ nên không che mất lỗi cần bắt.
        assertThat(viPham)
                .as("TC%d - chi duoc sai dung mot truong, tranh che loi", tc)
                .hasSize(1);
        assertThat(viPham.iterator().next().getPropertyPath())
                .hasToString(truongBiLamSai);
    }

    // ================================================================
    // Sinh dữ liệu
    // ================================================================

    private RegisterFormDTO buildForm(String fullName, String email, String password) {
        RegisterFormDTO dto = new RegisterFormDTO();
        dto.setFullName(fullName);
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }

    /**
     * Tạo email đúng định dạng với độ dài cho trước.
     * Giữ phần local ≤ 64 ký tự và mỗi nhãn miền ≤ 63 ký tự để Hibernate Validator
     * vẫn coi là email hợp lệ kể cả khi chuỗi rất dài.
     */
    private static String emailOfLength(int length) {
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
