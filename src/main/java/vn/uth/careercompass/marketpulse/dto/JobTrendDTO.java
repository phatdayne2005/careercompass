package vn.uth.careercompass.marketpulse.dto;

import lombok.Builder;
import lombok.Getter;
import vn.uth.careercompass.marketpulse.entity.JobTrend;

import java.time.LocalDateTime;

@Getter
@Builder
public class JobTrendDTO {
    private Long id;
    private String source;
    private String jobTitle;
    private String company;
    private LocalDateTime createdAt;

    public static JobTrendDTO from(JobTrend trend) {
        return JobTrendDTO.builder()
                .id(trend.getId())
                .source(trend.getSource())
                .jobTitle(trend.getJobTitle())
                .company(trend.getCompany())
                .createdAt(trend.getCreatedAt())
                .build();
    }
}