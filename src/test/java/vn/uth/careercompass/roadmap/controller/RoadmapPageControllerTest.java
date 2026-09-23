package vn.uth.careercompass.roadmap.controller;

import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.roadmap.dto.RoadmapNodeDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.service.ProgressService;
import vn.uth.careercompass.roadmap.service.RoadmapService;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = RoadmapPageController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
class RoadmapPageControllerTest {

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

    private final User userAccount = User.builder().email("student@uth.edu.vn").build();

    @Test
    void roadmapPage_whenTemplateAndNodeAreProvided_rendersSelectedNode() throws Exception {
        RoadmapNodeDTO selected = node(2L, 2, ProgressStatus.NOT_STARTED);
        RoadmapViewDTO roadmap = roadmap(selected, node(1L, 1, ProgressStatus.DONE),
                node(3L, 3, ProgressStatus.DONE));
        givenRoadmap(roadmap);

        mockMvc.perform(get("/roadmap")
                        .param("templateId", "10")
                        .param("nodeId", "2")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("roadmap/index"))
                .andExpect(model().attribute("selectedNode", selected))
                .andExpect(model().attribute("estimateText", "~3 tuần"))
                .andExpect(model().attribute("prerequisiteText", "Hoàn thành tầng Nền tảng"))
                .andExpect(model().attribute("tier1Nodes", List.of(roadmap.getNodes().get(1))))
                .andExpect(model().attribute("tier2Nodes", List.of(selected)))
                .andExpect(model().attribute("tier3Nodes", List.of(roadmap.getNodes().get(2))));

        verify(roadmapService).getRoadmap(userAccount, 10L);
    }

