package vn.uth.careercompass.roadmap.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.dto.SkillSummaryDTO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test cho {@link PdfService}.
 *
 * <p>PHẠM VI: PdfService sinh file PDF nhị phân bằng OpenPDF. Việc kiểm TỪNG BYTE của PDF
 * không có ý nghĩa cho unit test (đó là trách nhiệm của thư viện OpenPDF). Vì vậy ta:
 *  <ul>
 *    <li>Test LOGIC THUẦN {@code skillLines} / {@code safe} (định dạng chuỗi, guard rỗng/null)
 *        — tách được, quyết định nội dung hiển thị trong PDF.</li>
 *    <li>Test happy-path {@code generateSkillGapReport}: ghi file vào thư mục tạm, kiểm
 *        đường dẫn/tên file (chứa userId, đuôi .pdf) và file THỰC SỰ được tạo — tức kiểm
 *        "service thu thập & lưu ở đâu", KHÔNG assert nội dung byte.</li>
 *  </ul>
 *
 * <p>Ghi chú SKIP: không assert nội dung nhị phân PDF (font, layout) — nằm ngoài phạm vi unit test.
 */
class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    // ============================================================
    // skillLines(List) — logic thuần (gọi qua reflection vì là private)
    // ============================================================

    @Test
    void skillLines_whenEmpty_returnsPlaceholder() {
        // Danh sách rỗng -> hiển thị "— Không có —" thay vì để trống
        String result = ReflectionTestUtils.invokeMethod(pdfService, "skillLines", List.of());
        assertThat(result).isEqualTo("— Không có —");
    }

    @Test
    void skillLines_whenNull_returnsPlaceholder() {
        // Null cũng phải an toàn -> cùng placeholder (guard null || isEmpty)
        List<SkillSummaryDTO> nullList = null;
        String result = ReflectionTestUtils.invokeMethod(pdfService, "skillLines", nullList);
        assertThat(result).isEqualTo("— Không có —");
    }

    @Test
    void skillLines_formatsEachSkillWithBulletAndCategory() {
        // Given: 2 skill, 1 có category, 1 không
        List<SkillSummaryDTO> skills = List.of(
                SkillSummaryDTO.builder().name("Java").category("Language").build(),
                SkillSummaryDTO.builder().name("Docker").build());

        // When
        String result = ReflectionTestUtils.invokeMethod(pdfService, "skillLines", skills);

        // Then: mỗi dòng có bullet; skill có category thì kèm "(...)"; kết quả đã trim
        assertThat(result).isEqualTo("• Java  (Language)\n• Docker");
    }

    // ============================================================
    // safe(String) — guard null -> ""
    // ============================================================

    @Test
    void safe_convertsNullToEmptyAndKeepsValue() {
        assertThat((String) ReflectionTestUtils.invokeMethod(pdfService, "safe", (Object) null)).isEmpty();
        assertThat((String) ReflectionTestUtils.invokeMethod(pdfService, "safe", "hello")).isEqualTo("hello");
    }

    // ============================================================
    // generateSkillGapReport — happy path: tạo file PDF vào thư mục tạm
    // ============================================================

    @Test
    void generateSkillGapReport_writesPdfFileAndReturnsPath(@TempDir Path tempDir) {
        // Given: cấu hình thư mục xuất báo cáo = thư mục tạm của test (@Value bị inject tay)
        ReflectionTestUtils.setField(pdfService, "reportDirectory", tempDir.toString());
        User user = User.builder().id(42L).fullName("Nguyen Van A").email("a@uth.edu.vn").build();
        SkillGapResultDTO result = SkillGapResultDTO.builder()
                .template(RoadmapTemplateDTO.builder().name("Backend").build())
                .requiredSkillCount(3).matchedSkillCount(2).missingSkillCount(1).matchPercent(66.67)
                .matchedSkills(List.of(SkillSummaryDTO.builder().name("Java").category("Language").build()))
                .missingSkills(List.of(SkillSummaryDTO.builder().name("Docker").build()))
                .build();

        // When
        String path = pdfService.generateSkillGapReport(user, result);

        // Then: đường dẫn chứa userId + đuôi .pdf, và file thực sự tồn tại trên đĩa
        assertThat(path).contains("skill-gap-user-42-").endsWith(".pdf");
        assertThat(Files.exists(Path.of(path))).isTrue();
    }
}
