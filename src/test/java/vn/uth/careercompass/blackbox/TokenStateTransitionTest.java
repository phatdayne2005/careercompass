package vn.uth.careercompass.blackbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.uth.careercompass.kernel.entity.PasswordResetToken;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.PasswordResetTokenRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.service.EmailService;
import vn.uth.careercompass.kernel.service.PasswordResetService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KỸ THUẬT: chuyển đổi trạng thái, theo mẫu slide 39.
 *
 * <p>Đối tượng: vòng đời của một token đặt lại mật khẩu.
 *
 * <p><b>Vì sao lại kiểm chính đối tượng đã có bảng quyết định?</b> Vì hai kỹ thuật trả lời
 * hai câu hỏi khác nhau, và câu hỏi thứ hai quan trọng hơn về mặt bảo mật:
 *
 * <table border="1">
 *   <caption>So sánh hai kỹ thuật trên cùng một đối tượng</caption>
 *   <tr><th>Kỹ thuật</th><th>Câu hỏi</th><th>Tính chất</th></tr>
 *   <tr><td>Bảng quyết định (sheet 03b)</td>
 *       <td>Token này <i>tại thời điểm này</i> có hợp lệ không?</td>
 *       <td>Tĩnh — chụp một khoảnh khắc</td></tr>
 *   <tr><td>Chuyển trạng thái (sheet này)</td>
 *       <td>Token đi qua được những <i>chuỗi sự kiện</i> nào?</td>
 *       <td>Theo thời gian — ràng buộc về thứ tự</td></tr>
 * </table>
 *
 * <p>Bảng quyết định KHÔNG diễn tả nổi tính chất "dùng rồi thì không dùng lại được": bốn luật
 * của nó chỉ mô tả bốn tổ hợp cờ, không nói gì về việc cờ đó đi tới bằng con đường nào. Chỉ
 * máy trạng thái mới ràng buộc được thứ tự. Đó cũng là lý do TC-ST-04 dưới đây — dùng token
 * hai lần liên tiếp — là test đáng giá nhất của lớp này.
 *
 * <p><b>Máy trạng thái</b> (xem sơ đồ ở sheet 04b của báo cáo):
 * <pre>
 *                     [tạo token]
 *                          ↓
 *                HỢP LỆ (chưa dùng, còn hạn)
 *                    ↓            ↓
 *          [đặt lại mật khẩu]  [quá 30 phút]
 *                    ↓            ↓
 *                ĐÃ DÙNG       HẾT HẠN
 * </pre>
 *
 * <p>Hai chuyển đổi ra khỏi HỢP LỆ đều <b>MỘT CHIỀU</b>: không sự kiện nào đưa token quay lại
 * trạng thái hợp lệ. Đó chính là thuộc tính an toàn cần bảo vệ.
 *
 * <p>Tiêu chí đủ: phủ hết cạnh của sơ đồ — 3 cạnh hợp lệ và 3 cạnh bị chặn.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Chuyen doi trang thai - vong doi token dat lai mat khau")
