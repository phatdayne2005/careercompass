package vn.uth.careercompass.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import vn.uth.careercompass.kernel.entity.ActivityLog;

import java.util.List;

/**
 * Dữ liệu tổng hợp cho trang Dashboard (Màn ③) của P5.
 * Gộp từ P4 (tiến độ roadmap + skill gap) và kernel (ActivityLog).
 */
@Getter
@Builder
public class DashboardDTO {
    private double progressPercent;   // % node đã hoàn thành
    private int completedNodes;
    private int totalNodes;

    private int matchedSkills;        // skill đã đạt (khớp yêu cầu role)
    private int missingSkills;        // skill còn thiếu

    // "Kỹ năng kế tiếp" (node chưa hoàn thành đầu tiên) — để mark-complete nhanh
    private String nextSkillName;
    private Long nextNodeId;
    private Long roadmapTemplateId;

    private List<ActivityLog> recentActivities;
}
