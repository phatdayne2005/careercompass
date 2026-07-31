package vn.uth.careercompass.marketpulse.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import vn.uth.careercompass.marketpulse.entity.JobTrend;
import vn.uth.careercompass.marketpulse.repository.JobTrendRepository;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link ScraperService}.
 *
 * <p>GHI CHÚ VỀ I/O: bản thân việc gọi mạng tới The Muse KHÔNG unit-test được. Nhưng service
 * gọi mạng QUA một {@link RestTemplate} là field của class — nên ta MOCK RestTemplate rồi
 * "tiêm tay" bằng {@link ReflectionTestUtils#setField} (vì nó khởi tạo bằng {@code new}, không
 * qua constructor nên @InjectMocks không chạm tới). Nhờ vậy vẫn test được TOÀN BỘ logic thuần:
 * lọc tin phần mềm, bóc thẻ HTML, cắt độ dài, và quy tắc thay/không-thay dữ liệu.
 *
 * <p>Lưu ý PAGES=4: mỗi lần scrape gọi exchange() 4 lần (trang 1..4). Ta dùng consecutive
 * stubbing: trả dữ liệu ở lần gọi ĐẦU, các lần sau trả body rỗng để không nhân đôi kết quả.
 */
@ExtendWith(MockitoExtension.class)
class ScraperServiceTest {

    @Mock
    private JobTrendRepository jobTrendRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ScraperService scraperService;

    @BeforeEach
    void setUp() {
        // RestTemplate là field khởi tạo bằng `new` trong service -> @InjectMocks bỏ qua.
        // Ghi đè bằng mock để chặn lời gọi mạng thật.
        ReflectionTestUtils.setField(scraperService, "restTemplate", restTemplate);
    }

    /** Tạo 1 "item" job kiểu Map giống JSON trả về từ The Muse. */
    private Map<String, Object> item(String name, String companyName, String contents) {
        Map<String, Object> m = new HashMap<>();
        if (name != null) {
            m.put("name", name);
        }
        if (companyName != null) {
            Map<String, Object> co = new HashMap<>();
            co.put("name", companyName);
            m.put("company", co);
        }
        if (contents != null) {
            m.put("contents", contents);
        }
        return m;
    }

    /** Bọc list item thành ResponseEntity giống body The Muse ({"results": [...]}) . */
    private ResponseEntity<Map> pageWith(List<Map<String, Object>> results) {
        Map<String, Object> body = new HashMap<>();
        body.put("results", results);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    /** Một trang rỗng (không có key "results") -> nhánh results==null -> continue. */
    private ResponseEntity<Map> emptyPage() {
        return new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
    }

    @SuppressWarnings("unchecked")
    private void stubExchange(ResponseEntity<Map> first, ResponseEntity<Map> rest) {
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenReturn(first, rest);
    }

    // ============================================================
    // Happy path: lọc đúng tin phần mềm, bóc HTML, map field, THAY dữ liệu cũ
    // ============================================================
    @Test
    void scrapeJobs_keepsSoftwareRolesStripsHtmlAndReplacesData() {
        // Given: trang 1 có 4 item:
        //  - "Senior Software Engineer": khớp "software" -> GIỮ
        //  - name=null                : bỏ (nhánh nameObj==null)
        //  - "Marketing Manager"      : không tín hiệu phần mềm -> LỌC bỏ
        //  - "Backend Developer" (không company, không contents): khớp "backend"/"developer" -> GIỮ
        List<Map<String, Object>> results = List.of(
                item("Senior Software Engineer", "Acme", "<p>We use Java &amp; Spring</p>"),
                item(null, "Ghost", "irrelevant"),
                item("Marketing Manager", "BizCo", "sales stuff"),
                item("Backend Developer", null, null)
        );
        // Trang 1 có dữ liệu; trang 2..4 rỗng để không nhân bản.
        stubExchange(pageWith(results), emptyPage());

        // When
        int saved = scraperService.scrapeJobs();

        // Then: 2 job hợp lệ được lưu; dữ liệu cũ bị xoá trước (deleteAll) rồi saveAll.
        assertThat(saved).isEqualTo(2);
        verify(jobTrendRepository).deleteAll();

        ArgumentCaptor<List<JobTrend>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobTrendRepository).saveAll(captor.capture());
        List<JobTrend> persisted = captor.getValue();
        assertThat(persisted).hasSize(2);

        // Job 1: nguồn cố định, company lấy từ map, HTML bị bóc, khoảng trắng gộp lại.
        JobTrend j1 = persisted.get(0);
        assertThat(j1.getSource()).isEqualTo("THEMUSE");
        assertThat(j1.getJobTitle()).isEqualTo("Senior Software Engineer");
        assertThat(j1.getCompany()).isEqualTo("Acme");
        assertThat(j1.getRawDescription()).isEqualTo("Senior Software Engineer We use Java &amp; Spring");
        assertThat(j1.getRawDescription()).doesNotContain("<p>", "</p>"); // thẻ HTML đã bị loại

        // Job 2: company thiếu -> "" (nhánh else); contents thiếu -> desc rỗng.
        JobTrend j2 = persisted.get(1);
        assertThat(j2.getJobTitle()).isEqualTo("Backend Developer");
        assertThat(j2.getCompany()).isEmpty();
        assertThat(j2.getRawDescription()).isEqualTo("Backend Developer");

        // Xác nhận có gọi exchange đúng PAGES=4 lần.
        verify(restTemplate, times(4))
                .exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
    }

