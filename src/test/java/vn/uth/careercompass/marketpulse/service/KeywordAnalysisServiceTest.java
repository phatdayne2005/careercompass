package vn.uth.careercompass.marketpulse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.uth.careercompass.marketpulse.dto.KeywordStatDTO;
import vn.uth.careercompass.marketpulse.entity.JobTrend;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link KeywordAnalysisService}.
 *
 * <p>Đây là service có logic THẬT (không chỉ gọi repo rồi trả về): nó đọc danh sách JD,
 * đếm tần suất từng công nghệ trong TRACKED_KEYWORDS, rồi sắp xếp giảm dần + cắt top N.
 * Vì vậy ta test kỹ phần đếm/sắp xếp bằng dữ liệu đầu vào cụ thể — chỉ cần mock repo trả list JD.
 *
 * <p>WHY mock repo: ta không quan tâm SQL/DB, chỉ nạp sẵn 1 tập JD trong bộ nhớ rồi kiểm
 * thuật toán đếm cho ra đúng con số + đúng thứ tự.
 */
@ExtendWith(MockitoExtension.class)
class KeywordAnalysisServiceTest {

    @Mock
    private JobTrendRepository jobTrendRepository;

    @InjectMocks
    private KeywordAnalysisService keywordAnalysisService;

    /** Helper nhỏ tạo 1 JobTrend chỉ với rawDescription — phần duy nhất mà thuật toán đọc. */
    private JobTrend jd(String rawDescription) {
        return JobTrend.builder().rawDescription(rawDescription).build();
    }

