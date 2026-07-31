package vn.uth.careercompass.marketpulse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.uth.careercompass.marketpulse.dto.KeywordStatDTO;
import vn.uth.careercompass.marketpulse.dto.MarketPulseViewDTO;
import vn.uth.careercompass.marketpulse.entity.JobTrend;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link MarketPulseService}.
 *
 * <p>Service này là "orchestrator" mỏng: gộp kết quả từ {@link KeywordAnalysisService}
 * và {@link JobTrendRepository} thành 1 DTO cho view. Ta mock cả hai dependency và
 * kiểm: (1) truyền đúng tham số, (2) map/limit đúng, (3) tổng số job = count().
 */
@ExtendWith(MockitoExtension.class)
class MarketPulseServiceTest {

    @Mock
    private JobTrendRepository jobTrendRepository;

    @Mock
    private KeywordAnalysisService keywordAnalysisService;

    @InjectMocks
    private MarketPulseService marketPulseService;

    /** Tạo N JobTrend giả, mỗi cái có id + tiêu đề riêng để phân biệt khi map sang DTO. */
    private List<JobTrend> makeJobs(int n) {
        List<JobTrend> list = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            list.add(JobTrend.builder()
                    .id((long) i)
                    .source("THEMUSE")
                    .jobTitle("Job " + i)
                    .company("Company " + i)
                    .build());
        }
        return list;
    }

    // ============================================================
    // Happy path: gộp keyword + recent jobs + tổng số
    // ============================================================
    @Test
    void getMarketPulse_assemblesKeywordsRecentJobsAndTotal() {
        // Given: keyword service trả 2 thống kê.
        List<KeywordStatDTO> keywords = List.of(
                KeywordStatDTO.builder().keyword("Java").count(9).build(),
                KeywordStatDTO.builder().keyword("React").count(5).build()
        );
        when(keywordAnalysisService.topKeywords(30, 10)).thenReturn(keywords);
        // repo trả 3 job gần nhất.
        when(jobTrendRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(makeJobs(3));
        // tổng số job trong DB.
        when(jobTrendRepository.count()).thenReturn(123L);

        // When
        MarketPulseViewDTO view = marketPulseService.getMarketPulse();

        // Then
        assertThat(view.getTopKeywords()).isSameAs(keywords);          // truyền thẳng list keyword
        assertThat(view.getRecentJobs()).hasSize(3);                   // 3 job map sang DTO
        assertThat(view.getRecentJobs().get(0).getJobTitle()).isEqualTo("Job 1"); // giữ nguyên thứ tự
        assertThat(view.getTotalJobsScraped()).isEqualTo(123);         // ép long->int từ count()

        // Xác nhận gọi keyword service đúng tham số (30 ngày, top 10).
        verify(keywordAnalysisService).topKeywords(30, 10);
    }

    // ============================================================
    // recentJobs bị cắt tối đa 10 phần tử (dù repo trả 50)
    // ============================================================
    @Test
    void getMarketPulse_limitsRecentJobsToTen() {
        // Given: repo trả 50 job, nhưng view chỉ hiển thị 10 job gần nhất.
        when(keywordAnalysisService.topKeywords(30, 10)).thenReturn(List.of());
        when(jobTrendRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(makeJobs(50));
        when(jobTrendRepository.count()).thenReturn(50L);

        // When
        MarketPulseViewDTO view = marketPulseService.getMarketPulse();

        // Then: .limit(10) trong stream -> đúng 10 phần tử.
        assertThat(view.getRecentJobs()).hasSize(10);
        assertThat(view.getRecentJobs().get(9).getJobTitle()).isEqualTo("Job 10");
    }

    // ============================================================
    // DB rỗng: mọi thứ rỗng/0
    // ============================================================
    @Test
    void getMarketPulse_whenEmptyDatabase_returnsEmptyView() {
        // Given
        when(keywordAnalysisService.topKeywords(30, 10)).thenReturn(List.of());
        when(jobTrendRepository.findTop50ByOrderByCreatedAtDesc()).thenReturn(List.of());
        when(jobTrendRepository.count()).thenReturn(0L);

        // When
        MarketPulseViewDTO view = marketPulseService.getMarketPulse();

        // Then
        assertThat(view.getTopKeywords()).isEmpty();
        assertThat(view.getRecentJobs()).isEmpty();
        assertThat(view.getTotalJobsScraped()).isZero();
    }
}
