package vn.uth.careercompass.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.uth.careercompass.kernel.entity.AuthProvider;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.RoleRepository;
import vn.uth.careercompass.kernel.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}") String adminEmail;
    @Value("${app.admin.password}") String adminPassword;
    @Value("${app.student.email}") String studentEmail;
    @Value("${app.student.password}") String studentPassword;
    @Value("${app.counselor.email}") String counselorEmail;
    @Value("${app.counselor.password}") String counselorPassword;
    @Override
    public void run(String... args) throws Exception {
        seedRoles();
        seedUser(RoleName.ADMIN, "System Admin", adminEmail, adminPassword);
        seedUser(RoleName.STUDENT, "System Student", studentEmail, studentPassword);
        seedUser(RoleName.COUNSELOR, "System Counselor", counselorEmail, counselorPassword);
    }

    private void seedRoles() {
        for (RoleName name : RoleName.values()) {
            if (!roleRepository.existsByName(name)) {
                Role newRole = Role.builder()
                        .name(name)
                        .description(describe(name))
                        .build();
                roleRepository.save(newRole);
            }
        }
    }

    private String describe(RoleName name){
        return switch (name){
            case ADMIN -> "Quản trị hệ thống";
            case COUNSELOR -> "Cố vấn hướng nghiệp";
            case STUDENT -> "Sinh viên";
        };
    }

    private void seedUser(RoleName roleName, String fullName, String email, String password) {
        if (userRepository.existsByEmail(email)) return;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Chưa seed Role " + roleName));
        userRepository.save(User.builder()
                .email(email).fullName(fullName)
                .passwordHash(passwordEncoder.encode(password))
                .role(role).authProvider(AuthProvider.LOCAL).enabled(true)
                .build());
    }
}
