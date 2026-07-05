package vn.uth.careercompass.dashboard.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.dashboard.dto.ActivityLogDTO;
import vn.uth.careercompass.dashboard.dto.DashboardViewDTO;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.ActivityLogRepository;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.service.RoadmapService;
import vn.uth.careercompass.roadmap.service.SkillGapService;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RoadmapService roadmapService;
    private final SkillGapService skillGapService;
    private final ActivityLogRepository activityLogRepository;

    public DashboardViewDTO getDashboard(User user) {
        // % tiến độ + số node — đọc UserNodeProgress qua RoadmapService (P4) có sẵn
        RoadmapViewDTO roadmap = roadmapService.getRoadmap(user, null);

        // Đếm skill gap — tái dùng SkillGapService (P4), cùng template đang xem
        SkillGapResultDTO skillGap = skillGapService.analyze(user, roadmap.getTemplate().getId());

        // 5 hoạt động gần nhất — đọc ActivityLog (Kernel)
        var recentActivities = activityLogRepository
                .findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(ActivityLogDTO::from)
                .toList();

        return DashboardViewDTO.builder()
                .roadmapCompletionPercent(roadmap.getCompletionPercent())
                .completedNodes(roadmap.getCompletedNodes())
                .totalNodes(roadmap.getTotalNodes())
                .matchedSkillCount(skillGap.getMatchedSkillCount())
                .missingSkillCount(skillGap.getMissingSkillCount())
                .recentActivities(recentActivities)
                .build();
    }
}