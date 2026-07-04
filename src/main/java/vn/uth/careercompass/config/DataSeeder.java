package vn.uth.careercompass.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.uth.careercompass.kernel.entity.AuthProvider;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.RoleRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;

/**
 * Seed nền tảng KERNEL (P1): 3 {@link Role} + 3 tài khoản mẫu (admin/student/counselor).
 *
 * <p>Idempotent — chỉ tạo khi chưa tồn tại, chạy đi chạy lại vô hại.
 * Chạy TRƯỚC các seeder của gói khác nhờ {@link Order}(1) (Role phải có trước khi tạo User).</p>
 *
 * <p><b>Ranh giới trách nhiệm:</b> seeder này CHỈ lo Role + account. Việc seed
 * roadmap/template/skill/career-role thuộc gói P7 — xem
 * {@code admin/config/TemplateDataSeeder}. KHÔNG nhét logic gói khác vào kernel.</p>
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    String adminEmail;
    @Value("${app.admin.password}")
    String adminPassword;
    @Value("${app.student.email}")
    String studentEmail;
    @Value("${app.student.password}")
    String studentPassword;
    @Value("${app.counselor.email}")
    String counselorEmail;
    @Value("${app.counselor.password}")
    String counselorPassword;

    @Override
    public void run(String... args) {
        seedRoles();
        seedUser(RoleName.ADMIN, "System Admin", adminEmail, adminPassword);
        seedUser(RoleName.STUDENT, "System Student", studentEmail, studentPassword);
        seedUser(RoleName.COUNSELOR, "System Counselor", counselorEmail, counselorPassword);
    }

    private void seedRoles() {
        for (RoleName name : RoleName.values()) {
            if (!roleRepository.existsByName(name)) {
                roleRepository.save(Role.builder()
                        .name(name)
                        .description(describe(name))
                        .build());
            }
        }
    }

    private String describe(RoleName name) {
        return switch (name) {
            case ADMIN -> "Quản trị hệ thống";
            case COUNSELOR -> "Cố vấn hướng nghiệp";
            case STUDENT -> "Sinh viên";
        };
    }

    /**
     * Tạo 1 tài khoản mẫu LOCAL nếu email chưa tồn tại.
     * Student KHÔNG gán sẵn careerRole — sinh viên tự chọn Target Role qua luồng Onboarding (P2).
     */
    private void seedUser(RoleName roleName, String fullName, String email, String password) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Chưa seed Role " + roleName));
        userRepository.save(User.builder()
                .email(email)
                .fullName(fullName)
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .authProvider(AuthProvider.LOCAL)
                .enabled(true)
                .build());
    }
}
