package vn.uth.careercompass.marketpulse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.marketpulse.dto.JobTrendDTO;
import vn.uth.careercompass.marketpulse.dto.MarketPulseViewDTO;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

@Service
@RequiredArgsConstructor
public class MarketPulseService {

    private final JobTrendRepository jobTrendRepository;
    private final KeywordAnalysisService keywordAnalysisService;

    public MarketPulseViewDTO getMarketPulse() {
        var topKeywords = keywordAnalysisService.topKeywords(30, 10);
        var recentJobs = jobTrendRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(JobTrendDTO::from)
                .limit(10)
                .toList();

        return MarketPulseViewDTO.builder()
                .topKeywords(topKeywords)
                .recentJobs(recentJobs)
                .totalJobsScraped((int) jobTrendRepository.count())
                .build();
    }
}