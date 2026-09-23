package vn.uth.careercompass.bva;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.uth.careercompass.kernel.service.PasswordPolicy;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Giá trị biên của độ dài mật khẩu (DEF-013).
 *
 * <p>Đây là trường hợp BVA khác hẳn hai bảng BVA sẵn có ở sheet 01: cùng MỘT biến nhưng
 * có HAI biên đo bằng HAI ĐƠN VỊ khác nhau.
 *
 * <ul>
 *   <li>Biên theo KÝ TỰ: 6 ≤ độ dài ≤ 30 — quy tắc nghiệp vụ của nhóm.</li>
 *   <li>Biên theo BYTE: ≤ 72 byte UTF-8 — giới hạn CỨNG của BCrypt, không phải nhóm chọn.</li>
 * </ul>
 *
 * <p>Với mật khẩu ASCII, 30 ký tự luôn là 30 byte nên biên byte không bao giờ chạm tới và
 * hoàn toàn vô hình. Khoảng "lọt lưới" chỉ tồn tại với chữ chiếm ĐÚNG 3 byte, tức chữ
 * tiếng Việt có dấu: 24 ký tự là 72 byte (vừa đủ), 25 ký tự là 75 byte — vẫn nằm trong
 * giới hạn 30 ký tự nhưng đã vượt giới hạn của BCrypt.
 *
 * <p>Biểu tượng cảm xúc thì KHÔNG chạm được biên byte, dù mỗi cái chiếm 4 byte: nó là cặp
 * thay thế UTF-16 nên {@code String.length()} đếm là 2, và 15 biểu tượng đã đủ 30 "ký tự"
 * trong khi mới có 60 byte. Biên ký tự luôn chặn trước.
 *
 * <p>Đây chính là lý do BVA phải hỏi "biên đo bằng đơn vị nào" chứ không chỉ hỏi "biên ở
 * đâu". Trước khi sửa, ba đường đặt mật khẩu hành xử ba kiểu khác nhau ở cùng biên này:
 * đăng ký lộ nguyên văn thông điệp tiếng Anh của thư viện, đặt lại mật khẩu báo sai rằng
 * "link đã hết hạn", còn đổi mật khẩu cũng lộ thông điệp tiếng Anh.
 */
@DisplayName("BVA — Độ dài mật khẩu (DEF-013)")
class PasswordPolicyBvaTest {

    /** Chữ cái tiếng Việt có dấu: 3 byte UTF-8 mỗi ký tự. */
    private static final String CHU_CO_DAU = "ậ";

    /** Biểu tượng cảm xúc: 4 byte UTF-8 mỗi ký tự. */
    private static final String BIEU_TUONG = "😀";

    // ================================================================
    // BẢNG 1 — Standard BVA theo KÝ TỰ (4n + 1 với n = 1 biến → 5 case)
    // ================================================================

    @ParameterizedTest(name = "{0} ký tự ({1}) → {2}")
    @CsvSource({
            "6,  min,   true",
            "7,  'min+', true",
            "18, nom,   true",
            "29, 'max-', true",
            "30, max,   true",
    })
    @DisplayName("Năm điểm chuẩn trong miền hợp lệ đều được chấp nhận")
    void standardBva_trongMien(int soKyTu, String nhan, boolean mongDoi) {
        String matKhau = "p".repeat(soKyTu);

        assertThat(PasswordPolicy.hopLe(matKhau))
                .as("%d ký tự (%s)", soKyTu, nhan)
                .isEqualTo(mongDoi);
    }

    // ================================================================
    // BẢNG 2 — Robustness BVA theo KÝ TỰ (thêm min− và max+)
    // ================================================================

    @Test
    @DisplayName("min− : 5 ký tự bị từ chối kèm thông điệp về số ký tự tối thiểu")
    void robustness_duoiBienDuoi() {
        assertThat(PasswordPolicy.kiemTra("p".repeat(5)))
                .isEqualTo("Mật khẩu phải từ 6 ký tự trở lên.");
    }

    @Test
    @DisplayName("max+ : 31 ký tự bị từ chối — biên trên này TRƯỚC ĐÂY chỉ có ở đăng ký")
    void robustness_trenBienTren() {
        assertThat(PasswordPolicy.kiemTra("p".repeat(31)))
                .isEqualTo("Mật khẩu không được quá 30 ký tự.");
    }

    @Test
    @DisplayName("Chuỗi rỗng và null đều bị từ chối, không ném ngoại lệ")
    void robustness_rongVaNull() {
        assertThat(PasswordPolicy.kiemTra("")).isNotNull();
        assertThat(PasswordPolicy.kiemTra(null)).isNotNull();
        assertThatCode(() -> PasswordPolicy.kiemTra(null)).doesNotThrowAnyException();
    }

    // ================================================================
    // BẢNG 3 — Biên thứ hai, đo bằng BYTE: đây là phần DEF-013 bỏ sót
    // ================================================================

    @Test
    @DisplayName("max byte : 24 ký tự tiếng Việt = đúng 72 byte → vẫn hợp lệ")
    void bienByte_dungNguong() {
        String matKhau = CHU_CO_DAU.repeat(24);

        assertThat(matKhau.getBytes(StandardCharsets.UTF_8)).hasSize(72);
        assertThat(matKhau.length()).as("vẫn nằm trong 30 ký tự").isEqualTo(24);
        assertThat(PasswordPolicy.hopLe(matKhau)).isTrue();
    }

