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
@DisplayName("Phan hoach lop tuong duong - form dang ky (password, fullName, email)")
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
    @DisplayName("V1 | Lop hop le: 6-30 ky tu co noi dung -> duoc chap nhan")
    @Order(1)
    void v1_lopHopLe_duocChapNhan() {
        assertThat(validator.validateProperty(formWithPassword("matkhau123"), "password"))
                .as("Dai dien lop hop le V1")
                .isEmpty();
    }

    @Test
    @DisplayName("X1 | Lop duoi 6 ky tu -> bi tu choi")
    @Order(4)
    void x1_duoiSauKyTu_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword("abc"), "password"))
                .as("Dai dien lop khong hop le X1")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X2 | Lop tren 30 ky tu -> bi tu choi")
    @Order(5)
    void x2_tren30KyTu_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword("p".repeat(31)), "password"))
                .as("Dai dien lop khong hop le X2")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X3 | Lop toan khoang trang -> bi tu choi du do dai hop le")
    @Order(6)
    void x3_toanKhoangTrang_biTuChoi() {
        // 8 dấu cách: độ dài 8 nằm trong [6, 30] nên @Size cho qua,
        // nhưng @NotBlank vẫn chặn vì không có ký tự nào khác khoảng trắng.
        String eightSpaces = " ".repeat(8);

        assertThat(eightSpaces.length())
                .as("Do dai phai nam trong khoang hop le de chung minh @Size khong phai thu chan")
                .isBetween(6, 30);

        assertThat(validator.validateProperty(formWithPassword(eightSpaces), "password"))
                .as("Dai dien lop khong hop le X3 - lop ma BVA khong cham toi")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X4 | Lop khong truyen gia tri (null) -> bi tu choi")
    @Order(7)
    void x4_null_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword(null), "password"))
                .as("Dai dien lop khong hop le X4")
                .isNotEmpty();
    }

    // ================================================================
    // TRƯỜNG fullName — @NotBlank @Size(max = 100)
    // ================================================================

    @Test
    @DisplayName("V2 | fullName lop hop le: 1-100 ky tu co noi dung")
    @Order(2)
    void v2_fullName_lopHopLe() {
        assertThat(validator.validateProperty(formWithFullName("Nguyen Van A"), "fullName")).isEmpty();
    }

    @Test
    @DisplayName("X5 | fullName tren 100 ky tu -> bi tu choi (@Size)")
    @Order(8)
    void x5_fullName_tren100KyTu() {
        assertThat(validator.validateProperty(formWithFullName("x".repeat(101)), "fullName")).isNotEmpty();
    }

    @Test
    @DisplayName("X6 | fullName toan khoang trang -> bi tu choi (@NotBlank)")
    @Order(9)
    void x6_fullName_toanKhoangTrang() {
        assertThat(validator.validateProperty(formWithFullName("     "), "fullName")).isNotEmpty();
    }

    @Test
    @DisplayName("X7 | fullName null -> bi tu choi (@NotBlank)")
    @Order(10)
    void x7_fullName_null() {
        assertThat(validator.validateProperty(formWithFullName(null), "fullName")).isNotEmpty();
    }

    // ================================================================
    // TRƯỜNG email — @NotBlank @Email @Size(min = 6, max = 150)
    // ================================================================

    @Test
    @DisplayName("V3 | email lop hop le: dung dinh dang, 6-150 ky tu")
    @Order(3)
    void v3_email_lopHopLe() {
        assertThat(validator.validateProperty(formWithEmail("sinhvien@uth.edu.vn"), "email")).isEmpty();
    }

    @Test
    @DisplayName("X8 | email duoi 6 ky tu -> bi tu choi (@Size)")
    @Order(11)
    void x8_email_duoiSauKyTu() {
        // "a@b.c" dài 5 ký tự, ĐÚNG định dạng email nên @Email cho qua.
        // Chỉ @Size(min = 6) chặn được — đây là lớp mà kiểm định dạng bỏ lọt.
        assertThat(validator.validateProperty(formWithEmail("a@b.c"), "email")).isNotEmpty();
    }

    @Test
    @DisplayName("X10 | email sai dinh dang -> bi tu choi (@Email)")
    @Order(13)
    void x10_email_saiDinhDang() {
        assertThat(validator.validateProperty(formWithEmail("khong-phai-email"), "email")).isNotEmpty();
    }

    @Test
    @DisplayName("X9 | email tren 150 ky tu -> bi tu choi (@Size)")
    @Order(12)
    void x9_email_tren150KyTu() {
        // Local-part 64 + '@' + hai nhãn miền + '.com' = 153 ký tự, vẫn đúng định dạng email.
        String longEmail = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(20) + ".com";
        assertThat(longEmail.length()).isGreaterThan(150);

        assertThat(validator.validateProperty(formWithEmail(longEmail), "email")).isNotEmpty();
    }

    @Test
    @DisplayName("X11 | email null -> bi tu choi (@NotBlank)")
    @Order(14)
    void x11_email_null() {
        assertThat(validator.validateProperty(formWithEmail(null), "email")).isNotEmpty();
    }
}
