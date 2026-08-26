package vn.uth.careercompass.blackbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import vn.uth.careercompass.kernel.entity.PasswordResetToken;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * KỸ THUẬT: Bảng quyết định (Decision Table Testing) — bảng thứ hai.
 *
 * <p>Đối tượng: {@code PasswordResetToken.isValid()} — quyết định token đặt lại
 * mật khẩu còn dùng được hay không.
 * <pre>
 *   public boolean isValid() {
 *       return !isExpired() &amp;&amp; !isUsed();
 *   }
 * </pre>
 *
 * <p>Hai điều kiện nhị phân ĐỘC LẬP với nhau:
 * <ul>
 *   <li>C1 — Token đã hết hạn chưa (so {@code expiresAt} với hiện tại)</li>
 *   <li>C2 — Token đã được dùng chưa (cờ {@code used})</li>
 * </ul>
 *
 * <p>Số rule tối đa = 2 × 2 = <b>4</b>. Vì cả hai điều kiện đều nhị phân nên
 * ở đây công thức 2^n áp dụng được — khác với bảng ProgressService (3 × 2 = 6)
 * nơi một điều kiện có ba giá trị.
 *
 * <p>BẢNG QUYẾT ĐỊNH (bản đầy đủ):
 * <pre>
 *   Điều kiện / Hành động       R1      R2      R3      R4
 *   C1 Token đã hết hạn         N       Y       N       Y
 *   C2 Token đã được dùng       N       N       Y       Y
 *   A1 Token hợp lệ             X       -       -       -
 *   A2 Token KHÔNG hợp lệ       -       X       X       X
 * </pre>
 *
 * <p>BẢNG SAU KHI RÚT GỌN: chỉ cần MỘT trong hai điều kiện đúng là token hỏng,
 * nên ba rule R2, R3, R4 gộp được thành một:
 * <pre>
 *   Điều kiện / Hành động       R1'     R2'
 *   C1 Token đã hết hạn         N       Y hoặc -
 *   C2 Token đã được dùng       N       - hoặc Y
 *   A1 Token hợp lệ             X       -
 *   A2 Token KHÔNG hợp lệ       -       X
 * </pre>
 *
 * <p>LÝ DO CÓ FILE NÀY: {@code PasswordResetServiceTest} sẵn có chỉ phủ R1 và R2
 * cho {@code validateToken}. Rule R3 (còn hạn nhưng đã dùng) chỉ được chạm gián tiếp
 * qua {@code resetPassword}, còn R4 (vừa hết hạn vừa đã dùng) chưa test bao giờ.
 */
@DisplayName("Bảng quyết định — hiệu lực của token đặt lại mật khẩu")
class TokenValidityDecisionTableTest {

    /** Dựng token theo đúng một ô của bảng quyết định. */
    private PasswordResetToken token(boolean expired, boolean used) {
        return PasswordResetToken.builder()
                .token("tok-" + expired + "-" + used)
                .expiresAt(expired
                        ? LocalDateTime.now().minusMinutes(1)   // đã quá hạn
                        : LocalDateTime.now().plusMinutes(30))  // còn hạn
                .used(used)
                .build();
    }

    @Test
    @DisplayName("R1 · còn hạn + chưa dùng → token HỢP LỆ")
    void rule1_conHan_chuaDung_hopLe() {
        PasswordResetToken t = token(false, false);

        assertThat(t.isExpired()).as("C1 — đã hết hạn").isFalse();
        assertThat(t.isUsed()).as("C2 — đã được dùng").isFalse();
        assertThat(t.isValid()).as("A1 — token hợp lệ").isTrue();
    }

    @Test
    @DisplayName("R2 · hết hạn + chưa dùng → token KHÔNG hợp lệ")
    void rule2_hetHan_chuaDung_khongHopLe() {
        PasswordResetToken t = token(true, false);

        assertThat(t.isExpired()).isTrue();
        assertThat(t.isUsed()).isFalse();
        assertThat(t.isValid()).as("A2 — token không hợp lệ").isFalse();
    }

    @Test
    @DisplayName("R3 · còn hạn + đã dùng → token KHÔNG hợp lệ")
    void rule3_conHan_daDung_khongHopLe() {
        PasswordResetToken t = token(false, true);

        assertThat(t.isExpired()).isFalse();
        assertThat(t.isUsed()).isTrue();
        assertThat(t.isValid()).as("A2 — dùng một lần rồi thì hỏng, dù còn hạn").isFalse();
    }

    @Test
    @DisplayName("R4 · hết hạn + đã dùng → token KHÔNG hợp lệ")
    void rule4_hetHan_daDung_khongHopLe() {
        PasswordResetToken t = token(true, true);

        assertThat(t.isExpired()).isTrue();
        assertThat(t.isUsed()).isTrue();
        assertThat(t.isValid()).as("A2 — cả hai điều kiện hỏng cùng lúc").isFalse();
    }
}