    @Test
    void roadmapPage_whenTemplateIsStoredInSession_selectsInProgressNode() throws Exception {
        RoadmapNodeDTO inProgress = node(2L, 2, ProgressStatus.IN_PROGRESS);
        RoadmapViewDTO roadmap = roadmap(inProgress, node(1L, 1, ProgressStatus.NOT_STARTED));
        givenRoadmap(roadmap);

                mockMvc.perform(get("/roadmap")
                        .sessionAttr("selectedRoadmapTemplateId", 10L)
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedNode", inProgress))
                .andExpect(model().attribute("estimateText", "~3 tuần"));

        verify(roadmapService).getRoadmap(userAccount, 10L);
    }

    @Test
    void roadmapPage_whenNodeIdDoesNotExist_fallsBackToFirstNode() throws Exception {
        RoadmapNodeDTO first = node(1L, 1, ProgressStatus.NOT_STARTED);
        RoadmapViewDTO roadmap = roadmap(first, node(2L, 3, ProgressStatus.NOT_STARTED));
        givenRoadmap(roadmap);

                mockMvc.perform(get("/roadmap")
                        .param("nodeId", "999")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedNode", first))
                .andExpect(model().attribute("estimateText", "~1 tuần"))
                .andExpect(model().attribute("prerequisiteText", "Không có"));
    }

    @Test
    void roadmapPage_whenNodesAreEmpty_usesDefaultTexts() throws Exception {
        RoadmapViewDTO roadmap = RoadmapViewDTO.builder()
                .template(RoadmapTemplateDTO.builder().id(10L).build())
                .nodes(List.of())
                .build();
        givenRoadmap(roadmap);

                mockMvc.perform(get("/roadmap")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedNode", (Object) null))
                .andExpect(model().attribute("estimateText", "~1 tuần"))
                .andExpect(model().attribute("prerequisiteText", "Không có"));
    }

    @Test
    void updateProgress_withTemplateId_redirectsToSelectedRoadmap() throws Exception {
        when(authenticatedUserService.requireCurrentUser(nullable(Authentication.class))).thenReturn(userAccount);

        mockMvc.perform(post("/roadmap/progress")
                        .param("skillNodeId", "7")
                        .param("status", "DONE")
                        .param("templateId", "10")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/roadmap?nodeId=7&templateId=10"));

        verify(progressService).updateProgress(userAccount, 7L, ProgressStatus.DONE);
    }

    @Test
    void updateProgress_withoutTemplateId_redirectsWithoutTemplateQuery() throws Exception {
        when(authenticatedUserService.requireCurrentUser(nullable(Authentication.class))).thenReturn(userAccount);

        mockMvc.perform(post("/roadmap/progress")
                        .param("skillNodeId", "7")
                        .param("status", "IN_PROGRESS")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/roadmap?nodeId=7"));

        verify(progressService).updateProgress(userAccount, 7L, ProgressStatus.IN_PROGRESS);
    }

    // ================================================================
    // Các nhánh chọn node và sinh văn bản gợi ý còn lại (FR2.2, FR2.3)
    // ================================================================

    // KHÔNG có phép kiểm cho nhánh nodes == null trong selectNode: không tới được.
    // RoadmapService.getRoadmap dựng danh sách bằng nodes.stream()...toList() từ kết quả
    // repository, mà findBy... của Spring Data trả danh sách RỖNG chứ không trả null. Đó
    // cũng là lý do filterByTier ngay bên cạnh gọi thẳng nodes.stream() mà không phòng
    // null — hai hàm không nhất quán, nhưng cả hai đều đúng với dữ liệu thật.

    @Test
    void roadmapPage_khongCoNodeDangHoc_chonNodeChuaHoanThanhDauTien() throws Exception {
        // Không node nào IN_PROGRESS -> rơi vào nhánh .or(...): lấy node chưa DONE đầu tiên.
        RoadmapNodeDTO daXong = node(1L, 1, ProgressStatus.DONE);
        RoadmapNodeDTO chuaHoc = node(2L, 2, ProgressStatus.NOT_STARTED);
        givenRoadmap(roadmap(daXong, chuaHoc));

        mockMvc.perform(get("/roadmap").param("templateId", "10")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedNode",
                        org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.is(2L))));
    }

    @Test
    void roadmapPage_hoanThanhHetMoiNode_quayVeNodeDauTien() throws Exception {
        // Mọi node đều DONE: cả hai bộ lọc đều rỗng -> orElse(nodes.get(0)).
        givenRoadmap(roadmap(node(1L, 1, ProgressStatus.DONE), node(2L, 2, ProgressStatus.DONE)));

        mockMvc.perform(get("/roadmap").param("templateId", "10")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedNode",
                        org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.is(1L))));
    }

    @Test
    void roadmapPage_nodeTang3_hienDieuKienVaThoiLuongCuaTangCaoNhat() throws Exception {
        RoadmapNodeDTO tang3 = node(3L, 3, ProgressStatus.NOT_STARTED);
        givenRoadmap(roadmap(tang3));

        mockMvc.perform(get("/roadmap").param("templateId", "10").param("nodeId", "3")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prerequisiteText", "Hoàn thành tầng Nền tảng + Cốt lõi"))
                .andExpect(model().attribute("estimateText", "~4 tuần"))
                .andExpect(model().attribute("tier3Nodes", org.hamcrest.Matchers.hasSize(1)));
    }

    @Test
    void roadmapPage_nodeKhongKhaiTang_dungVanBanMacDinhVaKhongLotVaoNhomNao() throws Exception {
        // tier null xảy ra với node do cố vấn tạo mà quên đặt tầng. Ba bộ lọc tierN đều
        // phải bỏ qua nó thay vì ném NullPointerException khi so sánh tier == n.
        RoadmapNodeDTO khongTang = RoadmapNodeDTO.builder()
                .id(9L).title("Node lạ").tier(null).status(ProgressStatus.NOT_STARTED).build();
        givenRoadmap(roadmap(khongTang));

        mockMvc.perform(get("/roadmap").param("templateId", "10")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("prerequisiteText", "Không có"))
                .andExpect(model().attribute("estimateText", "~1 tuần"))
                .andExpect(model().attribute("tier1Nodes", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(model().attribute("tier2Nodes", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(model().attribute("tier3Nodes", org.hamcrest.Matchers.hasSize(0)));
    }

    private void givenRoadmap(RoadmapViewDTO roadmap) {
        when(authenticatedUserService.requireCurrentUser(nullable(Authentication.class))).thenReturn(userAccount);
        when(roadmapService.getRoadmap(userAccount, 10L)).thenReturn(roadmap);
        when(roadmapService.getRoadmap(userAccount, (Long) null)).thenReturn(roadmap);
        when(roadmapService.getActiveTemplates()).thenReturn(List.of());
    }

    private RoadmapViewDTO roadmap(RoadmapNodeDTO... nodes) {
        return RoadmapViewDTO.builder()
                .template(RoadmapTemplateDTO.builder().id(10L).name("Default").build())
                .nodes(List.of(nodes))
                .build();
    }

    private RoadmapNodeDTO node(Long id, int tier, ProgressStatus status) {
        return RoadmapNodeDTO.builder()
                .id(id)
                .title("Node " + id)
                .tier(tier)
                .status(status)
                .build();
    }
}
