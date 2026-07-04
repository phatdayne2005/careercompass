package vn.uth.careercompass.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import vn.uth.careercompass.roadmap.repository.SkillGapReportRepository;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.util.List;

/**
 * Service quản lý SkillTree template cho Counselor (P7).
 *
 * <p><b>Xử lý lỗi:</b> KHÔNG bọc try/catch nuốt lỗi. Validation ném
 * {@link IllegalArgumentException} với message rõ ràng (vd "Vui lòng nhập vai trò...") để tầng
 * controller hiển thị lại cho người dùng; {@code @Transactional} tự rollback khi có exception.
 * (Trước đây mọi method bọc {@code catch(Exception)} rồi ném RuntimeException chung chung
 * "...thất bại!" → nuốt mất lý do thật.)</p>
 */
@Service
@RequiredArgsConstructor
public class CounselorTemplateService {
    private final SkillTreeTemplateRepository templateRepository;
    private final SkillNodeRepository nodeRepository;
    private final SkillRepository skillRepository;
    private final LearningResourceRepository resourceRepository;
    private final CareerRoleRepository careerRoleRepository;
    private final UserNodeProgressRepository userNodeProgressRepository;
    private final SkillGapReportRepository skillGapReportRepository;

    public List<SkillTreeTemplate> getAllTemplates() {
        return templateRepository.findAllWithCareerRole();
    }

