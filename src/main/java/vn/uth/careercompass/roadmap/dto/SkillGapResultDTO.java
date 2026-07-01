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
public class SkillGapResultDTO {
    private RoadmapTemplateDTO template;
    private Integer requiredSkillCount;
    private Integer matchedSkillCount;
    private Integer missingSkillCount;
    private Double matchPercent;
    private List<SkillSummaryDTO> matchedSkills;
    private List<SkillSummaryDTO> missingSkills;
}
