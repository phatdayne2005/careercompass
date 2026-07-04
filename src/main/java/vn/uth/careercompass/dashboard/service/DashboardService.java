package vn.uth.careercompass.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.dashboard.dto.DashboardDTO;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.ActivityLogRepository;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.service.RoadmapService;
import vn.uth.careercompass.roadmap.service.SkillGapService;

/**
 * Tổng hợp dữ liệu cho Dashboard (Màn ③) của P5 — gói ĐỌC từ các gói khác:
 * P4 (tiến độ roadmap + skill gap) và kernel (ActivityLog). Không sở hữu entity riêng.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RoadmapService roadmapService;
    private final SkillGapService skillGapService;
    private final ActivityLogRepository activityLogRepository;

    @Transactional(readOnly = true)
    public DashboardDTO buildDashboard(User user) {
        DashboardDTO.DashboardDTOBuilder builder = DashboardDTO.builder();

        // Tiến độ roadmap + kỹ năng kế tiếp (từ P4)
        try {
            RoadmapViewDTO roadmap = roadmapService.getRoadmap(user, null);
            builder.progressPercent(roadmap.getCompletionPercent())
                    .completedNodes(roadmap.getCompletedNodes())
                    .totalNodes(roadmap.getTotalNodes())
                    .roadmapTemplateId(roadmap.getTemplate().getId());

            roadmap.getNodes().stream()
                    .filter(node -> !ProgressStatus.DONE.equals(node.getStatus()))
                    .findFirst()
                    .ifPresent(node -> builder.nextSkillName(node.getSkillName()).nextNodeId(node.getId()));
        } catch (Exception e) {
            // Chưa có roadmap khả dụng → giữ mặc định 0
        }

        // Skill gap (từ P4)
        try {
            SkillGapResultDTO gap = skillGapService.analyze(user, null);
            builder.matchedSkills(gap.getMatchedSkillCount())
                    .missingSkills(gap.getMissingSkillCount());
        } catch (Exception e) {
            // Chưa có dữ liệu → 0
        }

        // Hoạt động gần đây (từ kernel ActivityLog) — 8 mục mới nhất
        builder.recentActivities(activityLogRepository
                .findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 8))
                .getContent());

        return builder.build();
    }
}
