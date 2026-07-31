package vn.uth.careercompass.profile.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import vn.uth.careercompass.kernel.entity.AuthProvider;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.service.UserProfileService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link ProfileService} — màn hình Hồ sơ (P3).
 *
 * <p>Service này pha trộn 2 kiểu method:
 * <ul>
 *   <li>Ủy quyền thuần cho {@link UserProfileService} (updateGithub / updateSkills).</li>
 *   <li>Tự xử lý validate + save (updateName / updateEmail / changePassword).</li>
 * </ul>
 * Trọng tâm test là nhóm thứ 2 vì có nhiều nhánh guard (LOCAL vs GOOGLE, trùng email, sai mật khẩu).
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProfileService profileService;

    // ============================================================
    // getProfile — trả về chính user (không biến đổi)
    // ============================================================

    @Test
    void getProfile_returnsSameUser() {
        User user = User.builder().build();
        assertThat(profileService.getProfile(user)).isSameAs(user);
    }

    // ============================================================
    // updateGithub / updateSkills — chỉ ủy quyền
    // ============================================================

    @Test
    void updateGithub_delegatesToUserProfileService() {
        User user = User.builder().build();

        profileService.updateGithub(user, "octocat");

        // Chỉ cần khẳng định có gọi đúng method của service kernel với đúng tham số.
        verify(userProfileService).setGithub(user, "octocat");
    }

    @Test
    void updateSkills_delegatesToUserProfileService() {
        User user = User.builder().build();
        List<Long> ids = List.of(1L, 2L, 3L);

        profileService.updateSkills(user, ids);

        verify(userProfileService).replaceSkills(user, ids);
    }

    // ============================================================
    // updateName — cắt khoảng trắng rồi lưu
    // ============================================================

    @Test
    void updateName_trimsAndSaves() {
        User user = User.builder().build();

        profileService.updateName(user, "  Nguyen Van A  ");

        // Họ tên phải được trim (tránh lưu dư khoảng trắng đầu/cuối).
        assertThat(user.getFullName()).isEqualTo("Nguyen Van A");
        verify(userRepository).save(user);
    }

    // ============================================================
    // updateEmail — nhiều nhánh guard
    // ============================================================

    @Test
    void updateEmail_whenGoogleAccount_throwsAndDoesNotSave() {
        // Given: tài khoản Google không được đổi email tại đây.
        User user = User.builder().email("g@uth.edu.vn").authProvider(AuthProvider.GOOGLE).build();

        assertThatThrownBy(() -> profileService.updateEmail(user, "new@uth.edu.vn"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tài khoản đăng nhập bằng Google không thể đổi email tại đây.");

        verify(userRepository, never()).save(any());
        // Không cần chạm tới repository để kiểm trùng email.
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void updateEmail_whenBlankEmail_throwsAndDoesNotSave() {
        // Given: email null -> chuẩn hoá thành "" -> rỗng -> chặn.
        User user = User.builder().email("a@uth.edu.vn").authProvider(AuthProvider.LOCAL).build();

        assertThatThrownBy(() -> profileService.updateEmail(user, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email không được để trống.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmail_whenSameEmailIgnoreCase_returnsWithoutSaving() {
        // Given: email mới trùng email cũ (chỉ khác hoa/thường) -> không đổi gì, không lưu.
        User user = User.builder().email("a@uth.edu.vn").authProvider(AuthProvider.LOCAL).build();

        profileService.updateEmail(user, "A@UTH.EDU.VN");

        // Vì "không có gì thay đổi" nên service thoát sớm: không tra trùng, không save.
        assertThat(user.getEmail()).isEqualTo("a@uth.edu.vn");
        verify(userRepository, never()).findByEmail(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmail_whenEmailAlreadyUsed_throwsAndDoesNotSave() {
        // Given: email mới đã thuộc về tài khoản khác.
        User user = User.builder().email("old@uth.edu.vn").authProvider(AuthProvider.LOCAL).build();
        User other = User.builder().email("taken@uth.edu.vn").build();
        when(userRepository.findByEmail("taken@uth.edu.vn")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> profileService.updateEmail(user, "taken@uth.edu.vn"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Email này đã được sử dụng bởi tài khoản khác.");

        // Email cũ giữ nguyên, không lưu.
        assertThat(user.getEmail()).isEqualTo("old@uth.edu.vn");
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateEmail_whenValidNewEmail_normalizesAndSaves() {
        // Given: email mới hợp lệ, chưa ai dùng -> đổi (đã trim + lowercase) rồi lưu.
        User user = User.builder().email("old@uth.edu.vn").authProvider(AuthProvider.LOCAL).build();
        when(userRepository.findByEmail("new@uth.edu.vn")).thenReturn(Optional.empty());

        profileService.updateEmail(user, "  NEW@UTH.EDU.VN  ");

        assertThat(user.getEmail()).isEqualTo("new@uth.edu.vn"); // đã chuẩn hoá
        verify(userRepository).save(user);
    }

    // ============================================================
    // changePassword — xác thực mật khẩu hiện tại trước
    // ============================================================

    @Test
    void changePassword_whenGoogleAccount_throwsAndDoesNotSave() {
        // Given: authProvider != LOCAL -> không có mật khẩu để đổi.
        User user = User.builder().authProvider(AuthProvider.GOOGLE).passwordHash("HASH").build();

        assertThatThrownBy(() -> profileService.changePassword(user, "cur", "new"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tài khoản Google không dùng mật khẩu tại đây.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_whenPasswordHashNull_throwsAndDoesNotSave() {
        // Given: LOCAL nhưng passwordHash null -> vẫn thuộc nhánh "không dùng mật khẩu".
        User user = User.builder().authProvider(AuthProvider.LOCAL).passwordHash(null).build();

        assertThatThrownBy(() -> profileService.changePassword(user, "cur", "new"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tài khoản Google không dùng mật khẩu tại đây.");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_whenCurrentPasswordNull_throwsAndDoesNotSave() {
        // Given: currentPassword null -> short-circuit trước khi gọi matches() -> chặn.
        User user = User.builder().authProvider(AuthProvider.LOCAL).passwordHash("HASH").build();

        assertThatThrownBy(() -> profileService.changePassword(user, null, "new"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mật khẩu hiện tại không đúng.");

        // Không stub passwordEncoder.matches vì luồng không chạm tới (tránh UnnecessaryStubbing).
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_whenCurrentPasswordWrong_throwsAndDoesNotSave() {
        // Given: mật khẩu hiện tại nhập sai -> matches() trả false -> chặn.
        User user = User.builder().authProvider(AuthProvider.LOCAL).passwordHash("HASH").build();
        when(passwordEncoder.matches("wrong", "HASH")).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(user, "wrong", "new"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Mật khẩu hiện tại không đúng.");

        assertThat(user.getPasswordHash()).isEqualTo("HASH"); // giữ nguyên
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_whenCurrentPasswordCorrect_encodesNewAndSaves() {
        // Given: mật khẩu hiện tại đúng -> hash mật khẩu mới rồi lưu.
        User user = User.builder().authProvider(AuthProvider.LOCAL).passwordHash("OLD_HASH").build();
        when(passwordEncoder.matches("current", "OLD_HASH")).thenReturn(true);
        when(passwordEncoder.encode("newSecret")).thenReturn("NEW_HASH");

        profileService.changePassword(user, "current", "newSecret");

        // Lưu HASH mới, không lưu mật khẩu thô.
        assertThat(user.getPasswordHash()).isEqualTo("NEW_HASH");
        verify(userRepository).save(user);
    }
}
