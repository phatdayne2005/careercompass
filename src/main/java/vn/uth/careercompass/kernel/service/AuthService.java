package vn.uth.careercompass.kernel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.kernel.entity.*;
import vn.uth.careercompass.kernel.exception.EmailAlreadyExistsException;
import vn.uth.careercompass.kernel.repository.RoleRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public void register(String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email đã được đăng ký");
        }
        Role studentRole = roleRepository.findByName(RoleName.STUDENT).orElseThrow(() -> new IllegalStateException("Chưa seed Role Student"));

        String passwordHash = passwordEncoder.encode(password);

        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .passwordHash(passwordHash)
                .role(studentRole)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);

        activityLogService.log(user, KernelActivityLogType.REGISTER, "Đăng ký tài khoản mới");
    }
}
