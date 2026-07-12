package vn.uth.careercompass.roadmap.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.uth.careercompass.admin.entity.LearningResource;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.LearningResourceRepository;
import vn.uth.careercompass.admin.repository.SkillNodeRepository;
import vn.uth.careercompass.admin.repository.SkillTreeTemplateRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.roadmap.dto.RoadmapNodeDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapResourceDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapService {
    private final SkillTreeTemplateRepository skillTreeTemplateRepository;
    private final SkillNodeRepository skillNodeRepository;
    private final UserNodeProgressRepository userNodeProgressRepository;
    private final LearningResourceRepository learningResourceRepository;

    @Transactional(readOnly = true)
    public List<RoadmapTemplateDTO> getActiveTemplates() {
        return skillTreeTemplateRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(RoadmapTemplateDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoadmapViewDTO getRoadmap(User user, Long templateId) {
        SkillTreeTemplate template = resolveTemplate(user, templateId);
        List<SkillNode> nodes = skillNodeRepository.findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(template.getId());
        Map<Long, UserNodeProgress> progressByNodeId = userNodeProgressRepository
                .findByUserAndSkillNode_Template_Id(user, template.getId())
                .stream()
                .collect(Collectors.toMap(progress -> progress.getSkillNode().getId(), Function.identity()));

        Map<Long, List<RoadmapResourceDTO>> resourcesByNodeId = learningResourceRepository
                .findBySkillNode_Template_IdOrderByIdAsc(template.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        resource -> resource.getSkillNode().getId(),
                        Collectors.mapping(this::toResourceDTO, Collectors.toList())));

        List<RoadmapNodeDTO> nodeDTOs = nodes.stream()
                .map(node -> toNodeDTO(node, progressByNodeId.get(node.getId()),
                        resourcesByNodeId.getOrDefault(node.getId(), List.of())))
                .toList();

        // Gating theo tier: tầng 2 mở khi tầng 1 xong hết; tầng 3 mở khi tầng 1+2 xong hết.
        // (allMatch trên stream rỗng trả true → tầng không có node coi như đã "xong", không chặn tầng sau.)
        boolean tier1Done = nodeDTOs.stream().filter(n -> isTier(n, 1))
                .allMatch(n -> ProgressStatus.DONE.equals(n.getStatus()));
        boolean tier2Done = nodeDTOs.stream().filter(n -> isTier(n, 2))
                .allMatch(n -> ProgressStatus.DONE.equals(n.getStatus()));
        nodeDTOs.forEach(n -> {
            int t = n.getTier() == null ? 1 : n.getTier();
            boolean completed = ProgressStatus.DONE.equals(n.getStatus());
            n.setLocked(!completed && ((t == 2 && !tier1Done) || (t >= 3 && !(tier1Done && tier2Done))));
        });

        int completedNodes = (int) nodeDTOs.stream()
                .filter(node -> ProgressStatus.DONE.equals(node.getStatus()))
                .count();

        return RoadmapViewDTO.builder()
                .template(RoadmapTemplateDTO.from(template))
                .totalNodes(nodeDTOs.size())
                .completedNodes(completedNodes)
                .completionPercent(calculatePercent(completedNodes, nodeDTOs.size()))
                .nodes(nodeDTOs)
                .build();
    }

    @Transactional(readOnly = true)
    public SkillTreeTemplate resolveTemplate(User user, Long templateId) {
        if (templateId != null) {
            return skillTreeTemplateRepository.findById(templateId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy roadmap"));
        }

        if (user.getTargetRoleId() != null) {
            return skillTreeTemplateRepository.findFirstByTargetRoleIdAndActiveTrueOrderByIdAsc(user.getTargetRoleId())
                    .or(() -> skillTreeTemplateRepository.findFirstByActiveTrueOrderByNameAsc())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có roadmap nào"));
        }

        return skillTreeTemplateRepository.findFirstByActiveTrueOrderByNameAsc()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chưa có roadmap nào"));
    }

    private boolean isTier(RoadmapNodeDTO node, int tier) {
        return node.getTier() != null && node.getTier() == tier;
    }

    /**
     * Kiểm 1 node có đang bị khóa với user không (dùng để CHẶN đánh dấu ở ProgressService).
     * Khóa nếu tầng của node > 1 và còn node ở tầng thấp hơn (cùng template) user chưa DONE.
     */
    @Transactional(readOnly = true)
    public boolean isNodeLocked(User user, SkillNode node) {
        int tier = node.getTier() == null ? 1 : node.getTier();
        if (tier <= 1) {
            return false;
        }
        Long templateId = node.getTemplate().getId();
        Set<Long> doneNodeIds = userNodeProgressRepository
                .findByUserAndSkillNode_Template_Id(user, templateId).stream()
                .filter(p -> ProgressStatus.DONE.equals(p.getStatus()))
                .map(p -> p.getSkillNode().getId())
                .collect(Collectors.toSet());
        return skillNodeRepository.findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(templateId).stream()
                .filter(n -> (n.getTier() == null ? 1 : n.getTier()) < tier)
                .anyMatch(n -> !doneNodeIds.contains(n.getId()));
    }

    public double calculatePercent(int completed, int total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((completed * 10000.0) / total) / 100.0;
    }

    private RoadmapNodeDTO toNodeDTO(SkillNode node, UserNodeProgress progress, List<RoadmapResourceDTO> resources) {
        return RoadmapNodeDTO.builder()
                .id(node.getId())
                .parentId(node.getParent() == null ? null : node.getParent().getId())
                .skillId(node.getSkill().getId())
                .skillName(node.getSkill().getName())
                .title(node.getTitle())
                .description(node.getDescription())
                .tier(node.getTier())
                .orderIndex(node.getOrderIndex())
                .requiredLevel(node.getRequiredLevel())
                .status(progress == null ? ProgressStatus.NOT_STARTED : progress.getStatus())
                .resources(resources)
                .build();
    }

    private RoadmapResourceDTO toResourceDTO(LearningResource resource) {
        return RoadmapResourceDTO.builder()
                .title(resource.getTitle())
                .url(resource.getUrl())
                .type(resource.getResourceType())
                .build();
    }
}
