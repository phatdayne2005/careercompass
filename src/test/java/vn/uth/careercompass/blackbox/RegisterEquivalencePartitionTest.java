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
import org.junit.jupiter.api.Test;
import vn.uth.careercompass.kernel.web.dto.request.RegisterFormDTO;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KỸ THUẬT: Phân hoạch lớp tương đương (Equivalence Partitioning) — chương IV mục 3.
 *
 * <p><b>Khác gì với BVA?</b> BVA thử các giá trị SÁT BIÊN. Phân lớp tương đương thử
 * MỘT ĐẠI DIỆN cho mỗi lớp — vì mọi giá trị trong cùng lớp cho cùng kết quả, thử thêm là thừa.
 * Hai kỹ thuật bổ sung cho nhau: EP đảm bảo không sót lớp nào, BVA đảm bảo không sai ở ranh giới.
 *
 * <p>Đối tượng: trường {@code password} của {@link RegisterFormDTO},
 * ràng buộc {@code @NotBlank @Size(min = 6, max = 30)}.
 *
 * <p>BẢNG PHÂN LỚP TƯƠNG ĐƯƠNG:
 * <pre>
 *   Tag  Lớp                                   Đại diện dùng để test   Kỳ vọng
 *   V1   6–30 ký tự, có nội dung               "matkhau123"            Chấp nhận
 *   X1   Dưới 6 ký tự (không rỗng)             "abc"                   Từ chối (@Size)
 *   X2   Trên 30 ký tự                          31 ký tự 'p'            Từ chối (@Size)
 *   X3   Toàn khoảng trắng                      "        " (8 dấu cách) Từ chối (@NotBlank)
 *   X4   Không truyền giá trị                   null                    Từ chối (@NotBlank)
 * </pre>
 *
 * <p><b>Lớp X3 là phát hiện của việc lập bảng.</b> Chuỗi 8 dấu cách có độ dài 8 nên
 * THOẢ MÃN {@code @Size(min = 6, max = 30)}, nhưng vẫn bị {@code @NotBlank} từ chối.
 * Đây là lớp mà bộ test giá trị biên không chạm tới, vì nó không nằm ở ranh giới độ dài nào cả.
 */
