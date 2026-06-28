package vn.uth.careercompass.admin.web;
 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.LearningResourceRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.admin.service.CounselorTemplateService;
 
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
 
@Controller
@RequiredArgsConstructor
public class CounselorTemplateController {
    private final CounselorTemplateService counselorTemplateService;
    private final SkillRepository skillRepository;
    private final LearningResourceRepository resourceRepository;
    private final CareerRoleRepository careerRoleRepository;
 
    private List<Skill> getAvailableSkills(List<SkillNode> nodes) {
        List<Skill> allSkills = skillRepository.findAll();
        Set<Long> existingSkillIds = nodes.stream()
                .map(n -> n.getSkill().getId())
                .collect(Collectors.toSet());
        return allSkills.stream()
                .filter(s -> !existingSkillIds.contains(s.getId()))
                .collect(Collectors.toList());
    }
 
    private List<String> getCleanResourceTypes() {
        List<String> rawTypes = resourceRepository.findDistinctResourceTypes();
        Set<String> cleanTypes = new java.util.LinkedHashSet<>(List.of("VIDEO", "ARTICLE", "COURSE", "DOCUMENTATION", "BOOK", "SLIDES"));
        for (String t : rawTypes) {
            if (t != null && !t.trim().isEmpty()) {
                cleanTypes.add(t.toUpperCase().trim());
            }
        }
        return new java.util.ArrayList<>(cleanTypes);
    }
 
    @GetMapping("/counselor/templates")
    public String listTemplates(Model model) {
        List<SkillTreeTemplate> templates = counselorTemplateService.getAllTemplates();
        model.addAttribute("templates", templates);
        return "counselor/templates";
    }
 
    @PostMapping("/counselor/templates")
    public String createTemplate(@RequestParam String name,
                                 @RequestParam String description,
                                 @RequestParam String careerRoleName) {
        counselorTemplateService.createTemplate(name, description, careerRoleName);
        return "redirect:/counselor/templates";
    }
 
    @PostMapping("/counselor/templates/{id}/update")
    public String updateTemplate(@PathVariable Long id,
                                 @RequestParam String name,
                                 @RequestParam String description,
                                 @RequestParam String careerRoleName) {
        counselorTemplateService.updateTemplate(id, name, description, careerRoleName);
        return "redirect:/counselor/templates";
    }
 
    @PostMapping("/counselor/templates/{id}/delete")
    public String deleteTemplate(@PathVariable Long id) {
        counselorTemplateService.deleteTemplate(id);
        return "redirect:/counselor/templates";
    }
 
    @GetMapping("/counselor/templates/{id}/editor")
    public String editor(@PathVariable Long id, Model model) {
        SkillTreeTemplate template = counselorTemplateService.getTemplateById(id);
        List<SkillNode> nodes = counselorTemplateService.getNodesByTemplateId(id);
        List<Skill> availableSkills = getAvailableSkills(nodes);
        List<String> categories = skillRepository.findDistinctCategories();
        List<String> resourceTypes = getCleanResourceTypes();
 
        model.addAttribute("template", template);
        model.addAttribute("nodes", nodes);
        model.addAttribute("allSkills", availableSkills);
        model.addAttribute("categories", categories);
        model.addAttribute("resourceTypes", resourceTypes);
        model.addAttribute("currentNode", null); // Bảng chi tiết bên phải lúc đầu trống
        return "counselor/editor";
    }
 