    public SkillTreeTemplate getTemplateById(Long id) {
        return templateRepository.findByIdWithCareerRole(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình có ID: " + id));
    }

    public SkillNode getNodeById(Long nodeId) {
        return nodeRepository.findByIdWithRelations(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút có ID: " + nodeId));
    }

    public List<SkillNode> getNodesByTemplateId(Long templateId) {
        return nodeRepository.findAllByTemplateIdWithRelations(templateId);
    }

    @Transactional
    public SkillNode addNode(Long templateId, Long skillId, String newSkillName, String newSkillCategory, Integer tier, Long parentId) {
        SkillTreeTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình có ID: " + templateId));

        Skill skill;
        if (newSkillName != null && !newSkillName.trim().isEmpty()) {
            String trimmedName = newSkillName.trim();
            String trimmedCat = (newSkillCategory != null && !newSkillCategory.trim().isEmpty()) ? newSkillCategory.trim() : "General";
            skill = skillRepository.findByNameIgnoreCase(trimmedName)
                    .map(existingSkill -> {
                        if (!trimmedCat.equalsIgnoreCase(existingSkill.getCategory())) {
                            existingSkill.setCategory(trimmedCat);
                            return skillRepository.save(existingSkill);
                        }
                        return existingSkill;
                    })
                    .orElseGet(() -> skillRepository.save(Skill.builder()
                            .name(trimmedName)
                            .category(trimmedCat)
                            .build()));
        } else if (skillId != null) {
            skill = skillRepository.findById(skillId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kỹ năng có ID: " + skillId));
        } else {
            throw new IllegalArgumentException("Vui lòng chọn kỹ năng sẵn có hoặc nhập tên kỹ năng mới!");
        }

        SkillNode parentNode = null;
        if (parentId != null) {
            parentNode = nodeRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút cha có ID: " + parentId));
        }

        return nodeRepository.save(SkillNode.builder()
                .template(template)
                .skill(skill)
                .title(skill.getName())
                .tier(tier == null ? 1 : tier)
                .parent(parentNode)
                .build());
    }

    @Transactional
    public void deleteNode(Long nodeId) {
        SkillNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút có ID: " + nodeId));
        nodeRepository.delete(node);
    }

    @Transactional
    public SkillNode updateNode(Long nodeId, Integer tier, Long parentId) {
        SkillNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút kỹ năng có ID: " + nodeId));

        SkillNode parentNode = null;
        if (parentId != null) {
            if (parentId.equals(nodeId)) {
                throw new IllegalArgumentException("Nút tiên quyết không thể là chính nó!");
            }
            parentNode = nodeRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút cha có ID: " + parentId));
        }

        node.setTier(tier == null ? node.getTier() : tier);
        node.setParent(parentNode);
        return nodeRepository.save(node);
    }

    @Transactional
    public LearningResource addResource(Long nodeId, String title, String url, String type, String desc) {
        SkillNode node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút có ID: " + nodeId));

        return resourceRepository.save(LearningResource.builder()
                .skillNode(node)
                .title(title)
                .url(url)
                .resourceType(type)
                .description(desc)
                .build());
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        LearningResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài nguyên có ID: " + resourceId));
        resourceRepository.delete(resource);
    }

    @Transactional
    public SkillTreeTemplate createTemplate(String name, String description, String careerRoleName) {
        CareerRole careerRole = resolveOrCreateCareerRole(careerRoleName);

        if (templateRepository.findByCareerRoleId(careerRole.getId()).isPresent()) {
            throw new IllegalArgumentException("Vai trò nghề nghiệp này đã có lộ trình mẫu!");
        }

        return templateRepository.save(SkillTreeTemplate.builder()
                .name(name)
                .description(description)
                .targetRoleId(careerRole.getId())
                .build());
    }

    @Transactional
    public SkillTreeTemplate updateTemplate(Long id, String name, String description, String careerRoleName) {
        SkillTreeTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình có ID: " + id));

        CareerRole careerRole = resolveOrCreateCareerRole(careerRoleName);

        templateRepository.findByCareerRoleId(careerRole.getId()).ifPresent(existingTpl -> {
            if (!existingTpl.getId().equals(id)) {
                throw new IllegalArgumentException("Vai trò nghề nghiệp này đã được gán cho một lộ trình khác!");
            }
        });

        template.setName(name);
        template.setDescription(description);
        template.setTargetRoleId(careerRole.getId());
        return templateRepository.save(template);
    }

    /** Tìm CareerRole theo tên (tạo mới nếu chưa có). Ném lỗi rõ nếu tên rỗng. */
    private CareerRole resolveOrCreateCareerRole(String careerRoleName) {
        if (careerRoleName == null || careerRoleName.trim().isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập vai trò nghề nghiệp mục tiêu!");
        }
        String trimmedRole = careerRoleName.trim();
        return careerRoleRepository.findByName(trimmedRole)
                .orElseGet(() -> careerRoleRepository.save(CareerRole.builder()
                        .name(trimmedRole)
                        .description("Mô tả cho vai trò " + trimmedRole)
                        .expectedSalaryRange("Thỏa thuận")
                        .marketDemand("Cao")
                        .build()));
    }

    /**
     * Xoá 1 lộ trình + toàn bộ dữ liệu phụ thuộc, đúng thứ tự khoá ngoại (KHÔNG có cascade DB).
     * Thứ tự: skill_gap_reports → user_node_progress → learning_resources → (gỡ self-ref) skill_nodes → template.
     *
     * <p>Đụng repo của P4 (progress/report) vì các bảng đó trỏ vào node/template mà P7 sở hữu —
     * không dọn trước sẽ vi phạm FK. CareerRole được GIỮ LẠI (catalog dùng chung); muốn xoá hẳn
     * nghề nghiệp là hành động admin riêng.</p>
     */
    @Transactional
    public void deleteTemplate(Long id) {
        SkillTreeTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình có ID: " + id));

        // 1. Dữ liệu người dùng phụ thuộc (P4) — xoá trước để không vi phạm khoá ngoại.
        skillGapReportRepository.deleteByTemplate_Id(id);
        userNodeProgressRepository.deleteBySkillNode_Template_Id(id);

        // 2. Tài liệu học của mọi node trong lộ trình.
        resourceRepository.deleteBySkillNode_Template_Id(id);

        // 3. Gỡ liên kết cha-con (self-reference) rồi xoá toàn bộ node.
        List<SkillNode> nodes = nodeRepository.findAllByTemplateIdWithRelations(id);
        nodes.forEach(node -> node.setParent(null));
        nodeRepository.saveAll(nodes);
        nodeRepository.flush();
        nodeRepository.deleteAll(nodes);

        // 4. Xoá template (CareerRole giữ nguyên).
        templateRepository.delete(template);
    }
}
