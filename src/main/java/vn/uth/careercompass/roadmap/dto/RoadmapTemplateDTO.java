package vn.uth.careercompass.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoadmapTemplateDTO {
    private Long id;
    private String name;
    private String description;
    private Long targetRoleId;

    public static RoadmapTemplateDTO from(SkillTreeTemplate template) {
        return RoadmapTemplateDTO.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .targetRoleId(template.getTargetRoleId())
                .build();
    }
}