    // ============================================================
    // Cào rỗng (không tin nào khớp) -> trả 0, KHÔNG xoá dữ liệu cũ
    // ============================================================
    @Test
    void scrapeJobs_whenNoResults_returnsZeroAndKeepsOldData() {
        // Given: mọi trang đều rỗng.
        stubExchange(emptyPage(), emptyPage());

        // When
        int saved = scraperService.scrapeJobs();

        // Then: fresh rỗng -> return 0; TUYỆT ĐỐI không deleteAll/saveAll (bảo toàn data cũ).
        assertThat(saved).isZero();
        verify(jobTrendRepository, never()).deleteAll();
        verify(jobTrendRepository, never()).saveAll(any());
    }

    // ============================================================
    // Tất cả tin đều bị lọc (không phải phần mềm) -> vẫn là 0
    // ============================================================
    @Test
    void scrapeJobs_whenAllRolesFilteredOut_returnsZero() {
        // Given: 2 tin phi phần mềm.
        List<Map<String, Object>> results = List.of(
                item("Mechanical Engineer", "SpaceX", "hardware"),
                item("HR Manager", "BizCo", "recruiting")
        );
        stubExchange(pageWith(results), emptyPage());

        // When
        int saved = scraperService.scrapeJobs();

        // Then: không tin nào qua bộ lọc -> 0, không đụng DB.
        assertThat(saved).isZero();
        verify(jobTrendRepository, never()).deleteAll();
        verify(jobTrendRepository, never()).saveAll(any());
    }

    // ============================================================
    // Lỗi mạng (exchange ném exception) -> nuốt lỗi, trả 0, giữ data cũ
    // ============================================================
    @Test
    @SuppressWarnings("unchecked")
    void scrapeJobs_whenHttpFails_returnsZeroAndKeepsOldData() {
        // Given: exchange ném lỗi ngay lần gọi đầu -> vào catch.
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("connection reset"));

        // When
        int saved = scraperService.scrapeJobs();

        // Then: catch trả 0; không xoá/lưu gì.
        assertThat(saved).isZero();
        verify(jobTrendRepository, never()).deleteAll();
        verify(jobTrendRepository, never()).saveAll(any());
    }

    // ============================================================
    // Cắt độ dài (cap): tiêu đề dài hơn 255 phải bị cắt còn 255
    // ============================================================
    @Test
    void scrapeJobs_capsOverlongTitleTo255() {
        // Given: tiêu đề rất dài nhưng vẫn chứa tín hiệu "software" để không bị lọc.
        String longTitle = "software " + "x".repeat(400); // > 255
        List<Map<String, Object>> results = List.of(item(longTitle, "Acme", "desc"));
        stubExchange(pageWith(results), emptyPage());

        // When
        scraperService.scrapeJobs();

        // Then: jobTitle bị cap(...,255) -> đúng 255 ký tự.
        ArgumentCaptor<List<JobTrend>> captor = ArgumentCaptor.forClass(List.class);
        verify(jobTrendRepository).saveAll(captor.capture());
        assertThat(captor.getValue().get(0).getJobTitle()).hasSize(255);
    }
}