    @PostMapping("/counselor/templates/{id}/nodes")
    public String addNode(@PathVariable Long id,
                           @RequestParam(required = false) String skillId,
                           @RequestParam(required = false) String newSkillName,
                           @RequestParam(required = false) String newSkillCategory,
                           @RequestParam Integer level,
                           @RequestParam(required = false) String parentId,
                           Model model) {
        Long skillIdLong = (skillId != null && !skillId.trim().isEmpty()) ? Long.valueOf(skillId.trim()) : null;
        Long parentIdLong = (parentId != null && !parentId.trim().isEmpty()) ? Long.valueOf(parentId.trim()) : null;
 
        counselorTemplateService.addNode(id, skillIdLong, newSkillName, newSkillCategory, level, parentIdLong);
        
        SkillTreeTemplate template = counselorTemplateService.getTemplateById(id);
        List<SkillNode> nodes = counselorTemplateService.getNodesByTemplateId(id);
        List<Skill> availableSkills = getAvailableSkills(nodes);
        List<String> categories = skillRepository.findDistinctCategories();
 
        model.addAttribute("template", template);
        model.addAttribute("nodes", nodes);
        model.addAttribute("allSkills", availableSkills);
        model.addAttribute("categories", categories);
        return "counselor/editor :: editor-left-pane";
    }
 
    @DeleteMapping("/counselor/templates/{templateId}/nodes/{nodeId}")
    public String deleteNode(@PathVariable Long templateId,
                             @PathVariable Long nodeId,
                             Model model) {
        counselorTemplateService.deleteNode(nodeId);
 
        SkillTreeTemplate template = counselorTemplateService.getTemplateById(templateId);
        List<SkillNode> nodes = counselorTemplateService.getNodesByTemplateId(templateId);
        List<Skill> availableSkills = getAvailableSkills(nodes);
        List<String> categories = skillRepository.findDistinctCategories();
 
        model.addAttribute("template", template);
        model.addAttribute("nodes", nodes);
        model.addAttribute("allSkills", availableSkills);
        model.addAttribute("categories", categories);
        return "counselor/editor :: editor-left-pane";
    }
 
    @GetMapping("/counselor/nodes/{nodeId}/details")
    public String nodeDetails(@PathVariable Long nodeId, Model model) {
        SkillNode node = counselorTemplateService.getNodeById(nodeId);
        List<String> resourceTypes = getCleanResourceTypes();
        
        // Lấy danh sách tất cả các node trong cùng template của node này để làm danh sách lựa chọn nút cha
        List<SkillNode> allNodes = counselorTemplateService.getNodesByTemplateId(node.getSkillTreeTemplate().getId());
        
        model.addAttribute("currentNode", node);
        model.addAttribute("nodes", allNodes);
        model.addAttribute("resourceTypes", resourceTypes);
        return "counselor/editor :: node-details";
    }
 
    @PostMapping("/counselor/templates/{templateId}/nodes/{nodeId}/update")
    public String updateNode(@PathVariable Long templateId,
                             @PathVariable Long nodeId,
                             @RequestParam Integer level,
                             @RequestParam(required = false) String parentId) {
        Long parentIdLong = (parentId != null && !parentId.trim().isEmpty()) ? Long.valueOf(parentId.trim()) : null;
        counselorTemplateService.updateNode(nodeId, level, parentIdLong);
        return "redirect:/counselor/templates/" + templateId + "/editor";
    }
 
    @PostMapping("/counselor/nodes/{nodeId}/resources")
    public String addResource(@PathVariable Long nodeId,
                              @RequestParam String title,
                              @RequestParam String url,
                              @RequestParam String resourceType,
                              @RequestParam(required = false) String description,
                              Model model) {
        counselorTemplateService.addResource(nodeId, title, url, resourceType, description);
        
        SkillNode node = counselorTemplateService.getNodeById(nodeId);
        List<String> resourceTypes = getCleanResourceTypes();
        
        model.addAttribute("currentNode", node);
        model.addAttribute("resourceTypes", resourceTypes);
        return "counselor/editor :: node-details";
    }
 
    @DeleteMapping("/counselor/nodes/{nodeId}/resources/{resourceId}")
    public String deleteResource(@PathVariable Long nodeId,
                                 @PathVariable Long resourceId,
                                 Model model) {
        counselorTemplateService.deleteResource(resourceId);
 
        SkillNode node = counselorTemplateService.getNodeById(nodeId);
        List<String> resourceTypes = getCleanResourceTypes();
        
        model.addAttribute("currentNode", node);
        model.addAttribute("resourceTypes", resourceTypes);
        return "counselor/editor :: node-details";
    }
}
