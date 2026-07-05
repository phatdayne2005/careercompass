package vn.uth.careercompass.marketpulse.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MarketPulseViewDTO {
    private List<KeywordStatDTO> topKeywords;   // dữ liệu cho biểu đồ cột Chart.js
    private List<JobTrendDTO> recentJobs;
    private int totalJobsScraped;
}