    // ============================================================
    // Đếm tần suất + sắp xếp giảm dần
    // ============================================================
    @Test
    void topKeywords_countsOccurrencesAndSortsDescending() {
        // Given: 4 JD. Đếm THEO SỐ JD nhắc tới keyword (mỗi JD tối đa +1, dù lặp nhiều lần).
        //  - "Java"  : JD1, JD2, JD3            -> 3
        //  - "Spring Boot": JD1, JD2            -> 2
        //  - "React" : JD4                      -> 1
        //  - "Python": JD3                      -> 1
        List<JobTrend> trends = List.of(
                jd("We build with Java and Spring Boot every day, Java Java"), // Java đếm 1 lần cho JD này
                jd("Java backend using Spring Boot"),
                jd("Python data pipelines and some Java scripting"),
                jd("Frontend with React")
        );
        // Bất kể tham số days là gì, ta trả nguyên list này (đã mock nên since không tác động).
        when(jobTrendRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                .thenReturn(trends);

        // When
        List<KeywordStatDTO> result = keywordAnalysisService.topKeywords(30, 10);

        // Then: keyword có count>0 mới xuất hiện; sắp giảm dần theo count.
        // Java(3) đứng đầu, kế đến Spring Boot(2); React & Python cùng 1.
        assertThat(result).extracting(KeywordStatDTO::getKeyword)
                .containsExactly("Java", "Spring Boot", "React", "Python"); // đúng thứ tự đầu
        assertThat(result).extracting(KeywordStatDTO::getCount)
                .containsExactly(3, 2, 1, 1);
        // Các keyword không xuất hiện (Docker, AWS...) bị loại vì count==0.
        assertThat(result).extracting(KeywordStatDTO::getKeyword)
                .doesNotContain("Docker", "AWS", "Kubernetes");
    }

    // ============================================================
    // Đếm KHÔNG phân biệt hoa/thường (CASE_INSENSITIVE)
    // ============================================================
    @Test
    void topKeywords_isCaseInsensitive() {
        // Given: "JAVA", "java", "JaVa" đều phải khớp keyword "Java".
        List<JobTrend> trends = List.of(
                jd("JAVA developer"),
                jd("senior java engineer"),
                jd("JaVa spring")
        );
        when(jobTrendRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                .thenReturn(trends);

        // When
        List<KeywordStatDTO> result = keywordAnalysisService.topKeywords(7, 10);

        // Then: cả 3 JD đều tính -> Java = 3.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getKeyword()).isEqualTo("Java");
        assertThat(result.get(0).getCount()).isEqualTo(3);
    }

    // ============================================================
    // Bỏ qua JD có rawDescription == null (nhánh null-check)
    // ============================================================
    @Test
    void topKeywords_ignoresNullDescriptions() {
        // Given: 1 JD null (bị bỏ qua), 1 JD hợp lệ chứa Docker.
        List<JobTrend> trends = List.of(
                jd(null),               // nhánh trend.getRawDescription()==null -> không đếm, không NPE
                jd("Docker and Kubernetes stack")
        );
        when(jobTrendRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                .thenReturn(trends);

        // When
        List<KeywordStatDTO> result = keywordAnalysisService.topKeywords(30, 10);

        // Then: chỉ Docker(1) và Kubernetes(1); JD null không gây lỗi.
        assertThat(result).extracting(KeywordStatDTO::getKeyword)
                .containsExactlyInAnyOrder("Docker", "Kubernetes");
        assertThat(result).allMatch(s -> s.getCount() == 1);
    }

    // ============================================================
    // Tôn trọng tham số `limit` (cắt top N sau khi sắp xếp)
    // ============================================================
    @Test
    void topKeywords_respectsLimit() {
        // Given: JD chứa nhiều keyword khác nhau, count đều = 1.
        List<JobTrend> trends = List.of(
                jd("Java Python React Docker AWS SQL") // 6 keyword, mỗi cái đúng 1 JD
        );
        when(jobTrendRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                .thenReturn(trends);

        // When: chỉ lấy top 2.
        List<KeywordStatDTO> result = keywordAnalysisService.topKeywords(30, 2);

        // Then: dù 6 keyword khớp, limit=2 nên chỉ trả 2 phần tử.
        assertThat(result).hasSize(2);
    }

    // ============================================================
    // Không có JD nào -> trả list rỗng
    // ============================================================
    @Test
    void topKeywords_whenNoJobs_returnsEmpty() {
        // Given: repo trả rỗng.
        when(jobTrendRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                .thenReturn(List.of());

        // When
        List<KeywordStatDTO> result = keywordAnalysisService.topKeywords(30, 10);

        // Then: không keyword nào có count>0 -> rỗng.
        assertThat(result).isEmpty();
    }

    // ============================================================
    // Keyword đặc biệt regex ("Node.js", "CI/CD", "C++"-style) khớp literal, không phải regex
    // ============================================================
    @Test
    void topKeywords_matchesSpecialCharKeywordsLiterally() {
        // WHY test này: keyword "Node.js" có dấu '.' — nếu code dùng regex thô, '.' sẽ khớp
        // MỌI ký tự. Service dùng Pattern.quote() nên phải khớp ĐÚNG chuỗi "Node.js".
        List<JobTrend> trends = List.of(
                jd("We use Node.js and CI/CD pipelines")
        );
        when(jobTrendRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                .thenReturn(trends);

        // When
        List<KeywordStatDTO> result = keywordAnalysisService.topKeywords(30, 10);

        // Then: cả "Node.js" và "CI/CD" đều khớp literal, count=1.
        assertThat(result).extracting(KeywordStatDTO::getKeyword)
                .containsExactlyInAnyOrder("Node.js", "CI/CD");
    }

    // ============================================================
    // Xác nhận có gọi repo với mốc thời gian trong quá khứ (days ngày trước)
    // ============================================================
    @Test
    void topKeywords_queriesRepositoryWithPastCutoff() {
        // Given
        when(jobTrendRepository.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                .thenReturn(List.of());
        LocalDateTime beforeCall = LocalDateTime.now().minusDays(30);

        // When
        keywordAnalysisService.topKeywords(30, 10);

        // Then: mốc `since` phải là quá khứ (khoảng now - 30 ngày). Ta chỉ kiểm nó không ở tương lai.
        verify(jobTrendRepository).findByCreatedAtAfterOrderByCreatedAtDesc(
                org.mockito.ArgumentMatchers.argThat(since ->
                        since != null && !since.isAfter(beforeCall.plusDays(30))));
    }
}
