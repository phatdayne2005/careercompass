package vn.uth.careercompass.blackbox;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("Phân hoạch lớp tương đương — trường mật khẩu")
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

    /** Form hợp lệ mọi trường, chỉ thay mật khẩu — kỹ thuật cô lập biến. */
    private RegisterFormDTO formWithPassword(String password) {
        RegisterFormDTO dto = new RegisterFormDTO();
        dto.setFullName("Nguyen Van A");
        dto.setEmail("a@example.com");
        dto.setPassword(password);
        return dto;
    }

    @Test
    @DisplayName("V1 · Lớp hợp lệ: 6–30 ký tự có nội dung → được chấp nhận")
    void v1_lopHopLe_duocChapNhan() {
        assertThat(validator.validateProperty(formWithPassword("matkhau123"), "password"))
                .as("Đại diện lớp hợp lệ V1")
                .isEmpty();
    }

    @Test
    @DisplayName("X1 · Lớp dưới 6 ký tự → bị từ chối")
    void x1_duoiSauKyTu_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword("abc"), "password"))
                .as("Đại diện lớp không hợp lệ X1")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X2 · Lớp trên 30 ký tự → bị từ chối")
    void x2_tren30KyTu_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword("p".repeat(31)), "password"))
                .as("Đại diện lớp không hợp lệ X2")
                .isNotEmpty();
    }

    @Test
    @DisplayName("X3 · Lớp toàn khoảng trắng → bị từ chối dù độ dài hợp lệ")
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
    void x4_null_biTuChoi() {
        assertThat(validator.validateProperty(formWithPassword(null), "password"))
                .as("Đại diện lớp không hợp lệ X4")
                .isNotEmpty();
    }
}
