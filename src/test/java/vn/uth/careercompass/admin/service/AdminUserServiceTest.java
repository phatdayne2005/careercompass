package vn.uth.careercompass.admin.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.uth.careercompass.admin.dto.UserAdminDto;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.ActivityLogRepository;
import vn.uth.careercompass.kernel.repository.RoleRepository;
import vn.uth.careercompass.kernel.repository.PasswordResetTokenRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.mentor.repository.MentorSessionRepository;
import vn.uth.careercompass.portfolio.entity.GitHubProfile;
import vn.uth.careercompass.portfolio.repository.GitHubProfileRepository;
import vn.uth.careercompass.portfolio.repository.ProjectRepositoryRepository;
import vn.uth.careercompass.roadmap.repository.SkillGapReportRepository;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link AdminUserService}.
 *
 * <p>ĐIỂM MỚI của file này: service đọc {@link SecurityContextHolder} (context bảo mật của
 * Spring Security) để biết admin ĐANG đăng nhập là ai, nhằm CHẶN admin tự khóa/tự xóa chính mình.
 * Trong unit test không có Spring Security thật, nên ta tự "đặt" một Authentication vào context
 * bằng {@link SecurityContextHolder}, rồi {@link #tearDown()} dọn sạch sau mỗi test để tránh
 * rò rỉ trạng thái sang test khác (context là biến static toàn cục theo thread).
 */
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private ActivityLogRepository activityLogRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private MentorSessionRepository mentorSessionRepository;
    @Mock
    private SkillGapReportRepository skillGapReportRepository;
    @Mock
    private UserNodeProgressRepository userNodeProgressRepository;
    @Mock
    private GitHubProfileRepository gitHubProfileRepository;
    @Mock
    private ProjectRepositoryRepository projectRepositoryRepository;

    @InjectMocks
    private AdminUserService adminUserService;

    // Dọn SecurityContext sau MỖI test. WHY: nếu 1 test set Authentication rồi không xóa,
    // test sau (chạy chung thread) sẽ "thấy" auth cũ -> gây lỗi giả (flaky test).
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Helper tạo User tối thiểu đủ cho mapper (role + enabled + email). */
    private User buildUser(Long id, String email, boolean enabled) {
        Role role = Role.builder().name(RoleName.STUDENT).build();
        return User.builder()
                .id(id)
                .fullName("Nguyen Van " + id)
                .email(email)
                .role(role)
                .enabled(enabled)
                .build();
    }

    /** Giả lập admin đang đăng nhập với email cho trước (đặt vào SecurityContext). */
    private void loginAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
    }

    // ============================================================================
    // getAllUsers()
    // ============================================================================
    @Test
    void getAllUsers_mapsEntitiesToDto() {
        // Given: repo trả về 2 user
        List<User> users = List.of(
                buildUser(1L, "a@uth.edu.vn", true),
                buildUser(2L, "b@uth.edu.vn", false));
        when(userRepository.findAllWithRoleAndCareerRole()).thenReturn(users);

        // When
        List<UserAdminDto> result = adminUserService.getAllUsers();

        // Then: đúng số lượng + mapper chuyển đúng field (email, roleName, enabled)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("a@uth.edu.vn");
        assertThat(result.get(0).getRoleName()).isEqualTo("STUDENT");
        assertThat(result.get(1).getEnabled()).isFalse();
    }

    // ============================================================================
    // searchUsers(keyword)
    // ============================================================================
    @Test
    void searchUsers_delegatesToRepositoryAndMaps() {
        when(userRepository.searchUsers("nguyen")).thenReturn(List.of(buildUser(1L, "a@uth.edu.vn", true)));

        List<UserAdminDto> result = adminUserService.searchUsers("nguyen");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFullName()).isEqualTo("Nguyen Van 1");
    }

    // ============================================================================
    // toggleUserStatus(userId)
    // ============================================================================
    @Test
    void toggleUserStatus_whenNotCurrentUser_flipsEnabledAndSaves() {
        // Given: user đang enabled=true, admin đăng nhập là NGƯỜI KHÁC
        User user = buildUser(1L, "target@uth.edu.vn", true);
        loginAs("admin@uth.edu.vn"); // khác email target -> được phép
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        // save trả lại chính user (mapper đọc trạng thái sau khi toggle)
        when(userRepository.save(user)).thenReturn(user);

        // When
        UserAdminDto dto = adminUserService.toggleUserStatus(1L);

        // Then: enabled bị lật true -> false
        assertThat(user.getEnabled()).isFalse();
        assertThat(dto.getEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void toggleUserStatus_whenNoAuthentication_stillAllowed() {
        // Given: KHÔNG có ai đăng nhập (auth == null) -> guard requireNotCurrentUser bỏ qua.
        // WHY test nhánh này: điều kiện guard là "auth != null && ...", nên auth null phải cho qua.
        User user = buildUser(1L, "target@uth.edu.vn", false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserAdminDto dto = adminUserService.toggleUserStatus(1L);

        assertThat(dto.getEnabled()).isTrue(); // false -> true
    }

    @Test
    void toggleUserStatus_whenTargetIsCurrentUser_throwsAndDoesNotSave() {
        // Given: admin cố khóa CHÍNH MÌNH (cùng email) -> phải bị chặn
        User self = buildUser(1L, "admin@uth.edu.vn", true);
        loginAs("admin@uth.edu.vn");
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> adminUserService.toggleUserStatus(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bạn không thể tự khóa tài khoản của chính mình!");

        // Không được đổi trạng thái, không lưu
        assertThat(self.getEnabled()).isTrue();
        verify(userRepository, never()).save(any());
    }

    @Test
    void toggleUserStatus_whenUserNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.toggleUserStatus(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy người dùng có ID: 99");

        verify(userRepository, never()).save(any());
    }

    // ============================================================================
    // changeUserRole(userId, roleNameStr)
    // ============================================================================
    @Test
    void changeUserRole_whenValid_setsRoleAndSaves() {
        // Given: user tồn tại, role COUNSELOR đã seed. roleNameStr viết thường -> service tự toUpperCase.
        User user = buildUser(1L, "a@uth.edu.vn", true);
        Role counselorRole = Role.builder().name(RoleName.COUNSELOR).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.COUNSELOR)).thenReturn(Optional.of(counselorRole));
        when(userRepository.save(user)).thenReturn(user);

        User result = adminUserService.changeUserRole(1L, "counselor");

        assertThat(result.getRole()).isEqualTo(counselorRole);
        verify(userRepository).save(user);
    }

    @Test
    void changeUserRole_whenRoleNameInvalid_throwsIllegalArgument() {
        // Given: user tồn tại nhưng chuỗi role không map được enum -> RoleName.valueOf ném IllegalArgumentException.
        // Không stub roleRepository vì luồng ném lỗi TRƯỚC khi tra role -> tránh stub thừa.
        User user = buildUser(1L, "a@uth.edu.vn", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> adminUserService.changeUserRole(1L, "SUPERUSER"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUserRole_whenRoleNotSeeded_throws() {
        // Given: chuỗi hợp lệ (ADMIN) nhưng DB chưa có row Role tương ứng.
        User user = buildUser(1L, "a@uth.edu.vn", true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.changeUserRole(1L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy vai trò: ADMIN");

        verify(userRepository, never()).save(any());
    }

    @Test
    void changeUserRole_whenUserNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.changeUserRole(99L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy người dùng có ID: 99");
    }

    /**
     * Test hồi quy cho DEF-001.
     *
     * <p>Trước khi sửa, quản trị viên tự hạ được vai trò của chính mình xuống STUDENT rồi
     * mất luôn quyền vào {@code /admin}, và không có đường quay lại qua giao diện — phải
     * sửa thẳng trong cơ sở dữ liệu. Kịch bản giao diện {@code TC-ADM-003} đã tái hiện
     * đúng tình huống này: sau khi chạy, tài khoản admin mang vai trò STUDENT thật.
     *
     * <p>Hai thao tác nguy hiểm tương tự là tự khoá và tự xoá đã được chặn từ trước; riêng
     * đổi vai trò bị bỏ sót.
     */
    @Test
    void changeUserRole_whenTargetIsCurrentUser_throwsAndDoesNotSave() {
        User self = buildUser(1L, "admin@uth.edu.vn", true);
        loginAs("admin@uth.edu.vn");
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> adminUserService.changeUserRole(1L, "student"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bạn không thể tự đổi vai trò của chính mình!");

        verify(userRepository, never()).save(any(User.class));
    }

    // ============================================================================
    // deleteUser(userId)
    // ============================================================================
    @Test
    void deleteUser_donDuSauBangThamChieu() {
        // DEF-012: sáu bảng có khoá ngoại tới users. Bỏ sót một bảng là lệnh xoá vi phạm
        // ràng buộc và thất bại TRONG IM LẶNG — quản trị viên bấm xoá, trang tải lại, người
        // dùng vẫn nguyên trong danh sách. Trước khi sửa chỉ có hai dòng đầu.
        User user = buildUser(1L, "target@uth.edu.vn", true);
        loginAs("admin@uth.edu.vn");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        adminUserService.deleteUser(1L);

        verify(userSkillRepository).deleteByUser(user);
        verify(activityLogRepository).deleteByUser(user);
        verify(passwordResetTokenRepository).deleteByUser(user);
        verify(mentorSessionRepository).deleteByUser(user);
        verify(skillGapReportRepository).deleteByUser(user);
        verify(userNodeProgressRepository).deleteByUser(user);
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_xoaBangConTruocBangCha() {
        // Thứ tự là phần quan trọng ngang với việc gọi đủ: mọi bảng con phải sạch TRƯỚC khi
        // xoá dòng trong users, nếu không cơ sở dữ liệu từ chối lệnh cuối.
        User user = buildUser(1L, "target@uth.edu.vn", true);
        loginAs("admin@uth.edu.vn");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        adminUserService.deleteUser(1L);

        var thuTu = inOrder(mentorSessionRepository, skillGapReportRepository,
                userNodeProgressRepository, passwordResetTokenRepository,
                userSkillRepository, activityLogRepository, userRepository);
        thuTu.verify(mentorSessionRepository).deleteByUser(user);
        thuTu.verify(skillGapReportRepository).deleteByUser(user);
        thuTu.verify(userNodeProgressRepository).deleteByUser(user);
        thuTu.verify(passwordResetTokenRepository).deleteByUser(user);
        thuTu.verify(userSkillRepository).deleteByUser(user);
        thuTu.verify(activityLogRepository).deleteByUser(user);
        thuTu.verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_xoaLuonHoSoGitHubDeTrangCongKhaiKhongConSong() {
        // github_profiles KHÔNG có khoá ngoại tới users (cột user_id kiểu Long), nên nó
        // không làm lệnh xoá thất bại — nó gây hậu quả khó thấy hơn: hồ sơ thành mồ côi và
        // /p/{slug} vẫn trả HTTP 200, phơi tên GitHub cùng toàn bộ repository của người đã
        // bị xoá cho bất kỳ ai có đường liên kết.
        User user = buildUser(1L, "target@uth.edu.vn", true);
        loginAs("admin@uth.edu.vn");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        GitHubProfile hoSo = GitHubProfile.builder()
                .id(7L).userId(1L).githubUsername("phatdayne").slug("phatdayne-abc123").build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(hoSo));

        adminUserService.deleteUser(1L);

        var thuTu = inOrder(projectRepositoryRepository, gitHubProfileRepository, userRepository);
        // Danh sách repository là bảng con của github_profiles nên phải sạch trước.
        thuTu.verify(projectRepositoryRepository).deleteByGithubProfileId(7L);
        thuTu.verify(gitHubProfileRepository).delete(hoSo);
        thuTu.verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_khongCoHoSoGitHub_vanXoaBinhThuong() {
        User user = buildUser(1L, "target@uth.edu.vn", true);
        loginAs("admin@uth.edu.vn");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        adminUserService.deleteUser(1L);

        verify(projectRepositoryRepository, never()).deleteByGithubProfileId(any());
        verify(gitHubProfileRepository, never()).delete(any());
        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_whenTargetIsCurrentUser_throwsAndDeletesNothing() {
        User self = buildUser(1L, "admin@uth.edu.vn", true);
        loginAs("admin@uth.edu.vn");
        when(userRepository.findById(1L)).thenReturn(Optional.of(self));

        assertThatThrownBy(() -> adminUserService.deleteUser(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bạn không thể tự xóa tài khoản của chính mình!");

        // Tuyệt đối không được xóa gì — kiểm đủ cả sáu bảng lẫn hồ sơ GitHub.
        verify(userSkillRepository, never()).deleteByUser(any());
        verify(activityLogRepository, never()).deleteByUser(any());
        verify(passwordResetTokenRepository, never()).deleteByUser(any());
        verify(mentorSessionRepository, never()).deleteByUser(any());
        verify(skillGapReportRepository, never()).deleteByUser(any());
        verify(userNodeProgressRepository, never()).deleteByUser(any());
        verify(gitHubProfileRepository, never()).delete(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteUser_whenUserNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.deleteUser(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy người dùng có ID: 99");

        verify(userRepository, never()).delete(any());
    }
}
