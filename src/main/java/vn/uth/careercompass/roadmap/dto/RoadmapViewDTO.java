package vn.uth.careercompass.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapViewDTO {
    private RoadmapTemplateDTO template;
    private Integer totalNodes;
    private Integer completedNodes;
    private Double completionPercent;
    private List<RoadmapNodeDTO> nodes;
}
