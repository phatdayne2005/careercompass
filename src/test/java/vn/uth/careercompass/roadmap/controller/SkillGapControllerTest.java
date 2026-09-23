package vn.uth.careercompass.roadmap.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapReportDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.service.PdfService;
import vn.uth.careercompass.roadmap.service.SkillGapService;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử REST API phân tích khoảng cách kỹ năng (API-04 → API-07, FR3.2 và FR3.3).
 *
 * Bốn endpoint này bao phủ 0% ở tầng đơn vị trước lớp này. Logic riêng của controller là
 * cách xử lý THÂN REQUEST VẮNG MẶT: cả /analyze lẫn /reports đều khai
 * @RequestBody(required = false), nên request không kèm JSON sẽ nhận request == null và
 * controller phải tự đổi thành templateId null thay vì ném NullPointerException. Đó chính
 * là bốn nhánh chưa được phủ, và là kiểu lỗi chỉ lộ ra khi máy khách gửi POST rỗng.
 */
@WebMvcTest(value = SkillGapController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
@DisplayName("FR3.2 / FR3.3 — REST API khoảng cách kỹ năng")
class SkillGapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SkillGapService skillGapService;

    @MockitoBean
    private PdfService pdfService;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    private static final User NGUOI_DUNG = User.builder()
            .id(1L).email("student@uth.edu.vn").fullName("Nguyen Van A").build();

    private static final RoadmapTemplateDTO TEMPLATE = RoadmapTemplateDTO.builder()
            .id(10L).name("Lộ trình Backend").targetRoleId(1L).build();

    private static final SkillGapResultDTO KET_QUA = SkillGapResultDTO.builder()
            .template(TEMPLATE)
            .requiredSkillCount(10)
            .matchedSkillCount(4)
            .missingSkillCount(6)
            .matchPercent(40.0)
            .matchedSkills(List.of())
            .missingSkills(List.of())
            .build();

    private static SkillGapReportDTO baoCao(long id) {
        return SkillGapReportDTO.builder()
                .id(id).template(TEMPLATE).summary("Thiếu 6 kỹ năng")
                .pdfPath("reports/skill-gap-" + id + ".pdf")
                .requiredSkillCount(10).matchedSkillCount(4).missingSkillCount(6)
                .createdAt(LocalDateTime.of(2026, 9, 23, 10, 0))
                .build();
    }

    // ================= API-04 · POST /api/skill-gap/analyze =================

    @Test
    @DisplayName("API-04 · Phân tích với templateId chỉ định")
    void analyze_coTemplateId() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(skillGapService.analyze(NGUOI_DUNG, 10L)).thenReturn(KET_QUA);

        mockMvc.perform(post("/api/skill-gap/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requiredSkillCount").value(10))
                .andExpect(jsonPath("$.missingSkillCount").value(6))
                .andExpect(jsonPath("$.matchPercent").value(40.0));

        verify(skillGapService).analyze(NGUOI_DUNG, 10L);
    }

    @Test
    @DisplayName("API-04 · POST không kèm thân request vẫn phân tích được (không lỗi 500)")
    void analyze_khongCoThanRequest() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(skillGapService.analyze(eq(NGUOI_DUNG), isNull())).thenReturn(KET_QUA);

        mockMvc.perform(post("/api/skill-gap/analyze"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchPercent").value(40.0));

        // request == null phải được đổi thành templateId null, không phải ném NPE.
        verify(skillGapService).analyze(NGUOI_DUNG, null);
    }

    @Test
    @DisplayName("API-04 · Thân request có nhưng templateId để trống thì dùng lộ trình mặc định")
    void analyze_thanRequestRong() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(skillGapService.analyze(eq(NGUOI_DUNG), isNull())).thenReturn(KET_QUA);

        mockMvc.perform(post("/api/skill-gap/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        verify(skillGapService).analyze(NGUOI_DUNG, null);
    }

    // ================= API-05 · POST /api/skill-gap/reports =================

    @Test
    @DisplayName("API-05 · Tạo báo cáo chạy đúng ba bước: phân tích → xuất PDF → lưu")
    void createReport_dungThuTuBaBuoc() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(skillGapService.analyze(NGUOI_DUNG, 10L)).thenReturn(KET_QUA);
        when(pdfService.generateSkillGapReport(NGUOI_DUNG, KET_QUA))
                .thenReturn("reports/skill-gap-1.pdf");
        when(skillGapService.saveReport(NGUOI_DUNG, KET_QUA, "reports/skill-gap-1.pdf"))
                .thenReturn(baoCao(1L));

        mockMvc.perform(post("/api/skill-gap/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"templateId\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.pdfPath").value("reports/skill-gap-1.pdf"));

        var thuTu = org.mockito.Mockito.inOrder(skillGapService, pdfService);
        thuTu.verify(skillGapService).analyze(NGUOI_DUNG, 10L);
        thuTu.verify(pdfService).generateSkillGapReport(NGUOI_DUNG, KET_QUA);
        // Đường dẫn PDF phải là đường dẫn PdfService vừa trả về, không phải giá trị dựng sẵn.
        thuTu.verify(skillGapService).saveReport(NGUOI_DUNG, KET_QUA, "reports/skill-gap-1.pdf");
    }

    @Test
    @DisplayName("API-05 · Tạo báo cáo không kèm thân request vẫn chạy")
    void createReport_khongCoThanRequest() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(skillGapService.analyze(eq(NGUOI_DUNG), isNull())).thenReturn(KET_QUA);
        when(pdfService.generateSkillGapReport(NGUOI_DUNG, KET_QUA)).thenReturn("reports/a.pdf");
        when(skillGapService.saveReport(NGUOI_DUNG, KET_QUA, "reports/a.pdf"))
                .thenReturn(baoCao(2L));

        mockMvc.perform(post("/api/skill-gap/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));

        verify(skillGapService).analyze(NGUOI_DUNG, null);
    }

    // ================= API-06 / API-07 · Đọc báo cáo =================

    @Test
    @DisplayName("API-06 · Liệt kê báo cáo của chính người dùng đang đăng nhập")
    void reports_lietKeTheoNguoiDung() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(skillGapService.getReports(NGUOI_DUNG)).thenReturn(List.of(baoCao(1L), baoCao(2L)));

        mockMvc.perform(get("/api/skill-gap/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(skillGapService).getReports(NGUOI_DUNG);
    }

    @Test
    @DisplayName("API-06 · Chưa có báo cáo nào thì trả mảng rỗng")
    void reports_chuaCoBaoCao() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(skillGapService.getReports(NGUOI_DUNG)).thenReturn(List.of());

        mockMvc.perform(get("/api/skill-gap/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("API-07 · Xem một báo cáo truyền kèm người dùng để service kiểm chủ sở hữu (BR-31)")
    void report_truyenKemNguoiDung() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(skillGapService.getReport(NGUOI_DUNG, 1L)).thenReturn(baoCao(1L));

        mockMvc.perform(get("/api/skill-gap/reports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.summary").value("Thiếu 6 kỹ năng"));

        // Người dùng phải được truyền xuống service — nếu không, ai cũng đọc được báo cáo
        // của người khác chỉ bằng cách đổi id trên URL.
        verify(skillGapService).getReport(NGUOI_DUNG, 1L);
    }
}