@DisplayName("Phân hoạch lớp tương đương — form đăng ký (password, fullName, email)")
// Giữ thứ tự V1 V2 V3 rồi X1..X10 như cột Tag của Bảng 4, để log chạy test
// đọc theo đúng trình tự báo cáo.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RegisterEquivalencePartitionTest {

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

    /** Form hợp lệ mọi trường — dùng làm nền để chỉ thay đúng một trường (cô lập biến). */
    private RegisterFormDTO validForm() {
        RegisterFormDTO dto = new RegisterFormDTO();
        dto.setFullName("Nguyen Van A");
        dto.setEmail("a@example.com");
        dto.setPassword("matkhau123");
        return dto;
    }

    private RegisterFormDTO formWithPassword(String password) {
        RegisterFormDTO dto = validForm();
        dto.setPassword(password);
        return dto;
    }

    private RegisterFormDTO formWithFullName(String fullName) {
        RegisterFormDTO dto = validForm();
        dto.setFullName(fullName);
        return dto;
    }

    private RegisterFormDTO formWithEmail(String email) {
        RegisterFormDTO dto = validForm();
        dto.setEmail(email);
        return dto;
    }

    @Test
    @DisplayName("V1 · Lớp hợp lệ: 6–30 ký tự có nội dung → được chấp nhận")
    @Order(1)
    void v1_lopHopLe_duocChapNhan() {
        assertThat(validator.validateProperty(formWithPassword("matkhau123"), "password"))
                .as("Đại diện lớp hợp lệ V1")
                .isEmpty();
    }

    @Test
    @DisplayName("X1 · Lớp dưới 6 ký tự → bị từ chối")
    @Order(4)
    void x1_duoiSauKyTu_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword("abc"), "password"))
                .as("Đại diện lớp không hợp lệ X1")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X2 · Lớp trên 30 ký tự → bị từ chối")
    @Order(5)
    void x2_tren30KyTu_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword("p".repeat(31)), "password"))
                .as("Đại diện lớp không hợp lệ X2")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X3 · Lớp toàn khoảng trắng → bị từ chối dù độ dài hợp lệ")
    @Order(6)
    void x3_toanKhoangTrang_biTuChoi() {
        // 8 dấu cách: độ dài 8 nằm trong [6, 30] nên @Size cho qua,
        // nhưng @NotBlank vẫn chặn vì không có ký tự nào khác khoảng trắng.
        String eightSpaces = " ".repeat(8);

        assertThat(eightSpaces.length())
                .as("Độ dài phải nằm trong khoảng hợp lệ để chứng minh @Size không phải thứ chặn")
                .isBetween(6, 30);

        assertThat(validator.validateProperty(formWithPassword(eightSpaces), "password"))
                .as("Đại diện lớp không hợp lệ X3 — lớp mà BVA không chạm tới")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X4 · Lớp không truyền giá trị (null) → bị từ chối")
    @Order(7)
    void x4_null_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword(null), "password"))
                .as("Đại diện lớp không hợp lệ X4")
                .isNotEmpty();
    }

    // ================================================================
    // TRƯỜNG fullName — @NotBlank @Size(max = 100)
    // ================================================================

    @Test
    @DisplayName("V2 · fullName lớp hợp lệ: 1–100 ký tự có nội dung")
    @Order(2)
    void v2_fullName_lopHopLe() {
        assertThat(validator.validateProperty(formWithFullName("Nguyen Van A"), "fullName")).isEmpty();
    }

    @Test
    @DisplayName("X5 · fullName trên 100 ký tự → bị từ chối (@Size)")
    @Order(8)
    void x5_fullName_tren100KyTu() {
        assertThat(validator.validateProperty(formWithFullName("x".repeat(101)), "fullName")).isNotEmpty();
    }

    @Test
    @DisplayName("X6 · fullName toàn khoảng trắng → bị từ chối (@NotBlank)")
    @Order(9)
    void x6_fullName_toanKhoangTrang() {
        assertThat(validator.validateProperty(formWithFullName("     "), "fullName")).isNotEmpty();
    }

    @Test
    @DisplayName("X7 · fullName null → bị từ chối (@NotBlank)")
    @Order(10)
    void x7_fullName_null() {
        assertThat(validator.validateProperty(formWithFullName(null), "fullName")).isNotEmpty();
    }

    // ================================================================
    // TRƯỜNG email — @NotBlank @Email @Size(max = 150)
    // ================================================================

    @Test
    @DisplayName("V3 · email lớp hợp lệ: đúng định dạng, không quá 150 ký tự")
    @Order(3)
    void v3_email_lopHopLe() {
        assertThat(validator.validateProperty(formWithEmail("sinhvien@uth.edu.vn"), "email")).isEmpty();
    }

    @Test
    @DisplayName("X8 · email sai định dạng → bị từ chối (@Email)")
    @Order(11)
    void x8_email_saiDinhDang() {
        assertThat(validator.validateProperty(formWithEmail("khong-phai-email"), "email")).isNotEmpty();
    }

    @Test
    @DisplayName("X9 · email trên 150 ký tự → bị từ chối (@Size)")
    @Order(12)
    void x9_email_tren150KyTu() {
        // Local-part 64 + '@' + hai nhãn miền + '.com' = 153 ký tự, vẫn đúng định dạng email.
        String longEmail = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(20) + ".com";
        assertThat(longEmail.length()).isGreaterThan(150);

        assertThat(validator.validateProperty(formWithEmail(longEmail), "email")).isNotEmpty();
    }

    @Test
    @DisplayName("X10 · email null → bị từ chối (@NotBlank)")
    @Order(13)
    void x10_email_null() {
        assertThat(validator.validateProperty(formWithEmail(null), "email")).isNotEmpty();
    }
}
