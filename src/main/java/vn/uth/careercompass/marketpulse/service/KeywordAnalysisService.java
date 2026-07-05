package vn.uth.careercompass.marketpulse.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.marketpulse.dto.KeywordStatDTO;
import vn.uth.careercompass.marketpulse.entity.JobTrend;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class KeywordAnalysisService {

    private final JobTrendRepository jobTrendRepository;

    /** Danh sách công nghệ theo dõi — mở rộng dần, có thể chuyển ra DB/config sau. */
    private static final List<String> TRACKED_KEYWORDS = List.of(
            "Java", "Spring Boot", "React", "Python", "Docker", "Kubernetes",
            "AWS", "SQL", "Node.js", "TypeScript", "CI/CD", "Microservices"
    );

    /** Đếm số JD nhắc tới từng keyword trong N ngày gần nhất, lấy top `limit`. */
    public List<KeywordStatDTO> topKeywords(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<JobTrend> trends = jobTrendRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);

        Map<String, Integer> counter = new HashMap<>();
        for (String keyword : TRACKED_KEYWORDS) {
            Pattern pattern = Pattern.compile(Pattern.quote(keyword), Pattern.CASE_INSENSITIVE);
            int count = 0;
            for (JobTrend trend : trends) {
                if (trend.getRawDescription() != null && pattern.matcher(trend.getRawDescription()).find()) {
                    count++;
                }
            }
            if (count > 0) counter.put(keyword, count);
        }

        return counter.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(e -> KeywordStatDTO.builder().keyword(e.getKey()).count(e.getValue()).build())
                .toList();
    }
}