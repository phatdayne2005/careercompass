package vn.uth.careercompass.roadmap.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.SkillNodeRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapReportDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.dto.SkillSummaryDTO;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.SkillGapReport;
import vn.uth.careercompass.roadmap.repository.SkillGapReportRepository;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkillGapService {
    private final RoadmapService roadmapService;
    private final SkillNodeRepository skillNodeRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillGapReportRepository skillGapReportRepository;
    private final UserNodeProgressRepository userNodeProgressRepository;

    @Transactional(readOnly = true)
    public SkillGapResultDTO analyze(User user, Long templateId) {
        SkillTreeTemplate template = roadmapService.resolveTemplate(user, templateId);
        List<SkillNode> nodes = skillNodeRepository.findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(template.getId());

        Map<Long, Skill> requiredSkills = nodes.stream()
                .map(SkillNode::getSkill)
                .collect(Collectors.toMap(Skill::getId, skill -> skill, (left, right) -> left, LinkedHashMap::new));

        Set<Long> acquiredSkillIds = new HashSet<>();
        userSkillRepository.findByUser(user).stream()
                .map(UserSkill::getSkill)
                .map(Skill::getId)
                .forEach(acquiredSkillIds::add);
        userNodeProgressRepository.findByUserAndSkillNode_Template_Id(user, template.getId()).stream()
                .filter(progress -> ProgressStatus.DONE.equals(progress.getStatus()))
                .forEach(progress -> acquiredSkillIds.add(progress.getSkillNode().getSkill().getId()));

        List<SkillSummaryDTO> matchedSkills = requiredSkills.values().stream()
                .filter(skill -> acquiredSkillIds.contains(skill.getId()))
                .sorted(Comparator.comparing(Skill::getName))
                .map(this::toSkillSummary)
                .toList();

        List<SkillSummaryDTO> missingSkills = requiredSkills.values().stream()
                .filter(skill -> !acquiredSkillIds.contains(skill.getId()))
                .sorted(Comparator.comparing(Skill::getName))
                .map(this::toSkillSummary)
                .toList();

        return SkillGapResultDTO.builder()
                .template(RoadmapTemplateDTO.from(template))
                .requiredSkillCount(requiredSkills.size())
                .matchedSkillCount(matchedSkills.size())
                .missingSkillCount(missingSkills.size())
                .matchPercent(roadmapService.calculatePercent(matchedSkills.size(), requiredSkills.size()))
                .matchedSkills(matchedSkills)
                .missingSkills(missingSkills)
                .build();
    }

    @Transactional(readOnly = true)
    public List<SkillSummaryDTO> getAcquiredSkills(User user) {
        Map<Long, SkillSummaryDTO> byId = new LinkedHashMap<>();
        userSkillRepository.findByUserWithSkill(user)
                .forEach(us -> byId.putIfAbsent(us.getSkill().getId(), toSkillSummary(us.getSkill())));
        userNodeProgressRepository.findByUserAndStatusWithSkill(user, ProgressStatus.DONE)
                .forEach(p -> byId.putIfAbsent(p.getSkillNode().getSkill().getId(),
                        toSkillSummary(p.getSkillNode().getSkill())));
        return byId.values().stream()
                .sorted(Comparator.comparing(SkillSummaryDTO::getName))
                .toList();
    }

    @Transactional
    public void addAcquiredSkill(User user, Long skillId, Long templateId) {
        if (skillId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu skillId");
        }

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy kỹ năng"));

        if (!userSkillRepository.existsByUserAndSkill(user, skill)) {
            userSkillRepository.save(UserSkill.builder().user(user).skill(skill).build());
        }

        SkillTreeTemplate template = roadmapService.resolveTemplate(user, templateId);
        skillNodeRepository.findByTemplate_IdAndSkill_Id(template.getId(), skill.getId())
                .orElseGet(() -> createSupplementalRoadmapNode(template, skill));
    }

    @Transactional
    public SkillGapReportDTO saveReport(User user, SkillGapResultDTO result, String pdfPath) {
        SkillTreeTemplate template = roadmapService.resolveTemplate(user, result.getTemplate().getId());
        SkillGapReport report = SkillGapReport.builder()
                .user(user)
                .template(template)
                .summary(buildSummary(result))
                .pdfPath(pdfPath)
                .requiredSkillCount(result.getRequiredSkillCount())
                .matchedSkillCount(result.getMatchedSkillCount())
                .missingSkillCount(result.getMissingSkillCount())
                .matchedSkillNames(joinSkillNames(result.getMatchedSkills()))
                .missingSkillNames(joinSkillNames(result.getMissingSkills()))
                .build();

        return SkillGapReportDTO.from(skillGapReportRepository.save(report));
    }

    @Transactional(readOnly = true)
    public List<SkillGapReportDTO> getReports(User user) {
        return skillGapReportRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(SkillGapReportDTO::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillGapReportDTO getReport(User user, Long reportId) {
        return skillGapReportRepository.findByIdAndUser(reportId, user)
                .map(SkillGapReportDTO::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy báo cáo skill gap"));
    }

    private SkillSummaryDTO toSkillSummary(Skill skill) {
        return SkillSummaryDTO.builder()
                .id(skill.getId())
                .name(skill.getName())
                .category(skill.getCategory())
                .build();
    }

    private SkillNode createSupplementalRoadmapNode(SkillTreeTemplate template, Skill skill) {
        return skillNodeRepository.save(SkillNode.builder()
                .template(template)
                .skill(skill)
                .title(skill.getName())
                .description("Kỹ năng bổ sung từ Skill Gap.")
                .tier(1)
                .orderIndex(nextOrderIndex(template.getId()))
                .requiredLevel(1)
                .customNode(true)
                .build());
    }

    private int nextOrderIndex(Long templateId) {
        return skillNodeRepository.findTopByTemplate_IdOrderByOrderIndexDesc(templateId)
                .map(node -> node.getOrderIndex() == null ? 1 : node.getOrderIndex() + 1)
                .orElse(0);
    }

    private String buildSummary(SkillGapResultDTO result) {
        return "Đã đạt " + result.getMatchedSkillCount() + "/" + result.getRequiredSkillCount()
                + " kỹ năng, còn thiếu " + result.getMissingSkillCount() + " kỹ năng.";
    }

    private String joinSkillNames(List<SkillSummaryDTO> skills) {
        return skills.stream()
                .map(SkillSummaryDTO::getName)
                .collect(Collectors.joining(", "));
    }
}
