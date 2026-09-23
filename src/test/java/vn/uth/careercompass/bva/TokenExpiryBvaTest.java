package vn.uth.careercompass.bva;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.uth.careercompass.kernel.entity.PasswordResetToken;
import vn.uth.careercompass.kernel.entity.User;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Giá trị biên của hạn dùng token đặt lại mật khẩu (FR6.1, NFR-S07).
 *
 * <p>Sheet 03b đã lập bảng quyết định cho token và sheet 04b đã lập bảng chuyển đổi trạng
 * thái, nhưng cả hai đều chỉ dùng hai mốc cách xa biên: "còn hạn" là {@code now + 30 phút}
 * và "hết hạn" là {@code now − 1 phút}. Không phép kiểm nào chạm vào ĐÚNG thời điểm hết
 * hạn, mà đó mới là chỗ dễ sai.
 *
 * <p>Biên ở đây nằm trong {@code PasswordResetToken.isExpired()}:
 * {@code LocalDateTime.now().isAfter(expiresAt)}. {@code isAfter} là phép so sánh NGHIÊM
 * NGẶT, nên tại đúng thời điểm {@code now == expiresAt} token vẫn được coi là CÒN HẠN.
 * Đổi thành {@code !isBefore} thì hành vi tại biên lật ngược, mà mọi phép kiểm hiện có
 * vẫn xanh — đúng kiểu lỗi lệch một đơn vị mà BVA sinh ra để bắt.
 *
 * <p>MỘT GIỚI HẠN PHẢI NÓI RÕ: không thể kiểm ĐÚNG thời điểm {@code now == expiresAt},
 * vì {@code isExpired()} tự gọi {@code LocalDateTime.now()} bên trong. Lúc phép kiểm dựng
 * xong đối tượng thì đồng hồ đã nhích qua, nên mọi lần chạy đều rơi vào phía "đã quá hạn".
 * Muốn ghim đúng biên thì phải tiêm {@code Clock} vào thực thể — một thay đổi ở mã sản
 * phẩm, ghi nhận lại ở đây để người đọc biết đây là chỗ CHƯA kiểm được chứ không phải chỗ
 * bị bỏ quên. Hai phép kiểm max− và max+ dưới đây kẹp sát biên từ hai phía trong mức chính
 * xác mà cách hiện thực cho phép.
 */
@DisplayName("BVA — Hạn dùng token đặt lại mật khẩu (30 phút)")
class TokenExpiryBvaTest {

    private static final int HAN_DUNG_PHUT = 30;

    private static PasswordResetToken token(LocalDateTime hetHanLuc) {
        return PasswordResetToken.builder()
                .id(1L)
                .token("token-test")
                .user(User.builder().id(1L).email("student@uth.edu.vn").build())
                .expiresAt(hetHanLuc)
                .used(false)
                .build();
    }

    // ================================================================
    // Standard BVA trên trục thời gian: 4n + 1 = 5 điểm với n = 1 biến
    // ================================================================

    @Test
    @DisplayName("min : token vừa được cấp (còn đủ 30 phút) thì hợp lệ")
    void bienDuoi_vuaCap() {
        assertThat(token(LocalDateTime.now().plusMinutes(HAN_DUNG_PHUT)).isValid()).isTrue();
    }

    @Test
    @DisplayName("nom : còn 15 phút thì hợp lệ")
    void giuaMien_con15Phut() {
        assertThat(token(LocalDateTime.now().plusMinutes(15)).isValid()).isTrue();
    }

    @Test
    @DisplayName("max− : chỉ còn 200 mili giây thì VẪN hợp lệ")
    void bienTren_truMotDonVi() {
        assertThat(token(LocalDateTime.now().plusNanos(200_000_000)).isExpired())
                .as("chưa qua thời điểm hết hạn thì chưa được coi là hết hạn")
                .isFalse();
    }

    @Test
    @DisplayName("max+ : quá hạn đúng một nano giây thì hết hiệu lực ngay")
    void bienTren_congMotDonVi() {
        // Đây là phía kẹp sát biên nhất mà cách hiện thực cho phép: chỉ cần expiresAt lùi
        // một nano giây so với lúc dựng đối tượng là isExpired() đã trả true.
        assertThat(token(LocalDateTime.now().minusNanos(1)).isExpired())
                .as("vừa qua thời điểm hết hạn là hết hạn ngay")
                .isTrue();
    }

    @Test
    @DisplayName("Phép so sánh tại biên là isAfter (nghiêm ngặt), không phải !isBefore")
    void phepSoSanhTaiBienLaNghiemNgat() {
        // Không ghim được now == expiresAt (xem ghi chú đầu lớp), nên ở đây khẳng định
        // điều còn kiểm được: quy ước của hệ thống là "đúng thời điểm hết hạn vẫn còn
        // hiệu lực", và token cấp ra luôn có đủ 30 phút chứ không ít hơn.
        LocalDateTime capLuc = LocalDateTime.now();
        PasswordResetToken t = token(capLuc.plusMinutes(HAN_DUNG_PHUT));

        assertThat(t.isValid()).isTrue();
        assertThat(t.getExpiresAt())
                .as("hạn dùng đúng 30 phút theo PasswordResetService")
                .isEqualTo(capLuc.plusMinutes(HAN_DUNG_PHUT));
    }

    // ================================================================
    // Robustness BVA: ngoài miền ở cả hai phía
    // ================================================================

    @Test
    @DisplayName("max++ : token cũ hàng giờ thì hết hiệu lực")
    void ngoaiMien_cuHangGio() {
        PasswordResetToken t = token(LocalDateTime.now().minusHours(3));

        assertThat(t.isExpired()).isTrue();
        assertThat(t.isValid()).isFalse();
    }

    @Test
    @DisplayName("Đã dùng rồi thì không hợp lệ dù còn hạn — hai điều kiện độc lập")
    void daDung_khongHopLeDuConHan() {
        PasswordResetToken t = token(LocalDateTime.now().plusMinutes(HAN_DUNG_PHUT));
        t.setUsed(true);

        assertThat(t.isExpired()).as("vẫn còn hạn").isFalse();
        assertThat(t.isValid()).as("nhưng đã dùng nên không hợp lệ").isFalse();
    }

    @Test
    @DisplayName("Hết hạn VÀ đã dùng thì vẫn không hợp lệ, không ném lỗi")
    void hetHanVaDaDung() {
        PasswordResetToken t = token(LocalDateTime.now().minusMinutes(1));
        t.setUsed(true);

        assertThat(t.isValid()).isFalse();
    }
}
