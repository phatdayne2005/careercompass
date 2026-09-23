package vn.uth.careercompass.dashboard.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.dashboard.dto.ActivityLogDTO;
import vn.uth.careercompass.dashboard.dto.DashboardViewDTO;
import vn.uth.careercompass.dashboard.service.DashboardService;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiểm thử trang tổng quan của sinh viên (FR6.2, API-19).
 *
 * Controller mỏng nhưng nắm hai điều không nên sai: nó phải lấy người dùng qua
 * AuthenticatedUserService (chứ không tin thẳng Authentication), và phải đặt
 * activeNav = "dashboard" để thanh điều hướng tô sáng đúng mục. Cả hai đều lặng lẽ hỏng
 * nếu ai đó sửa nhầm: trang vẫn trả HTTP 200.
 */
@WebMvcTest(value = DashboardController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
@DisplayName("FR6.2 — Trang tổng quan sinh viên")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    private static final User NGUOI_DUNG = User.builder()
            .id(1L).email("student@uth.edu.vn").fullName("Nguyen Van A").build();

    @Test
    @DisplayName("FR6.2 · Trang tổng quan hiện tiến độ và hoạt động gần đây")
    void dashboardPage_hienTienDoVaHoatDong() throws Exception {
        DashboardViewDTO view = DashboardViewDTO.builder()
                .roadmapCompletionPercent(37.5)
                .completedNodes(3).totalNodes(8)
                .matchedSkillCount(4).missingSkillCount(6)
                .recentActivities(List.of(ActivityLogDTO.builder()
                        .type("ROADMAP").description("Hoàn thành node Java")
                        .createdAt(LocalDateTime.of(2026, 9, 23, 9, 0)).build()))
                .build();
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(dashboardService.getDashboard(NGUOI_DUNG)).thenReturn(view);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/home"))
                .andExpect(model().attribute("activeNav", "dashboard"))
                .andExpect(model().attribute("dashboard", view));

        // Người dùng phải lấy qua service, không đọc thẳng từ Authentication.
        verify(authenticatedUserService).requireCurrentUser(any());
        verify(dashboardService).getDashboard(NGUOI_DUNG);
    }

    @Test
    @DisplayName("FR6.2 · Sinh viên chưa có tiến độ nào thì trang vẫn mở, số liệu bằng 0")
    void dashboardPage_chuaCoTienDo() throws Exception {
        DashboardViewDTO trong = DashboardViewDTO.builder()
                .roadmapCompletionPercent(0.0)
                .completedNodes(0).totalNodes(0)
                .matchedSkillCount(0).missingSkillCount(0)
                .recentActivities(List.of())
                .build();
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(NGUOI_DUNG);
        when(dashboardService.getDashboard(NGUOI_DUNG)).thenReturn(trong);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/home"))
                .andExpect(model().attribute("dashboard", trong));
    }
}
