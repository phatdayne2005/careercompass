package vn.uth.careercompass.profile.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.uth.careercompass.kernel.entity.AuthProvider;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.service.UserProfileService;

import java.util.List;

@Service
public class ProfileService {

    private final UserProfileService userProfileService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileService(UserProfileService userProfileService,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userProfileService = userProfileService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getProfile(User user) {
        return user;
    }

    public void updateGithub(User user, String githubUsername) {
        userProfileService.setGithub(user, githubUsername);
    }

    public void updateSkills(User user, List<Long> skillIds) {
        userProfileService.replaceSkills(user, skillIds);
    }

    /** Đổi họ tên (tự do). */
    @Transactional
    public void updateName(User user, String fullName) {
        user.setFullName(fullName.trim());
        userRepository.save(user);
    }

    /**
     * Đổi email — chỉ tài khoản LOCAL, kiểm tra trùng. Sau khi đổi controller sẽ buộc đăng nhập lại
     * (email là username của Spring Security).
     */
    @Transactional
    public void updateEmail(User user, String newEmail) {
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("Tài khoản đăng nhập bằng Google không thể đổi email tại đây.");
        }
        String email = newEmail == null ? "" : newEmail.trim().toLowerCase();
        if (email.isBlank()) {
            throw new IllegalStateException("Email không được để trống.");
        }
        if (email.equalsIgnoreCase(user.getEmail())) {
            return; // không đổi gì
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("Email này đã được sử dụng bởi tài khoản khác.");
        }
        user.setEmail(email);
        userRepository.save(user);
    }

    /** Đổi mật khẩu — xác thực mật khẩu hiện tại trước. Chỉ tài khoản LOCAL. */
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            throw new IllegalStateException("Tài khoản Google không dùng mật khẩu tại đây.");
        }
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalStateException("Mật khẩu hiện tại không đúng.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}