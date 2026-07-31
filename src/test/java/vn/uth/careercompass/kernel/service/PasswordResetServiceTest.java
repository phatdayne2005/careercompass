package vn.uth.careercompass.kernel.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import vn.uth.careercompass.kernel.entity.AuthProvider;
import vn.uth.careercompass.kernel.entity.PasswordResetToken;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.PasswordResetTokenRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link PasswordResetService}.
 *
 * <p>KỸ THUẬT MỚI ở file này: service có field {@code @Value("${app.base-url}") baseUrl}.
 * Khi unit test KHÔNG có Spring nên field này = null. Ta tự "tiêm tay" bằng
 * {@link ReflectionTestUtils#setField} trong {@code @BeforeEach} (xem bên dưới).
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

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

    @BeforeEach
    void setUp() {
        // Spring không chạy -> @Value không được inject. Ta gán tay giá trị baseUrl
        // để link trong email được ghép đúng. Đây là cách chuẩn xử lý field @Value trong unit test.
        ReflectionTestUtils.setField(passwordResetService, "baseUrl", "http://localhost:8080");
    }

    // ============================================================
    // createResetToken(email)
    // ============================================================

    @Test
    void createResetToken_whenLocalUser_savesTokenAndSendsEmail() {
        // Given: user LOCAL tồn tại
        User user = User.builder().email("a@uth.edu.vn").authProvider(AuthProvider.LOCAL).build();
        when(userRepository.findByEmail("a@uth.edu.vn")).thenReturn(Optional.of(user));

        // When
        passwordResetService.createResetToken("a@uth.edu.vn");

        // Then: token được lưu, đúng user, hạn 30' trong tương lai, chưa dùng
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isNotBlank();
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());

        // Và: email được gửi tới đúng địa chỉ, nội dung chứa link kèm token vừa tạo.
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendEmail(eq("a@uth.edu.vn"), anyString(), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
                .contains("http://localhost:8080/reset-password?token=" + saved.getToken());
    }

    @Test
    void createResetToken_whenEmailNotFound_doesNothing() {
        // Given: email không tồn tại -> service phải IM LẶNG (chống dò email tồn tại hay không)
        when(userRepository.findByEmail("ghost@uth.edu.vn")).thenReturn(Optional.empty());

        // When
        passwordResetService.createResetToken("ghost@uth.edu.vn");

        // Then: không tạo token, không gửi mail
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    @Test
    void createResetToken_whenGoogleUser_doesNothing() {
        // Given: user đăng nhập bằng Google -> không có mật khẩu để reset -> bỏ qua
        User googleUser = User.builder().email("g@uth.edu.vn").authProvider(AuthProvider.GOOGLE).build();
        when(userRepository.findByEmail("g@uth.edu.vn")).thenReturn(Optional.of(googleUser));

        // When
        passwordResetService.createResetToken("g@uth.edu.vn");

        // Then: tuyệt đối không tạo token / gửi mail
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendEmail(any(), any(), any());
    }

    // ============================================================
    // validateToken(token)
    // ============================================================

    @Test
    void validateToken_whenValid_returnsToken() {
        PasswordResetToken token = PasswordResetToken.builder()
                .token("abc")
                .expiresAt(LocalDateTime.now().plusMinutes(10)) // còn hạn
                .used(false)                                    // chưa dùng
                .build();
        when(passwordResetTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));

        Optional<PasswordResetToken> result = passwordResetService.validateToken("abc");

        assertThat(result).contains(token);
    }

    @Test
    void validateToken_whenExpired_returnsEmpty() {
        // Token hết hạn -> isValid()=false -> filter loại bỏ -> Optional.empty
        PasswordResetToken expired = PasswordResetToken.builder()
                .token("old")
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // đã quá hạn
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByToken("old")).thenReturn(Optional.of(expired));

        assertThat(passwordResetService.validateToken("old")).isEmpty();
    }

    @Test
    void validateToken_whenNotFound_returnsEmpty() {
        when(passwordResetTokenRepository.findByToken("nope")).thenReturn(Optional.empty());

        assertThat(passwordResetService.validateToken("nope")).isEmpty();
    }

    // ============================================================
    // resetPassword(token, newPassword)
    // ============================================================

    @Test
    void resetPassword_whenValid_encodesPasswordAndMarksUsed() {
        User user = User.builder().email("a@uth.edu.vn").passwordHash("OLD").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .token("abc")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(false)
                .build();
        when(passwordResetTokenRepository.findByToken("abc")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpass")).thenReturn("NEW_HASH");

        passwordResetService.resetPassword("abc", "newpass");

        // Mật khẩu mới đã được hash và gán vào user
        assertThat(user.getPasswordHash()).isEqualTo("NEW_HASH");
        verify(userRepository).save(user);
        // Token bị đánh dấu đã dùng -> không thể tái sử dụng (one-time)
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void resetPassword_whenTokenAlreadyUsed_throwsAndDoesNotSave() {
        User user = User.builder().passwordHash("OLD").build();
        PasswordResetToken usedToken = PasswordResetToken.builder()
                .token("abc")
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .used(true) // đã dùng rồi
                .build();
        when(passwordResetTokenRepository.findByToken("abc")).thenReturn(Optional.of(usedToken));

        assertThatThrownBy(() -> passwordResetService.resetPassword("abc", "newpass"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Token không hợp lệ hoặc đã hết hạn");

        // Không được đổi mật khẩu
        assertThat(user.getPasswordHash()).isEqualTo("OLD");
        verify(userRepository, never()).save(any());
    }
}
