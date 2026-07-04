package vn.uth.careercompass.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import vn.uth.careercompass.admin.dto.CuratedRoadmap;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Seed dữ liệu SkillTree của gói P7 từ các lộ trình CURATE ở {@code resources/data/roadmaps/*.json}.
 * Mỗi file = 1 CareerRole + 1 SkillTreeTemplate + N SkillNode (mỗi node là 1 skill cụ thể, 3 tầng,
 * kèm ≥2 LearningResource). Là nguồn để P2 (onboarding) và P4 (roadmap, skill-gap) đọc.
 *
 * <p><b>Nguyên tắc:</b></p>
 * <ul>
 *   <li>Idempotent: chỉ seed khi bảng template còn rỗng — KHÔNG bao giờ xoá dữ liệu đang có.</li>
 *   <li>KHÔNG gọi mạng lúc khởi động — link học đã được bake sẵn trong file JSON
 *       (nguồn: roadmap.sh scrape sẵn + curate official docs).</li>
 *   <li>Node = <b>skill cụ thể</b> (đúng khái niệm Skill của document), không phải section-title.
 *       3 tầng = trường {@code tier} (1 Nền tảng · 2 Cốt lõi · 3 Nâng cao).</li>
 * </ul>
 *
 * <p>Format thô roadmap.sh ({@code data/*.json}) KHÔNG còn được seed trực tiếp — chỉ giữ làm nguồn
 * cho công cụ scrape link của admin sau này.</p>
 *
 * <p>Chạy sau {@code config/DataSeeder} (Role) nhờ {@link Order}(2).</p>
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class TemplateDataSeeder implements CommandLineRunner {

    private final SkillRepository skillRepository;
    private final SkillNodeRepository skillNodeRepository;
    private final SkillTreeTemplateRepository skillTreeTemplateRepository;
    private final CareerRoleRepository careerRoleRepository;
    private final LearningResourceRepository learningResourceRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) {
        // Idempotent: đã có template thì bỏ qua (không đụng dữ liệu hiện có).
        if (skillTreeTemplateRepository.count() > 0) {
            return;
        }
        try {
            Resource[] files = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:data/roadmaps/*.json");
            for (Resource file : files) {
                try {
                    CuratedRoadmap roadmap = objectMapper.readValue(file.getInputStream(), CuratedRoadmap.class);
                    seedRoadmap(roadmap);
                } catch (Exception e) {
                    System.err.println("[TemplateDataSeeder] Bỏ qua file " + file.getFilename() + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("[TemplateDataSeeder] Lỗi quét data/roadmaps/*.json: " + e.getMessage());
        }
    }

    private void seedRoadmap(CuratedRoadmap cr) {
        if (cr.getRole() == null || cr.getNodes() == null) {
            return;
        }
        // Idempotent theo role (phòng chạy lại khi thêm file mới mà template chưa rỗng hẳn).
        if (careerRoleRepository.findByName(cr.getRole()).isPresent()) {
            return;
        }

        CareerRole role = careerRoleRepository.save(CareerRole.builder()
                .name(cr.getRole())
                .description(cr.getDescription())
                .expectedSalaryRange(cr.getSalaryRange())
                .marketDemand(cr.getDemand())
                .build());

        SkillTreeTemplate template = skillTreeTemplateRepository.save(SkillTreeTemplate.builder()
                .name("Lộ trình " + cr.getRole())
                .description(cr.getDescription())
                .targetRoleId(role.getId())
                .active(true)
                .build());

        // Pass 1: tạo node (skill cụ thể + tier), lưu theo tên skill để pass 2 gắn cha.
        Map<String, SkillNode> bySkill = new HashMap<>();
        int order = 0;
        for (CuratedRoadmap.Node n : cr.getNodes()) {
            Skill skill = findOrCreateSkill(n.getSkill(), n.getCategory());
            SkillNode node = skillNodeRepository.save(SkillNode.builder()
                    .template(template)
                    .skill(skill)
                    .title(n.getSkill())
                    .tier(n.getTier() == null ? 1 : n.getTier())
                    .orderIndex(order++)
                    .requiredLevel(1)
                    .build());
            bySkill.put(n.getSkill(), node);
        }

        // Pass 2: gắn cha (theo tên skill trong CÙNG roadmap) + thêm link học.
        for (CuratedRoadmap.Node n : cr.getNodes()) {
            SkillNode node = bySkill.get(n.getSkill());
            if (n.getParent() != null) {
                SkillNode parent = bySkill.get(n.getParent());
                if (parent != null) {
                    node.setParent(parent);
                    skillNodeRepository.save(node);
                }
            }
            if (n.getLinks() != null) {
                for (CuratedRoadmap.Link link : n.getLinks()) {
                    learningResourceRepository.save(LearningResource.builder()
                            .skillNode(node)
                            .title(link.getTitle())
                            .url(link.getUrl())
                            .resourceType(link.getType() == null ? "ARTICLE" : link.getType())
                            .description("Tài liệu học cho kỹ năng " + n.getSkill() + ".")
                            .build());
                }
            }
        }
    }

    private Skill findOrCreateSkill(String name, String category) {
        return skillRepository.findByName(name)
                .orElseGet(() -> skillRepository.save(Skill.builder()
                        .name(name)
                        .category(category == null ? "General" : category)
                        .build()));
    }
}
