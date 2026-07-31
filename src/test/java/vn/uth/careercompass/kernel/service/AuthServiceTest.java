package vn.uth.careercompass.kernel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.uth.careercompass.kernel.entity.AuthProvider;
import vn.uth.careercompass.kernel.entity.KernelActivityLogType;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.exception.EmailAlreadyExistsException;
import vn.uth.careercompass.kernel.repository.RoleRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link AuthService}.
 *
 * <p>MỤC TIÊU: test RIÊNG logic của AuthService, KHÔNG đụng DB thật, KHÔNG bật Spring.
 * Mọi dependency (UserRepository, RoleRepository, PasswordEncoder, ActivityLogService)
 * đều được Mockito "giả lập" (mock) — ta tự dạy chúng trả về gì.
 *
 * <p>WHY mock? Vì unit test chỉ quan tâm: "cho input X + dependency phản hồi Y,
 * thì AuthService cư xử đúng không?". Ta không test lại code của Spring Data hay BCrypt.
 */
// @ExtendWith(MockitoExtension.class): kích hoạt Mockito cho JUnit 5.
// Nó sẽ tự động khởi tạo các field @Mock và tiêm chúng vào @InjectMocks trước mỗi @Test.
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // @Mock = tạo 1 "đồ giả" của interface/class này. Mặc định mọi method trả về
    // giá trị rỗng (null / false / Optional.empty) cho tới khi ta dạy nó bằng when(...).
    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ActivityLogService activityLogService;

    // @InjectMocks = tạo THẬT 1 AuthService, rồi nhét 4 mock ở trên vào constructor của nó.
    // Đây chính là "đối tượng đang được kiểm thử" (System Under Test - SUT).
    @InjectMocks
    private AuthService authService;

    // ============================================================================
    // TEST 1 — HAPPY PATH: đăng ký thành công
    // ============================================================================
    @Test
    void register_whenEmailIsNew_savesUserAndLogsActivity() {
        // ---------- ARRANGE (Given): chuẩn bị dữ liệu + dạy mock ----------
        String fullName = "Nguyen Van A";
        String email = "a@uth.edu.vn";
        String rawPassword = "secret123";

        Role studentRole = Role.builder().name(RoleName.STUDENT).build();

        // Dạy mock: email này CHƯA tồn tại -> cho phép đăng ký tiếp.
        when(userRepository.existsByEmail(email)).thenReturn(false);
        // Dạy mock: Role STUDENT đã được seed -> trả về role.
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.of(studentRole));
        // Dạy mock: khi encode "secret123" thì trả chuỗi hash giả.
        // WHY: ta không test thuật toán BCrypt, chỉ cần biết service CÓ gọi encode
        // và LƯU chuỗi đã hash (chứ không lưu password thô).
        when(passwordEncoder.encode(rawPassword)).thenReturn("HASHED_PW");

        // ---------- ACT (When): gọi method cần test ----------
        authService.register(fullName, email, rawPassword);

        // ---------- ASSERT (Then): kiểm kết quả ----------
        // ArgumentCaptor = "cái bẫy" bắt lại đối tượng User thực sự được truyền vào save(),
        // để ta soi từng field bên trong nó.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        // verify(...) = khẳng định "save() ĐÃ được gọi đúng 1 lần", đồng thời chụp lại tham số.
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        // Kiểm các field được set đúng khi build User:
        assertThat(savedUser.getFullName()).isEqualTo(fullName);
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getPasswordHash()).isEqualTo("HASHED_PW"); // lưu HASH, không phải "secret123"
        assertThat(savedUser.getPasswordHash()).isNotEqualTo(rawPassword);
        assertThat(savedUser.getRole()).isEqualTo(studentRole);
        assertThat(savedUser.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);

        // Khẳng định có ghi ActivityLog loại REGISTER cho đúng user vừa tạo.
        verify(activityLogService).log(savedUser, KernelActivityLogType.REGISTER, "Đăng ký tài khoản mới");
    }

    // ============================================================================
    // TEST 2 — email đã tồn tại: phải ném exception, KHÔNG lưu gì
    // ============================================================================
    @Test
    void register_whenEmailExists_throwsAndDoesNotSave() {
        // ---------- ARRANGE ----------
        String email = "existing@uth.edu.vn";
        // Dạy mock: email đã tồn tại -> service phải chặn ngay.
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // ---------- ACT + ASSERT ----------
        // assertThatThrownBy: chạy đoạn code trong lambda và khẳng định nó ném đúng exception.
        assertThatThrownBy(() -> authService.register("Ten", email, "pw"))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessage("Email đã được đăng ký");

        // Rất quan trọng: khẳng định KHÔNG hề gọi save() -> không tạo user rác trong DB.
        verify(userRepository, never()).save(any());
        // Cũng không được ghi log đăng ký.
        verify(activityLogService, never()).log(any(), any(), any());
        // LƯU Ý: ở test này ta KHÔNG stub roleRepository hay passwordEncoder.
        // Vì luồng dừng sớm ở check email, nếu stub thừa Mockito (strict mode) sẽ báo lỗi
        // UnnecessaryStubbingException — đây là 1 pitfall kinh điển cần nhớ.
    }

    // ============================================================================
    // TEST 3 — chưa seed Role STUDENT: phải ném IllegalStateException
    // ============================================================================
    @Test
    void register_whenStudentRoleNotSeeded_throwsIllegalState() {
        // ---------- ARRANGE ----------
        String email = "new@uth.edu.vn";
        when(userRepository.existsByEmail(email)).thenReturn(false);
        // Dạy mock: tra Role STUDENT ra rỗng -> mô phỏng DB chưa seed role.
        when(roleRepository.findByName(RoleName.STUDENT)).thenReturn(Optional.empty());

        // ---------- ACT + ASSERT ----------
        assertThatThrownBy(() -> authService.register("Ten", email, "pw"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Chưa seed Role Student");

        // Lỗi cấu hình hệ thống -> không được tạo user.
        verify(userRepository, never()).save(any());
    }
}
