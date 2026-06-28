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
import vn.uth.careercompass.admin.entity.*;
import vn.uth.careercompass.admin.repository.*;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Thêm các repository của P7
    private final SkillRepository skillRepository;
    private final SkillNodeRepository skillNodeRepository;
    private final SkillTreeTemplateRepository skillTreeTemplateRepository;
    private final CareerRoleRepository careerRoleRepository;
    private final LearningResourceRepository learningResourceRepository;

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
    public void run(String... args) throws Exception {
        seedRoles();
        seedSkills(); // goi seed skills truoc
        CareerRole defaultRole = seedRoadmaps(); // goi seed lo trinh mau va lay ra vai tro mac dinh

        // Seed cac tai khoai mau
        seedUser(RoleName.ADMIN, "System Admin", adminEmail, adminPassword, null);
        seedUser(RoleName.STUDENT, "System Student", studentEmail, studentPassword, defaultRole);
        // gan vai tro mac dinh cho sv
        seedUser(RoleName.COUNSELOR, "System Counselor", counselorEmail, counselorPassword, null);
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

    private String describe(RoleName name) {
        return switch (name) {
            case ADMIN -> "Quản trị hệ thống";
            case COUNSELOR -> "Cố vấn hướng nghiệp";
            case STUDENT -> "Sinh viên";
        };
    }

    private void seedUser(RoleName roleName, String fullName, String email, String password, CareerRole targetRole) {
        if (userRepository.existsByEmail(email))
            return;
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Chưa seed Role " + roleName));
        userRepository.save(User.builder()
                .email(email).fullName(fullName)
                .passwordHash(passwordEncoder.encode(password))
                .role(role).authProvider(AuthProvider.LOCAL).enabled(true)
                .careerRole(targetRole) // them careerrole vao
                .build());
    }

    private void seedSkills() {
        if (skillRepository.count() == 0) {
            List<Skill> skills = List.of(
                    Skill.builder().name("Git").category("Tools").build(),
                    Skill.builder().name("Java Core").category("Language").build(),
                    Skill.builder().name("SQL").category("Database").build(),
                    Skill.builder().name("Spring Boot").category("Framework").build(),
                    Skill.builder().name("Spring Security").category("Security").build());
            skillRepository.saveAll(skills);
        }
    }

    private CareerRole seedRoadmaps() {
        // 1. Neu da co CareerRole thi lay cai dau tien ra tra ve (tranh lap du lieu)
        if (careerRoleRepository.count() > 0) {
            return careerRoleRepository.findAll().get(0);
        }

        // 2. Seed CareerRole
        CareerRole backendRole = CareerRole.builder()
                .name("Java Backend Developer")
                .description(
                        "Phat trien cac ung dung phia may chu bang ngon ngu Java va he sinh thai Spring Framework.")
                .expectedSalaryRange("15,000,000 -35,000,000 VND")
                .marketDemand("High")
                .build();
        backendRole = careerRoleRepository.save(backendRole);

        // 3. Seed SkillTreeTemplate gan voi CareerRole
        SkillTreeTemplate template = SkillTreeTemplate.builder()
                .name("Lo trinh Java Backend Developer chuan")
                .description("Khung chuong trinh dao tao tu co ban den nang cao cho lap trinh vien Java Backend.")
                .careerRole(backendRole)
                .build();
        template = skillTreeTemplateRepository.save(template);

        // Lay ra cac Skill da seed
        Skill gitSkill = skillRepository.findByName("Git").orElseThrow();
        Skill javaSkill = skillRepository.findByName("Java Core").orElseThrow();
        Skill sqlSkill = skillRepository.findByName("SQL").orElseThrow();
        Skill springBootSkill = skillRepository.findByName("Spring Boot").orElseThrow();
        Skill springSecuritySkill = skillRepository.findByName("Spring Security").orElseThrow();

        // 4. Seed cac SkillNode (Tu co ban den nang cao)
        // Seed cac SkillNode (Cap đo 1 - khong co cha)
        SkillNode nodeGit = SkillNode.builder()
                .skillTreeTemplate(template)
                .skill(gitSkill)
                .level(1)
                .parentNode(null)
                .build();

        SkillNode nodeJava = SkillNode.builder()
                .skillTreeTemplate(template)
                .skill(javaSkill)
                .level(1)
                .parentNode(null)
                .build();

        SkillNode nodeSQL = SkillNode.builder()
                .skillTreeTemplate(template)
                .skill(sqlSkill)
                .level(1)
                .parentNode(null)
                .build();

        nodeGit = skillNodeRepository.save(nodeGit);
        nodeJava = skillNodeRepository.save(nodeJava);
        nodeSQL = skillNodeRepository.save(nodeSQL);
        // Seed cac SkillNode phu thuoc (Cap đo 2 - cha la Java Core)
        SkillNode nodeSpringBoot = SkillNode.builder()
                .skillTreeTemplate(template)
                .skill(springBootSkill)
                .level(2)
                .parentNode(nodeJava)
                .build();
        nodeSpringBoot = skillNodeRepository.save(nodeSpringBoot);

        // Seed cac SkillNode phu thuoc (Cap đo 3 - cha la Spring Boot)
        SkillNode nodeSpringSecurity = SkillNode.builder()
                .skillTreeTemplate(template)
                .skill(springSecuritySkill)
                .level(3)
                .parentNode(nodeSpringBoot)
                .build();
        nodeSpringSecurity = skillNodeRepository.save(nodeSpringSecurity);

        // 5. Seed cac LearningResource cho tung SkillNode
        LearningResource gitResource1 = LearningResource.builder()
                .skillNode(nodeGit)
                .title("Huong dan Git co ban cho nguoi moi")
                .url("https://git-scm.com/book/en/v2")
                .resourceType("DOCUMENTATION")
                .description("Tai lieu chinh thuc cua Git, rat chi tiet va de hieu.")
                .build();

        LearningResource gitResource2 = LearningResource.builder()
                .skillNode(nodeGit)
                .title("Git Cheat Sheet (Github Education)")
                .url("https://education.github.com/git-cheat-sheet-education.pdf")
                .resourceType("DOCUMENTATION")
                .description("Bang tra cuu nhanh cac cau lenh Git thong dung.")
                .build();

        LearningResource javaResource1 = LearningResource.builder()
                .skillNode(nodeJava)
                .title("Java Core Tutorial for Beginners (W3Schools)")
                .url("https://www.w3schools.com/java/")
                .resourceType("DOCUMENTATION")
                .description("Tai lieu tu hoc ngon ngu lap trinh Java tu co ban.")
                .build();

        LearningResource javaResource2 = LearningResource.builder()
                .skillNode(nodeJava)
                .title("Java Core Basics & Practices (Youtube)")
                .url("https://www.youtube.com/watch?v=grEKMHGYyns")
                .resourceType("VIDEO")
                .description("Video huong dan chi tiet ve lap trinh huong doi tuong trong Java.")
                .build();

        learningResourceRepository.save(gitResource1);
        learningResourceRepository.save(gitResource2);
        learningResourceRepository.save(javaResource1);
        learningResourceRepository.save(javaResource2);
        return backendRole;

    }

}
