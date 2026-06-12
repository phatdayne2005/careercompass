package vn.uth.careercompass.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.repository.RoleRepository;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        seedRoles();
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
}
