package vn.uth.careercompass.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapNodeDTO {
    private Long id;
    private Long parentId;
    private Long skillId;
    private String skillName;
    private String title;
    private String description;
    private Integer tier;
    private Integer orderIndex;
    private Integer requiredLevel;
    private ProgressStatus status;
}
