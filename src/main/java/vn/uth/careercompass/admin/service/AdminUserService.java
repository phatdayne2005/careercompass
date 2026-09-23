package vn.uth.careercompass.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.admin.dto.UserAdminDto;
import vn.uth.careercompass.admin.mapper.UserAdminMapper;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.RoleRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;
import vn.uth.careercompass.kernel.repository.ActivityLogRepository;
import vn.uth.careercompass.kernel.repository.PasswordResetTokenRepository;
import vn.uth.careercompass.mentor.repository.MentorSessionRepository;
import vn.uth.careercompass.portfolio.repository.GitHubProfileRepository;
import vn.uth.careercompass.portfolio.repository.ProjectRepositoryRepository;
import vn.uth.careercompass.roadmap.repository.SkillGapReportRepository;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản trị người dùng cho Admin (P7).
 *
 * <p>Xử lý lỗi: validation ném {@link IllegalArgumentException} với message rõ ràng cho controller
 * hiển thị lại; không bọc {@code catch(Exception)} nuốt lỗi thành RuntimeException chung chung.</p>
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserSkillRepository userSkillRepository;
    private final ActivityLogRepository activityLogRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final MentorSessionRepository mentorSessionRepository;
    private final SkillGapReportRepository skillGapReportRepository;
    private final UserNodeProgressRepository userNodeProgressRepository;
    private final GitHubProfileRepository gitHubProfileRepository;
    private final ProjectRepositoryRepository projectRepositoryRepository;

    public List<UserAdminDto> getAllUsers() {
        return userRepository.findAllWithRoleAndCareerRole().stream()
                .map(UserAdminMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<UserAdminDto> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword).stream()
                .map(UserAdminMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserAdminDto toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + userId));
        requireNotCurrentUser(user, "Bạn không thể tự khóa tài khoản của chính mình!");

        user.setEnabled(!user.getEnabled());
        return UserAdminMapper.toDto(userRepository.save(user));
    }

    @Transactional
    public User changeUserRole(Long userId, String roleNameStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + userId));
        // DEF-001: thiếu đúng dòng này nên quản trị viên tự hạ được vai trò của chính mình
        // xuống STUDENT, mất luôn quyền vào /admin và KHÔNG có đường quay lại qua giao diện —
        // muốn khôi phục phải sửa thẳng trong cơ sở dữ liệu. Hai thao tác nguy hiểm tương tự
        // là tự khoá và tự xoá thì đã được chặn từ trước; đổi vai trò bị bỏ sót.
        requireNotCurrentUser(user, "Bạn không thể tự đổi vai trò của chính mình!");
        RoleName roleName = RoleName.valueOf(roleNameStr.toUpperCase());
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò: " + roleName));
        user.setRole(role);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + userId));
        requireNotCurrentUser(user, "Bạn không thể tự xóa tài khoản của chính mình!");

        // DEF-012: trước đây chỉ dọn users_skills và activity_logs, tức 2 trong 6 bảng có
        // khoá ngoại tới users. Bốn bảng còn lại giữ nguyên tham chiếu nên lệnh xoá vi phạm
        // ràng buộc và THẤT BẠI TRONG IM LẶNG — quản trị viên bấm xoá, trang tải lại, người
        // dùng vẫn còn nguyên trong danh sách.
        //
        // Thứ tự dưới đây đi từ bảng con lên bảng cha; đổi thứ tự là vỡ khoá ngoại.
        xoaHoSoGitHub(user);
        mentorSessionRepository.deleteByUser(user);
        skillGapReportRepository.deleteByUser(user);
        userNodeProgressRepository.deleteByUser(user);
        passwordResetTokenRepository.deleteByUser(user);
        userSkillRepository.deleteByUser(user);
        activityLogRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    /**
     * Dọn hồ sơ E-Portfolio và danh sách repository của nó.
     *
     * <p>github_profiles KHÔNG có khoá ngoại tới users — nó trỏ bằng cột user_id kiểu Long
     * chứ không phải quan hệ @ManyToOne. Nên bảng này không làm lệnh xoá thất bại, mà gây
     * hậu quả khó thấy hơn: hồ sơ trở thành MỒ CÔI và trang chia sẻ công khai /p/{slug} vẫn
     * trả HTTP 200. Tên tài khoản GitHub, danh sách repository, mô tả và phần tóm tắt do AI
     * sinh của người đã bị xoá vẫn hiển thị cho bất kỳ ai có đường liên kết.
     */
    private void xoaHoSoGitHub(User user) {
        gitHubProfileRepository.findByUserId(user.getId()).ifPresent(hoSo -> {
            projectRepositoryRepository.deleteByGithubProfileId(hoSo.getId());
            gitHubProfileRepository.delete(hoSo);
        });
    }

    /** Chặn admin thao tác lên chính tài khoản đang đăng nhập (tự khoá / tự xoá). */
    private void requireNotCurrentUser(User user, String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && user.getEmail().equalsIgnoreCase(auth.getName())) {
            throw new IllegalArgumentException(message);
        }
    }
}
