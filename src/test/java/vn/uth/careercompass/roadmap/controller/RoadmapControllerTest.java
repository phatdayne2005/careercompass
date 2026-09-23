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
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.service.ProgressService;
import vn.uth.careercompass.roadmap.service.RoadmapService;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

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
 * Kiểm thử REST API lộ trình học (API-01 → API-03, FR2.2 và FR2.4).
 *
 * Ba endpoint này trước đây chỉ được kiểm ở tầng HTTP bằng Postman, còn ở tầng đơn vị thì
 * bao phủ 0%. Kiểm bằng Postman đòi ứng dụng phải chạy và CSDL phải có dữ liệu; lát cắt
 * web này chạy trong vài giây và không cần gì cả, nên bắt lỗi sớm hơn nhiều.
 *
 * Điểm đáng kiểm nhất nằm ở updateProgress: controller tự đi từ node đã cập nhật ngược lên
 * template để tính lại phần trăm hoàn thành. Chuỗi getSkillNode().getTemplate().getId() đó
 * là logic riêng của controller, không có trong ProgressService.
 */
@WebMvcTest(value = RoadmapController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
@DisplayName("FR2.2 / FR2.4 — REST API lộ trình học")
class RoadmapControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoadmapService roadmapService;

    @MockitoBean
    private ProgressService progressService;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    private static final User NGUOI_DUNG = User.builder()
            .id(1L).email("student@uth.edu.vn").fullName("Nguyen Van A").build();

    private static final RoadmapTemplateDTO TEMPLATE = RoadmapTemplateDTO.builder()
            .id(10L).name("Lộ trình Backend").description("Mô tả").targetRoleId(1L).build();

    @Test
    @DisplayName("API-01 · GET /api/roadmap/templates trả danh sách template đang bật")
    void templates_traDanhSachJson() throws Exception {
        when(roadmapService.getActiveTemplates()).thenReturn(List.of(TEMPLATE));

        mockMvc.perform(get("/api/roadmap/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].name").value("Lộ trình Backend"));
    }

    @Test
    @DisplayName("API-01 · Chưa có template nào thì trả mảng rỗng, không phải null")
    void templates_khongCoTemplate_traMangRong() throws Exception {
        when(roadmapService.getActiveTemplates()).thenReturn(List.of());

        mockMvc.perform(get("/api/roadmap/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    @Test
    @DisplayName("API-02 · Không truyền templateId thì dùng lộ trình mặc định của người dùng")
    void roadmap_khongTruyenTemplateId() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(roadmapService.getRoadmap(eq(NGUOI_DUNG), isNull()))
                .thenReturn(roadmap(8, 2, 25.0));

        mockMvc.perform(get("/api/roadmap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalNodes").value(8))
                .andExpect(jsonPath("$.completedNodes").value(2))
                .andExpect(jsonPath("$.completionPercent").value(25.0))
                .andExpect(jsonPath("$.template.id").value(10));

        verify(roadmapService).getRoadmap(NGUOI_DUNG, null);
    }

    @Test
    @DisplayName("API-02 · Truyền templateId thì lấy đúng lộ trình được chỉ định")
    void roadmap_truyenTemplateId() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(roadmapService.getRoadmap(NGUOI_DUNG, 10L)).thenReturn(roadmap(4, 4, 100.0));

        mockMvc.perform(get("/api/roadmap").param("templateId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercent").value(100.0));

        verify(roadmapService).getRoadmap(NGUOI_DUNG, 10L);
    }

    @Test
    @DisplayName("API-03 · Đánh dấu hoàn thành trả về phần trăm tiến độ tính lại")
    void updateProgress_traVePhanTramTinhLai() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(progressService.updateProgress(NGUOI_DUNG, 100L, ProgressStatus.DONE))
                .thenReturn(tienDo(100L, ProgressStatus.DONE));
        when(roadmapService.getRoadmap(NGUOI_DUNG, 10L)).thenReturn(roadmap(8, 3, 37.5));

        mockMvc.perform(post("/api/roadmap/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillNodeId\":100,\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skillNodeId").value(100))
                .andExpect(jsonPath("$.status").value("DONE"))
                // Phần trăm phải lấy từ lộ trình tính lại SAU khi ghi tiến độ,
                // nếu lấy trước thì người dùng thấy con số cũ.
                .andExpect(jsonPath("$.completionPercent").value(37.5));
    }

    @Test
    @DisplayName("API-03 · Bỏ đánh dấu hoàn thành cũng đi đúng luồng và hạ phần trăm")
    void updateProgress_boDanhDau() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(progressService.updateProgress(NGUOI_DUNG, 100L, ProgressStatus.NOT_STARTED))
                .thenReturn(tienDo(100L, ProgressStatus.NOT_STARTED));
        when(roadmapService.getRoadmap(NGUOI_DUNG, 10L)).thenReturn(roadmap(8, 2, 25.0));

        mockMvc.perform(post("/api/roadmap/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillNodeId\":100,\"status\":\"NOT_STARTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.completionPercent").value(25.0));
    }

    @Test
    @DisplayName("API-03 · Phần trăm lấy từ ĐÚNG template chứa node vừa cập nhật")
    void updateProgress_layTemplateTuNodeVuaCapNhat() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        // Node thuộc template 99, KHÁC template mặc định 10 của người dùng.
        UserNodeProgress tienDo = tienDo(200L, ProgressStatus.IN_PROGRESS);
        tienDo.getSkillNode().getTemplate().setId(99L);
        when(progressService.updateProgress(NGUOI_DUNG, 200L, ProgressStatus.IN_PROGRESS))
                .thenReturn(tienDo);
        when(roadmapService.getRoadmap(NGUOI_DUNG, 99L)).thenReturn(roadmap(5, 1, 20.0));

        mockMvc.perform(post("/api/roadmap/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skillNodeId\":200,\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercent").value(20.0));

        // Nếu controller lấy nhầm template mặc định thì phép kiểm này đỏ.
        verify(roadmapService).getRoadmap(NGUOI_DUNG, 99L);
    }

    private static RoadmapViewDTO roadmap(int tong, int xong, double phanTram) {
        return RoadmapViewDTO.builder()
                .template(TEMPLATE)
                .totalNodes(tong)
                .completedNodes(xong)
                .completionPercent(phanTram)
                .nodes(List.of())
                .build();
    }

    private static UserNodeProgress tienDo(long nodeId, ProgressStatus trangThai) {
        SkillNode node = SkillNode.builder()
                .id(nodeId)
                .template(SkillTreeTemplate.builder().id(10L).name("Lộ trình Backend").build())
                .build();
        return UserNodeProgress.builder()
                .id(1L).user(NGUOI_DUNG).skillNode(node).status(trangThai).build();
    }
}
