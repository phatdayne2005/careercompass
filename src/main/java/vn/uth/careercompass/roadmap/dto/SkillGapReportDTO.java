package vn.uth.careercompass.roadmap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.uth.careercompass.roadmap.entity.SkillGapReport;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapReportDTO {
    private Long id;
    private RoadmapTemplateDTO template;
    private String summary;
    private String pdfPath;
    private Integer requiredSkillCount;
    private Integer matchedSkillCount;
    private Integer missingSkillCount;
    private String matchedSkillNames;
    private String missingSkillNames;
    private LocalDateTime createdAt;

    public static SkillGapReportDTO from(SkillGapReport report) {
        return SkillGapReportDTO.builder()
                .id(report.getId())
                .template(RoadmapTemplateDTO.from(report.getTemplate()))
                .summary(report.getSummary())
                .pdfPath(report.getPdfPath())
                .requiredSkillCount(report.getRequiredSkillCount())
                .matchedSkillCount(report.getMatchedSkillCount())
                .missingSkillCount(report.getMissingSkillCount())
                .matchedSkillNames(report.getMatchedSkillNames())
                .missingSkillNames(report.getMissingSkillNames())
                .createdAt(report.getCreatedAt())
                .build();
    }
}
