package vn.uth.careercompass.dashboard.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DashboardViewDTO {
    private double roadmapCompletionPercent;
    private int completedNodes;
    private int totalNodes;
    private int matchedSkillCount;
    private int missingSkillCount;
    private List<ActivityLogDTO> recentActivities;
}