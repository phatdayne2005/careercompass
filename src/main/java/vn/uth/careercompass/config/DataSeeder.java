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
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import vn.uth.careercompass.config.dto.RoadmapJson;

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

    private ExecutorService executorService = Executors.newFixedThreadPool(5, r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

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
        if (learningResourceRepository.count() <= 4) {
            System.out.println("🔄 Phát hiện dữ liệu cũ chưa được cào tài liệu học tập. Đang tự động làm sạch DB...");
            
            // 1. Gỡ tham chiếu CareerRole của User
            userRepository.findAll().forEach(u -> {
                if (u.getCareerRole() != null) {
                    u.setCareerRole(null);
                    userRepository.save(u);
                }
            });
            
            // 2. Xóa các bảng liên quan đến Lộ trình
            learningResourceRepository.deleteAll();
            skillNodeRepository.deleteAll();
            skillTreeTemplateRepository.deleteAll();
            careerRoleRepository.deleteAll();
            
            System.out.println("✅ Đã làm sạch DB cũ thành công. Tiến hành seed lại toàn bộ...");
        }

        seedRoles();
        seedSkills(); // goi seed skills truoc
        CareerRole defaultRole = seedRoadmaps(); // goi seed lo trinh mau va lay ra vai tro mac dinh
        seedRoadmapsFromFolder();

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
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            if (user.getCareerRole() == null && targetRole != null) {
                user.setCareerRole(targetRole);
                userRepository.save(user);
            }
            return;
        }
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
                .targetRoleId(backendRole.getId())
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
                .template(template)
                .skill(gitSkill)
                .title(gitSkill.getName())
                .requiredLevel(1)
                .parent(null)
                .build();

        SkillNode nodeJava = SkillNode.builder()
                .template(template)
                .skill(javaSkill)
                .title(javaSkill.getName())
                .requiredLevel(1)
                .parent(null)
                .build();

        SkillNode nodeSQL = SkillNode.builder()
                .template(template)
                .skill(sqlSkill)
                .title(sqlSkill.getName())
                .requiredLevel(1)
                .parent(null)
                .build();

        nodeGit = skillNodeRepository.save(nodeGit);
        nodeJava = skillNodeRepository.save(nodeJava);
        nodeSQL = skillNodeRepository.save(nodeSQL);
        // Seed cac SkillNode phu thuoc (Cap đo 2 - cha la Java Core)
        SkillNode nodeSpringBoot = SkillNode.builder()
                .template(template)
                .skill(springBootSkill)
                .title(springBootSkill.getName())
                .requiredLevel(2)
                .parent(nodeJava)
                .build();
        nodeSpringBoot = skillNodeRepository.save(nodeSpringBoot);

        // Seed cac SkillNode phu thuoc (Cap đo 3 - cha la Spring Boot)
        SkillNode nodeSpringSecurity = SkillNode.builder()
                .template(template)
                .skill(springSecuritySkill)
                .title(springSecuritySkill.getName())
                .requiredLevel(3)
                .parent(nodeSpringBoot)
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

    private void seedRoadmapsFromFolder() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:data/*.json");

            ObjectMapper mapper = new ObjectMapper();

            for (Resource resource : resources) {
                try {
                    RoadmapJson data = mapper.readValue(resource.getInputStream(), RoadmapJson.class);
                    if (data.getTitle() == null || data.getTitle().getPage() == null) {
                        continue;
                    }

                    String roleName = data.getTitle().getPage();

                    // Check if CareerRole is already seeded
                    if (careerRoleRepository.findByName(roleName).isPresent()) {
                        System.out.println("⏭️ Lộ trình " + roleName + " đã tồn tại, bỏ qua.");
                        continue;
                    }

                    CareerRole careerRole = careerRoleRepository.save(
                            CareerRole.builder()
                                    .name(roleName)
                                    .description(data.getDescription())
                                    .expectedSalaryRange("15,000,000 - 40,000,000 VND")
                                    .marketDemand("High")
                                    .build()
                    );

                    SkillTreeTemplate template = skillTreeTemplateRepository.save(
                            SkillTreeTemplate.builder()
                                    .name("Lộ trình " + roleName)
                                    .description("Khung chương trình đào tạo chuẩn cào từ roadmap.sh")
                                    .targetRoleId(careerRole.getId())
                                    .build()
                    );

                    Map<String, RoadmapJson.NodeInfo> nodesMap = new HashMap<>();
                    Map<String, List<String>> incomingEdgesMap = new HashMap<>();

                    for (RoadmapJson.NodeInfo node : data.getNodes()) {
                        nodesMap.put(node.getId(), node);
                    }

                    for (RoadmapJson.EdgeInfo edge : data.getEdges()) {
                        incomingEdgesMap.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>()).add(edge.getSource());
                    }

                    Map<String, SkillNode> createdNodesMap = new HashMap<>();

                    for (RoadmapJson.NodeInfo node : data.getNodes()) {
                        if ("topic".equals(node.getType()) || "subtopic".equals(node.getType())) {
                            String skillName = node.getData().getLabel();
                            if (skillName == null || skillName.trim().isEmpty()) continue;

                            Skill skill = skillRepository.findByName(skillName)
                                    .orElseGet(() -> skillRepository.save(
                                            Skill.builder()
                                                    .name(skillName)
                                                    .category(node.getType().toUpperCase())
                                                    .build()
                                    ));

                            SkillNode skillNode = SkillNode.builder()
                                    .template(template)
                                    .skill(skill)
                                    .title(skill.getName())
                                    .requiredLevel(1)
                                    .build();

                            SkillNode savedNode = skillNodeRepository.save(skillNode);
                            createdNodesMap.put(node.getId(), savedNode);

                            String roadmapSlug = data.getSlug();
                            if (roadmapSlug == null || roadmapSlug.trim().isEmpty()) {
                                roadmapSlug = resource.getFilename().replace(".json", "");
                            }
                            seedResourcesForNode(roadmapSlug, node.getId(), savedNode);
                        }
                    }

                    for (Map.Entry<String, SkillNode> entry : createdNodesMap.entrySet()) {
                        String nodeId = entry.getKey();
                        SkillNode currentSkillNode = entry.getValue();

                        SkillNode parentSkillNode = findParentTopicNode(nodeId, incomingEdgesMap, nodesMap, createdNodesMap);
                        if (parentSkillNode != null) {
                            currentSkillNode.setParent(parentSkillNode);
                            currentSkillNode.setRequiredLevel(parentSkillNode.getRequiredLevel() + 1);
                            skillNodeRepository.save(currentSkillNode);
                        }
                    }

                    System.out.println("🚀 Đã seed thành công lộ trình " + roleName + " với " + createdNodesMap.size() + " kỹ năng!");

                } catch (Exception e) {
                    System.err.println("Lỗi khi seed file " + resource.getFilename() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi quét thư mục data/*.json: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private SkillNode findParentTopicNode(
            String nodeId,
            Map<String, List<String>> incomingEdgesMap,
            Map<String, RoadmapJson.NodeInfo> nodesMap,
            Map<String, SkillNode> createdNodesMap) {

        List<String> parents = incomingEdgesMap.get(nodeId);
        if (parents == null || parents.isEmpty()) {
            return null;
        }

        for (String parentId : parents) {
            RoadmapJson.NodeInfo parentNode = nodesMap.get(parentId);
            if (parentNode == null) continue;

            if (("topic".equals(parentNode.getType()) || "subtopic".equals(parentNode.getType()))
                    && createdNodesMap.containsKey(parentId)) {
                return createdNodesMap.get(parentId);
            }

            SkillNode ancestor = findParentTopicNode(parentId, incomingEdgesMap, nodesMap, createdNodesMap);
            if (ancestor != null) {
                return ancestor;
            }
        }
        return null;
    }

    private void seedResourcesForNode(String roadmapSlug, String nodeId, SkillNode skillNode) {
        if (roadmapSlug == null || roadmapSlug.trim().isEmpty()) {
            return;
        }

        executorService.submit(() -> {
            try {
                String cleanSlug = roadmapSlug.toLowerCase().trim();
                String urlString = "https://roadmap.sh/" + cleanSlug + "/" + nodeId + ".json";

                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(conn.getInputStream());

                    JsonNode resourcesNode = rootNode.get("resources");
                    if (resourcesNode != null && resourcesNode.isArray()) {
                        List<LearningResource> resourcesToSave = new ArrayList<>();
                        for (JsonNode resNode : resourcesNode) {
                            String type = resNode.has("type") ? resNode.get("type").asText() : "DOCUMENTATION";
                            String title = resNode.has("title") ? resNode.get("title").asText() : "";
                            String resUrl = resNode.has("url") ? resNode.get("url").asText() : "";

                            if (title.isEmpty() || resUrl.isEmpty()) continue;
                            if ("roadmap".equalsIgnoreCase(type)) continue;

                            LearningResource learningResource = LearningResource.builder()
                                    .skillNode(skillNode)
                                    .title(title)
                                    .url(resUrl)
                                    .resourceType(type.toUpperCase())
                                    .description("Tài liệu học tập cào tự động từ roadmap.sh")
                                    .build();
                            resourcesToSave.add(learningResource);
                        }

                        if (!resourcesToSave.isEmpty()) {
                            learningResourceRepository.saveAll(resourcesToSave);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("⚠️ Lỗi cào tài liệu cho Node ID: " + nodeId + " trong lộ trình: " + roadmapSlug + " -> " + e.getMessage());
            }
        });
    }

}
