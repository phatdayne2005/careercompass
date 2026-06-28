package vn.uth.careercompass.admin.service;
 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.admin.dto.UserAdminDto;
import vn.uth.careercompass.admin.mapper.UserAdminMapper;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.RoleRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;
 
import java.util.List;
import java.util.stream.Collectors;
 
@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
 
    public List<UserAdminDto> getAllUsers() {
        try {
            return userRepository.findAllWithRoleAndCareerRole().stream()
                    .map(UserAdminMapper::toDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách người dùng: " + e.getMessage());
            throw new RuntimeException("Lấy danh sách người dùng thất bại!", e);
        }
    }
 
    @Transactional
    public UserAdminDto toggleUserStatus(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + userId));
            
            // Ngăn chặn admin tự khóa chính mình
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                String currentUsername = auth.getName();
                if (user.getEmail().equalsIgnoreCase(currentUsername)) {
                    throw new IllegalArgumentException("Bạn không thể tự khóa tài khoản của chính mình!");
                }
            }

            user.setEnabled(!user.getEnabled());
            User savedUser = userRepository.save(user);
            return UserAdminMapper.toDto(savedUser);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Lỗi khi đảo trạng thái hoạt động của người dùng ID " + userId + ": " + e.getMessage());
            throw new RuntimeException("Cập nhật trạng thái hoạt động người dùng thất bại!", e);
        }
    }
 
    @Transactional
    public User changeUserRole(Long userId, String roleNameStr) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng có ID: " + userId));
            RoleName roleName = RoleName.valueOf(roleNameStr.toUpperCase());
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò: " + roleName));
            user.setRole(role);
            return userRepository.save(user);
        } catch (Exception e) {
            System.err.println("Lỗi khi thay đổi vai trò người dùng ID " + userId + ": " + e.getMessage());
            throw new RuntimeException("Thay đổi vai trò người dùng thất bại!", e);
        }
    }
}