// Giữ thứ tự TC-ST-01..06 đúng như các dòng của bảng chuyển trạng thái trong báo cáo.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TokenStateTransitionTest {

    private static final String MA_TOKEN = "token-kiem-thu";
    private static final String MAT_KHAU_MOI = "MatKhauMoi@123";

    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private PasswordResetService passwordResetService;

    // ================================================================
    // Dựng token ở từng trạng thái của máy
    // ================================================================

    /** Trạng thái HỢP LỆ: chưa dùng, còn 30 phút nữa mới hết hạn. */
    private PasswordResetToken tokenHopLe() {
        return PasswordResetToken.builder()
                .token(MA_TOKEN)
                .user(User.builder().id(1L).email("student@gmail.com").build())
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build();
    }

    /** Trạng thái ĐÃ DÙNG: còn hạn nhưng cờ used đã bật. */
    private PasswordResetToken tokenDaDung() {
        PasswordResetToken t = tokenHopLe();
        t.setUsed(true);
        return t;
    }

    /** Trạng thái HẾT HẠN: chưa dùng nhưng đã quá thời hạn. */
    private PasswordResetToken tokenHetHan() {
        PasswordResetToken t = tokenHopLe();
        t.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        return t;
    }

    private void choRepositoryTraVe(PasswordResetToken t) {
        when(passwordResetTokenRepository.findByToken(MA_TOKEN)).thenReturn(Optional.of(t));
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$bam");
    }

    // ================================================================
    // Ba cạnh HỢP LỆ của sơ đồ
    // ================================================================

    @Test
    @Order(1)
    @DisplayName("TC-ST-01 | (chua co) --[tao token]--> HOP LE")
    void st01_taoToken_sangTrangThaiHopLe() {
        // authProvider phải là LOCAL: createResetToken thoát sớm với tài khoản đăng nhập
        // bằng Google, vì mật khẩu của họ do Google giữ chứ không nằm trong hệ thống này.
        // Bỏ sót dòng này thì service không tạo token nào và test đỏ ngay — chính lần chạy
        // đầu tiên đã vấp đúng chỗ đó.
        User user = User.builder().id(1L).email("student@gmail.com")
                .authProvider(vn.uth.careercompass.kernel.entity.AuthProvider.LOCAL).build();
        when(userRepository.findByEmail("student@gmail.com")).thenReturn(Optional.of(user));

        passwordResetService.createResetToken("student@gmail.com");

        // Token vừa tạo phải nằm ở trạng thái HỢP LỆ: chưa dùng và còn hạn.
        var bat = org.mockito.ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(bat.capture());
        PasswordResetToken moi = bat.getValue();

        assertThat(moi.isUsed()).as("token mới tạo phải chưa dùng").isFalse();
        assertThat(moi.isExpired()).as("token mới tạo phải còn hạn").isFalse();
        assertThat(moi.isValid()).as("token mới tạo phải hợp lệ").isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("TC-ST-02 | HOP LE --[dat lai mat khau]--> DA DUNG")
    void st02_hopLe_sangDaDung() {
        PasswordResetToken t = tokenHopLe();
        choRepositoryTraVe(t);

        passwordResetService.resetPassword(MA_TOKEN, MAT_KHAU_MOI);

        assertThat(t.isUsed()).as("sau khi đặt lại, token phải chuyển sang ĐÃ DÙNG").isTrue();
        assertThat(t.isValid()).as("ĐÃ DÙNG thì không còn hợp lệ").isFalse();
        verify(userRepository).save(t.getUser());
    }

    @Test
    @Order(3)
    @DisplayName("TC-ST-03 | HOP LE --[qua 30 phut]--> HET HAN")
    void st03_hopLe_sangHetHan() {
        PasswordResetToken t = tokenHopLe();
        assertThat(t.isValid()).as("trước khi hết hạn vẫn hợp lệ").isTrue();

        // Sự kiện "thời gian trôi qua": đẩy mốc hết hạn về quá khứ.
        t.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertThat(t.isExpired()).as("đã quá mốc hết hạn").isTrue();
        assertThat(t.isUsed()).as("hết hạn KHÔNG kéo theo đã dùng").isFalse();
        assertThat(t.isValid()).as("HẾT HẠN thì không còn hợp lệ").isFalse();
    }

    // ================================================================
    // Ba cạnh BỊ CHẶN — thuộc tính an toàn của máy trạng thái
    // ================================================================

    /**
     * Test đáng giá nhất của lớp này.
     *
     * <p>Bảng quyết định ở sheet 03b chỉ nói "token đã dùng thì không hợp lệ". Nó không nói
     * được rằng KHÔNG CÓ đường nào đưa token từ ĐÃ DÙNG quay về HỢP LỆ. Chuỗi hai sự kiện
     * dưới đây mới kiểm chứng được điều đó.
     */
    @Test
    @Order(4)
    @DisplayName("TC-ST-04 | DA DUNG --[dat lai lan nua]--> BI CHAN, khong quay lai HOP LE")
    void st04_daDung_dungLaiBiChan() {
        PasswordResetToken t = tokenHopLe();
        choRepositoryTraVe(t);

        // Sự kiện 1: dùng token lần đầu — thành công.
        passwordResetService.resetPassword(MA_TOKEN, MAT_KHAU_MOI);
        assertThat(t.isUsed()).isTrue();

        // Sự kiện 2: dùng LẠI chính token đó — phải bị từ chối.
        assertThatThrownBy(() -> passwordResetService.resetPassword(MA_TOKEN, "MatKhauKhac@456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn");

        assertThat(t.isValid()).as("token vẫn phải ở trạng thái ĐÃ DÙNG").isFalse();
        // Mật khẩu chỉ được đổi đúng MỘT lần, ở sự kiện đầu tiên.
        verify(userRepository, org.mockito.Mockito.times(1)).save(t.getUser());
    }

    @Test
    @Order(5)
    @DisplayName("TC-ST-05 | HET HAN --[dat lai mat khau]--> BI CHAN")
    void st05_hetHan_dungBiChan() {
        PasswordResetToken t = tokenHetHan();
        when(passwordResetTokenRepository.findByToken(MA_TOKEN)).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> passwordResetService.resetPassword(MA_TOKEN, MAT_KHAU_MOI))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn");

        assertThat(t.isUsed()).as("bị chặn thì không được đánh dấu đã dùng").isFalse();
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    @Order(6)
    @DisplayName("TC-ST-06 | DA DUNG --[qua 30 phut]--> van DA DUNG, khong tro ve HOP LE")
    void st06_daDung_hetHanVanKhongHopLe() {
        PasswordResetToken t = tokenDaDung();

        // Sự kiện "thời gian trôi qua" tác động lên token đã dùng.
        t.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertThat(t.isUsed()).as("vẫn giữ cờ đã dùng").isTrue();
        assertThat(t.isExpired()).as("và nay thêm hết hạn").isTrue();
        assertThat(t.isValid()).as("hai cờ cùng bật thì càng không hợp lệ").isFalse();
    }

    // ================================================================
    // Cổng vào validateToken — chỉ trạng thái HỢP LỆ mới đi qua được
    // ================================================================

    @Test
    @Order(7)
    @DisplayName("TC-ST-07 | validateToken chi chap nhan trang thai HOP LE")
    void st07_validateToken_chiQuaOTrangThaiHopLe() {
        for (PasswordResetToken t : new PasswordResetToken[]{tokenDaDung(), tokenHetHan()}) {
            when(passwordResetTokenRepository.findByToken(MA_TOKEN)).thenReturn(Optional.of(t));
            assertThat(passwordResetService.validateToken(MA_TOKEN))
                    .as("token ở trạng thái không hợp lệ phải bị lọc bỏ")
                    .isEmpty();
        }
        when(passwordResetTokenRepository.findByToken(MA_TOKEN)).thenReturn(Optional.of(tokenHopLe()));
        assertThat(passwordResetService.validateToken(MA_TOKEN))
                .as("chỉ token HỢP LỆ mới đi qua được cổng này")
                .isPresent();
    }
}
