package vn.uth.careercompass.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import vn.uth.careercompass.admin.dto.RoadmapJson;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.entity.LearningResource;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.LearningResourceRepository;
import vn.uth.careercompass.admin.repository.SkillNodeRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.admin.repository.SkillTreeTemplateRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Seed dữ liệu SkillTree của gói P7 (Admin/Counselor): CareerRole + SkillTreeTemplate + SkillNode
 * + LearningResource. Là nguồn để P2 (onboarding chọn nghề) và P4 (render roadmap, tính skill gap) đọc.
 *
 * <p><b>Nguyên tắc an toàn (khác hẳn seeder cũ):</b></p>
 * <ul>
 *   <li>Idempotent: chỉ seed khi bảng template còn rỗng — KHÔNG bao giờ xoá dữ liệu đang có
 *       (seeder cũ tự wipe DB mỗi lần boot → mất template do Counselor tạo tay).</li>
 *   <li>KHÔNG gọi mạng lúc khởi động — việc cào tài liệu từ roadmap.sh chuyển thành hành động
 *       admin bấm tay ({@code ResourceScraperService}), tránh 1500+ request chặn boot.</li>
 *   <li>Set {@code tier} đúng (suy từ độ sâu cây) để P4 render đủ 3 tầng Nền tảng/Cốt lõi/Nâng cao.</li>
 * </ul>
 *
 * <p>Chạy sau {@code config/DataSeeder} (Role) nhờ {@link Order}(2).</p>
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class TemplateDataSeeder implements CommandLineRunner {

    private static final int MAX_TIER = 3;

    private final SkillRepository skillRepository;
    private final SkillNodeRepository skillNodeRepository;
    private final SkillTreeTemplateRepository skillTreeTemplateRepository;
    private final CareerRoleRepository careerRoleRepository;
    private final LearningResourceRepository learningResourceRepository;

    @Override
    public void run(String... args) {
        // Idempotent: đã có template thì bỏ qua toàn bộ (không đụng dữ liệu hiện có).
        if (skillTreeTemplateRepository.count() > 0) {
            return;
        }

        seedHeroBackendRoadmap();   // 1 roadmap curate tay: đủ 3 tầng + ≥2 link/node (offline)
        seedFromFolder();           // các roadmap thật từ data/*.json: cấu trúc + tier, chưa có link
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1) Roadmap "hero" — curate tay, luôn đầy đủ để demo/bảo vệ (không cần mạng)
    // ─────────────────────────────────────────────────────────────────────────
    private void seedHeroBackendRoadmap() {
        CareerRole role = careerRoleRepository.save(CareerRole.builder()
                .name("Java Backend Developer")
                .description("Phát triển ứng dụng phía máy chủ bằng Java + hệ sinh thái Spring.")
                .expectedSalaryRange("15,000,000 - 35,000,000 VND")
                .marketDemand("High")
                .build());

        SkillTreeTemplate template = skillTreeTemplateRepository.save(SkillTreeTemplate.builder()
                .name("Lộ trình Java Backend Developer")
                .description("Khung lộ trình chuẩn từ nền tảng đến nâng cao cho lập trình viên Java Backend.")
                .targetRoleId(role.getId())
                .active(true)
                .build());

        // Tầng 1 — Nền tảng (không có node cha)
        SkillNode git = saveHeroNode(template, "Git", "Tools", null, 1);
        addResource(git, "Pro Git (sách chính thức)", "https://git-scm.com/book/vi/v2", "DOCUMENTATION");
        addResource(git, "Git Cheat Sheet - GitHub Education", "https://education.github.com/git-cheat-sheet-education.pdf", "DOCUMENTATION");

        SkillNode java = saveHeroNode(template, "Java Core", "Language", null, 1);
        addResource(java, "Java Tutorial - W3Schools", "https://www.w3schools.com/java/", "DOCUMENTATION");
        addResource(java, "Java Programming - freeCodeCamp (YouTube)", "https://www.youtube.com/watch?v=grEKMHGYyns", "VIDEO");

        SkillNode sql = saveHeroNode(template, "SQL", "Database", null, 1);
        addResource(sql, "SQL Tutorial - Mode", "https://mode.com/sql-tutorial/", "COURSE");
        addResource(sql, "Learn SQL - W3Schools", "https://www.w3schools.com/sql/", "DOCUMENTATION");

        // Tầng 2 — Cốt lõi (cha là Java Core)
        SkillNode spring = saveHeroNode(template, "Spring Boot", "Framework", java, 2);
        addResource(spring, "Spring Boot Reference (chính thức)", "https://docs.spring.io/spring-boot/index.html", "DOCUMENTATION");
        addResource(spring, "Spring Boot Tutorial - Amigoscode (YouTube)", "https://www.youtube.com/watch?v=9SGDpanrc8U", "VIDEO");

        // Tầng 3 — Nâng cao (cha là Spring Boot)
        SkillNode rest = saveHeroNode(template, "REST API", "Backend", spring, 3);
        addResource(rest, "REST API Tutorial", "https://restfulapi.net/", "ARTICLE");
        addResource(rest, "Designing REST APIs - Microsoft", "https://learn.microsoft.com/azure/architecture/best-practices/api-design", "DOCUMENTATION");

        SkillNode security = saveHeroNode(template, "Spring Security", "Security", spring, 3);
        addResource(security, "Spring Security Reference (chính thức)", "https://docs.spring.io/spring-security/reference/index.html", "DOCUMENTATION");
        addResource(security, "Spring Security - Dan Vega (YouTube)", "https://www.youtube.com/watch?v=iJ2muJniikY", "VIDEO");
    }

    private SkillNode saveHeroNode(SkillTreeTemplate template, String skillName, String category, SkillNode parent, int tier) {
        Skill skill = findOrCreateSkill(skillName, category);
        return skillNodeRepository.save(SkillNode.builder()
                .template(template)
                .skill(skill)
                .parent(parent)
                .title(skill.getName())
                .tier(tier)
                .orderIndex(0)
                .requiredLevel(1)
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2) Các roadmap thật từ resources/data/*.json — chỉ dựng cấu trúc + tier
    // ─────────────────────────────────────────────────────────────────────────
    private void seedFromFolder() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:data/*.json");
            ObjectMapper mapper = new ObjectMapper();

            for (Resource resource : resources) {
                try {
                    RoadmapJson data = mapper.readValue(resource.getInputStream(), RoadmapJson.class);
                    seedOneRoadmap(data);
                } catch (Exception e) {
                    System.err.println("[TemplateDataSeeder] Bỏ qua file " + resource.getFilename() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[TemplateDataSeeder] Lỗi quét data/*.json: " + e.getMessage());
        }
    }

    private void seedOneRoadmap(RoadmapJson data) {
        if (data.getTitle() == null || data.getTitle().getPage() == null) {
            return;
        }
        String roleName = data.getTitle().getPage();
        if (careerRoleRepository.findByName(roleName).isPresent()) {
            return; // đã seed (vd trùng với hero) → bỏ qua
        }

        CareerRole role = careerRoleRepository.save(CareerRole.builder()
                .name(roleName)
                .description(data.getDescription())
                .expectedSalaryRange("15,000,000 - 40,000,000 VND")
                .marketDemand("High")
                .build());

        SkillTreeTemplate template = skillTreeTemplateRepository.save(SkillTreeTemplate.builder()
                .name("Lộ trình " + roleName)
                .description("Khung lộ trình chuẩn (nguồn: roadmap.sh).")
                .targetRoleId(role.getId())
                .active(true)
                .build());

        // Bản đồ tra cứu node theo id + cạnh đi tới (target <- các source)
        Map<String, RoadmapJson.NodeInfo> nodesById = new HashMap<>();
        for (RoadmapJson.NodeInfo n : data.getNodes()) {
            nodesById.put(n.getId(), n);
        }
        Map<String, List<String>> incomingEdges = new HashMap<>();
        if (data.getEdges() != null) {
            for (RoadmapJson.EdgeInfo e : data.getEdges()) {
                incomingEdges.computeIfAbsent(e.getTarget(), k -> new ArrayList<>()).add(e.getSource());
            }
        }

        // Pass 1: tạo SkillNode cho mọi node loại topic/subtopic (chưa gắn cha)
        Map<String, SkillNode> created = new HashMap<>();
        for (RoadmapJson.NodeInfo n : data.getNodes()) {
            if (!isSkillNode(n)) {
                continue;
            }
            String label = n.getData() == null ? null : n.getData().getLabel();
            if (label == null || label.trim().isEmpty()) {
                continue;
            }
            Skill skill = findOrCreateSkill(label.trim(), "General");
            SkillNode node = skillNodeRepository.save(SkillNode.builder()
                    .template(template)
                    .skill(skill)
                    .title(skill.getName())
                    .tier(1)
                    .orderIndex(0)
                    .requiredLevel(1)
                    .build());
            created.put(n.getId(), node);
        }

        // Pass 2: gắn cha (node topic/subtopic gần nhất theo cạnh)
        for (Map.Entry<String, SkillNode> entry : created.entrySet()) {
            SkillNode parent = findParentSkillNode(entry.getKey(), incomingEdges, nodesById, created);
            if (parent != null) {
                entry.getValue().setParent(parent);
            }
        }

        // Pass 3: suy tier từ độ sâu cây (gốc=1, con=2, cháu trở xuống=3) rồi lưu
        for (SkillNode node : created.values()) {
            node.setTier(depthTier(node));
            skillNodeRepository.save(node);
        }
    }

    private boolean isSkillNode(RoadmapJson.NodeInfo n) {
        return "topic".equals(n.getType()) || "subtopic".equals(n.getType());
    }

    /** Lần theo các cạnh đi tới để tìm node topic/subtopic gần nhất làm cha. */
    private SkillNode findParentSkillNode(String nodeId,
                                          Map<String, List<String>> incomingEdges,
                                          Map<String, RoadmapJson.NodeInfo> nodesById,
                                          Map<String, SkillNode> created) {
        List<String> sources = incomingEdges.get(nodeId);
        if (sources == null) {
            return null;
        }
        for (String sourceId : sources) {
            if (created.containsKey(sourceId)) {
                return created.get(sourceId);
            }
            RoadmapJson.NodeInfo src = nodesById.get(sourceId);
            if (src == null) {
                continue;
            }
            SkillNode ancestor = findParentSkillNode(sourceId, incomingEdges, nodesById, created);
            if (ancestor != null) {
                return ancestor;
            }
        }
        return null;
    }

    /** tier = min(độ sâu + 1, 3). Có chặn vòng lặp phòng dữ liệu cây bị chu trình. */
    private int depthTier(SkillNode node) {
        int depth = 0;
        SkillNode parent = node.getParent();
        while (parent != null && depth < MAX_TIER) {
            depth++;
            parent = parent.getParent();
        }
        return Math.min(depth + 1, MAX_TIER);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers dùng chung
    // ─────────────────────────────────────────────────────────────────────────
    private Skill findOrCreateSkill(String name, String category) {
        return skillRepository.findByName(name)
                .orElseGet(() -> skillRepository.save(Skill.builder()
                        .name(name)
                        .category(category)
                        .build()));
    }

    private void addResource(SkillNode node, String title, String url, String type) {
        learningResourceRepository.save(LearningResource.builder()
                .skillNode(node)
                .title(title)
                .url(url)
                .resourceType(type)
                .description("Tài liệu học tập được biên soạn sẵn.")
                .build());
    }
}