    @Test
    @DisplayName("max byte+ : 25 ký tự tiếng Việt = 75 byte → bị từ chối dù chỉ 25 ký tự")
    void bienByte_vuotNguong() {
        String matKhau = CHU_CO_DAU.repeat(25);

        assertThat(matKhau.getBytes(StandardCharsets.UTF_8)).hasSize(75);
        assertThat(matKhau.length())
                .as("25 ký tự — vẫn thoả @Size(max = 30), đây là chỗ biên ký tự bỏ lọt")
                .isLessThanOrEqualTo(PasswordPolicy.SO_KY_TU_TOI_DA);
        assertThat(PasswordPolicy.kiemTra(matKhau))
                .isEqualTo("Mật khẩu chứa quá nhiều ký tự có dấu hoặc biểu tượng "
                        + "(vượt 72 byte). Vui lòng rút ngắn lại.");
    }

    @Test
    @DisplayName("Biểu tượng cảm xúc KHÔNG chạm được biên byte vì biên ký tự chặn trước")
    void bienByte_bieuTuongCamXucBiChanTruoc() {
        // Một biểu tượng cảm xúc là CẶP THAY THẾ UTF-16: String.length() đếm nó là 2, còn
        // UTF-8 đếm là 4 byte. Nhiều nhất 15 biểu tượng mới đủ 30 "ký tự" theo cách Java
        // đếm, mà 15 × 4 = 60 byte — chưa tới ngưỡng 72. Nên với biểu tượng cảm xúc, biên
        // ký tự luôn chặn trước và biên byte không bao giờ chạm tới.
        String toiDa = BIEU_TUONG.repeat(15);
        assertThat(toiDa.length()).isEqualTo(30);
        assertThat(toiDa.getBytes(StandardCharsets.UTF_8)).hasSize(60);
        assertThat(PasswordPolicy.hopLe(toiDa)).isTrue();

        String qua = BIEU_TUONG.repeat(16);
        assertThat(qua.length()).isEqualTo(32);
        assertThat(PasswordPolicy.kiemTra(qua))
                .as("bị chặn bởi biên KÝ TỰ chứ không phải biên byte")
                .isEqualTo("Mật khẩu không được quá 30 ký tự.");
    }

    @Test
    @DisplayName("Chỉ ký tự 3 byte mới lọt biên ký tự rồi vướng biên byte")
    void chiKyTu3ByteMoiChamDuocBienByte() {
        // Đây là kết luận quan trọng của cả phân tích: khoảng "lọt lưới" chỉ tồn tại với
        // ký tự 3 byte. Ký tự 1 byte (ASCII) không bao giờ tới 72 byte trong 30 ký tự;
        // ký tự 4 byte thì bị biên ký tự chặn trước. Chỉ 3 byte là vừa khít lọt qua.
        for (int soKyTu = PasswordPolicy.SO_KY_TU_TOI_THIEU;
             soKyTu <= PasswordPolicy.SO_KY_TU_TOI_DA; soKyTu++) {
            String matKhau = CHU_CO_DAU.repeat(soKyTu);
            boolean quaByte = matKhau.getBytes(StandardCharsets.UTF_8).length
                    > PasswordPolicy.SO_BYTE_TOI_DA;
            assertThat(PasswordPolicy.hopLe(matKhau))
                    .as("%d ký tự tiếng Việt = %d byte", soKyTu,
                            matKhau.getBytes(StandardCharsets.UTF_8).length)
                    .isEqualTo(!quaByte);
        }
    }

    @Test
    @DisplayName("Mật khẩu ASCII không bao giờ chạm biên byte — vì sao lỗi này ẩn lâu")
    void bienByte_asciiKhongBaoGioCham() {
        // 30 ký tự ASCII là 30 byte, cách ngưỡng 72 rất xa. Bộ kiểm thử nào chỉ dùng
        // "p".repeat(n) sẽ không bao giờ phát hiện được biên thứ hai.
        String toiDaAscii = "p".repeat(PasswordPolicy.SO_KY_TU_TOI_DA);

        assertThat(toiDaAscii.getBytes(StandardCharsets.UTF_8))
                .hasSize(PasswordPolicy.SO_KY_TU_TOI_DA);
        assertThat(toiDaAscii.getBytes(StandardCharsets.UTF_8).length)
                .isLessThan(PasswordPolicy.SO_BYTE_TOI_DA);
    }

    // ================================================================
    // BẢNG 4 — Biên 72 byte là có thật, không phải nhóm tự đặt
    // ================================================================

    @Test
    @DisplayName("BCrypt chấp nhận đúng 72 byte và ném lỗi ở 73 byte")
    void bcrypt_nguong72ByteLaCoThat() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        assertThatCode(() -> encoder.encode("p".repeat(72)))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> encoder.encode("p".repeat(73)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("password cannot be more than 72 bytes");
    }

    @Test
    @DisplayName("Mọi mật khẩu qua được PasswordPolicy đều mã hoá được, không ném lỗi")
    void moiMatKhauHopLeDeuMaHoaDuoc() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        for (String matKhau : new String[]{
                "p".repeat(6), "p".repeat(30),
                CHU_CO_DAU.repeat(24), BIEU_TUONG.repeat(15),
                "MậtKhẩuCủaTôi123", "Pa$$w0rd!"}) {
            assertThat(PasswordPolicy.hopLe(matKhau))
                    .as("mật khẩu %d ký tự / %d byte phải hợp lệ",
                            matKhau.length(), matKhau.getBytes(StandardCharsets.UTF_8).length)
                    .isTrue();
            assertThatCode(() -> encoder.encode(matKhau))
                    .as("đã hợp lệ thì BCrypt phải mã hoá được")
                    .doesNotThrowAnyException();
        }
    }
}
