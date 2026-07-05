package vn.uth.careercompass.dashboard.dto;

import lombok.Builder;
import lombok.Getter;
import vn.uth.careercompass.kernel.entity.ActivityLog;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActivityLogDTO {
    private String type;
    private String description;
    private LocalDateTime createdAt;

    public static ActivityLogDTO from(ActivityLog log) {
        return ActivityLogDTO.builder()
                .type(log.getType())
                .description(log.getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }
}