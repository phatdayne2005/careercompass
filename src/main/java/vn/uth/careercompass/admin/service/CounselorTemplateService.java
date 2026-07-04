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
import vn.uth.careercompass.kernel.repository.UserRepository;
 
import java.util.List;
 
@Service
@RequiredArgsConstructor
public class CounselorTemplateService {
    private final SkillTreeTemplateRepository templateRepository;
    private final SkillNodeRepository nodeRepository;
    private final SkillRepository skillRepository;
    private final LearningResourceRepository resourceRepository;
    private final CareerRoleRepository careerRoleRepository;
    private final UserRepository userRepository;
 
    public List<SkillTreeTemplate> getAllTemplates() {
        try {
            return templateRepository.findAllWithCareerRole();
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách lộ trình: " + e.getMessage());
            throw new RuntimeException("Lấy danh sách lộ trình thất bại!", e);
        }
    }
 
    public SkillTreeTemplate getTemplateById(Long id) {
        try {
            return templateRepository.findByIdWithCareerRole(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình có ID: " + id));
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin lộ trình ID " + id + ": " + e.getMessage());
            throw new RuntimeException("Lấy thông tin lộ trình thất bại!", e);
        }
    }
 
    public SkillNode getNodeById(Long nodeId) {
        try {
            return nodeRepository.findByIdWithRelations(nodeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút có ID: " + nodeId));
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin nút ID " + nodeId + ": " + e.getMessage());
            throw new RuntimeException("Lấy thông tin nút thất bại!", e);
        }
    }
 
    public List<SkillNode> getNodesByTemplateId(Long templateId) {
        try {
            return nodeRepository.findAllByTemplateIdWithRelations(templateId);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách nút của lộ trình ID " + templateId + ": " + e.getMessage());
            throw new RuntimeException("Lấy danh sách nút lộ trình thất bại!", e);
        }
    }
 
    @Transactional
    public SkillNode addNode(Long templateId, Long skillId, String newSkillName, String newSkillCategory, Integer level, Long parentId) {
        try {
            SkillTreeTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình có ID: " + templateId));
            
            Skill skill = null;
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
                        .orElseGet(() -> {
                            Skill newSkill = Skill.builder()
                                    .name(trimmedName)
                                    .category(trimmedCat)
                                    .build();
                            return skillRepository.save(newSkill);
                        });
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
 
            SkillNode newNode = SkillNode.builder()
                    .template(template)
                    .skill(skill)
                    .title(skill.getName())
                    .requiredLevel(level)
                    .parent(parentNode)
                    .build();
 
            return nodeRepository.save(newNode);
        } catch (Exception e) {
            System.err.println("Lỗi khi thêm nút mới vào lộ trình ID " + templateId + ": " + e.getMessage());
            throw new RuntimeException("Thêm nút vào lộ trình thất bại!", e);
        }
    }
 
    @Transactional
    public void deleteNode(Long nodeId) {
        try {
            SkillNode node = nodeRepository.findById(nodeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút có ID: " + nodeId));
            nodeRepository.delete(node);
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa nút ID " + nodeId + ": " + e.getMessage());
            throw new RuntimeException("Xóa nút lộ trình thất bại!", e);
        }
    }

    @Transactional
    public SkillNode updateNode(Long nodeId, Integer level, Long parentId) {
        try {
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
            
            node.setRequiredLevel(level);
            node.setParent(parentNode);
            return nodeRepository.save(node);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật nút kỹ năng ID " + nodeId + ": " + e.getMessage());
            throw new RuntimeException("Cập nhật nút kỹ năng thất bại!", e);
        }
    }
 
    @Transactional
    public LearningResource addResource(Long nodeId, String title, String url, String type, String desc) {
        try {
            SkillNode node = nodeRepository.findById(nodeId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nút có ID: " + nodeId));
 
            LearningResource resource = LearningResource.builder()
                    .skillNode(node)
                    .title(title)
                    .url(url)
                    .resourceType(type)
                    .description(desc)
                    .build();
 
            return resourceRepository.save(resource);
        } catch (Exception e) {
            System.err.println("Lỗi khi thêm tài liệu vào nút ID " + nodeId + ": " + e.getMessage());
            throw new RuntimeException("Thêm tài nguyên học tập thất bại!", e);
        }
    }
 
    @Transactional
    public void deleteResource(Long resourceId) {
        try {
            LearningResource resource = resourceRepository.findById(resourceId)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài nguyên có ID: " + resourceId));
            resourceRepository.delete(resource);
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa tài nguyên ID " + resourceId + ": " + e.getMessage());
            throw new RuntimeException("Xóa tài nguyên học tập thất bại!", e);
        }
    }
 
    @Transactional
    public SkillTreeTemplate createTemplate(String name, String description, String careerRoleName) {
        try {
            if (careerRoleName == null || careerRoleName.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập vai trò nghề nghiệp mục tiêu!");
            }
            String trimmedRole = careerRoleName.trim();
            CareerRole careerRole = careerRoleRepository.findByName(trimmedRole)
                    .orElseGet(() -> {
                        CareerRole newRole = CareerRole.builder()
                                .name(trimmedRole)
                                .description("Mô tả cho vai trò " + trimmedRole)
                                .expectedSalaryRange("Thỏa thuận")
                                .marketDemand("Cao")
                                .build();
                        return careerRoleRepository.save(newRole);
                    });
 
            if (templateRepository.findByCareerRoleId(careerRole.getId()).isPresent()) {
                throw new IllegalArgumentException("Vai trò nghề nghiệp này đã có lộ trình mẫu!");
            }
 
            SkillTreeTemplate template = SkillTreeTemplate.builder()
                    .name(name)
                    .description(description)
                    .targetRoleId(careerRole.getId())
                    .build();
            return templateRepository.save(template);
        } catch (Exception e) {
            System.err.println("Lỗi khi tạo mới lộ trình: " + e.getMessage());
            throw new RuntimeException("Tạo mới lộ trình thất bại!", e);
        }
    }
 
    @Transactional
    public SkillTreeTemplate updateTemplate(Long id, String name, String description, String careerRoleName) {
        try {
            SkillTreeTemplate template = templateRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình có ID: " + id));
            
            if (careerRoleName == null || careerRoleName.trim().isEmpty()) {
                throw new IllegalArgumentException("Vui lòng nhập vai trò nghề nghiệp mục tiêu!");
            }
            
            String trimmedRole = careerRoleName.trim();
            CareerRole careerRole = careerRoleRepository.findByName(trimmedRole)
                    .orElseGet(() -> {
                        CareerRole newRole = CareerRole.builder()
                                .name(trimmedRole)
                                .description("Mô tả cho vai trò " + trimmedRole)
                                .expectedSalaryRange("Thỏa thuận")
                                .marketDemand("Cao")
                                .build();
                        return careerRoleRepository.save(newRole);
                    });
 
            templateRepository.findByCareerRoleId(careerRole.getId()).ifPresent(existingTpl -> {
                if (!existingTpl.getId().equals(id)) {
                    throw new IllegalArgumentException("Vai trò nghề nghiệp này đã được gán cho một lộ trình khác!");
                }
            });
 
            template.setName(name);
            template.setDescription(description);
            template.setTargetRoleId(careerRole.getId());
            
            return templateRepository.save(template);
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật lộ trình ID " + id + ": " + e.getMessage());
            throw new RuntimeException("Cập nhật lộ trình thất bại!", e);
        }
    }
 
    @Transactional
    public void deleteTemplate(Long id) {
        try {
            SkillTreeTemplate template = templateRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lộ trình có ID: " + id));
            
            // 1. Phá vỡ quan hệ cha-con tự liên kết (self-referential) trên các SkillNode
            List<SkillNode> nodes = nodeRepository.findAllByTemplateIdWithRelations(id);
            for (SkillNode node : nodes) {
                node.setParent(null);
                nodeRepository.save(node);
            }
            nodeRepository.flush(); // Đẩy các cập nhật NULL xuống DB trước

            CareerRole role = template.getCareerRole();

            // 2. Gỡ tham chiếu CareerRole của User để tránh lỗi khóa ngoại
            if (role != null) {
                userRepository.findAll().forEach(u -> {
                    if (role.equals(u.getCareerRole())) {
                        u.setCareerRole(null);
                        userRepository.save(u);
                    }
                });
            }

            // 3. Xóa Template (sẽ Cascade xóa tất cả SkillNode và LearningResource tương ứng)
            templateRepository.delete(template);
            templateRepository.flush();

            // 4. Xóa CareerRole
            if (role != null) {
                careerRoleRepository.delete(role);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi xóa lộ trình ID " + id + ": " + e.getMessage());
            throw new RuntimeException("Xóa lộ trình thất bại!", e);
        }
    }
}
