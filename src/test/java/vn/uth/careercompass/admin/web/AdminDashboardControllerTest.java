package vn.uth.careercompass.admin.web;

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
import vn.uth.careercompass.admin.service.AdminDashboardService;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiểm thử bảng điều khiển quản trị (FR7.1).
 *
 * Controller này chỉ có ba dòng nhưng làm một việc dễ hỏng âm thầm: nó dùng
 * model.addAllAttributes(stats) để rải THẲNG mọi khoá của Map vào model. Nếu service đổi
 * tên khoá thì trang vẫn trả HTTP 200 và chỉ hiện ô trống — không có lỗi nào được ném.
 * Vì vậy phép kiểm ở đây khẳng định đúng SÁU khoá mà giao diện đang đọc.
 *
 * Controller còn khai @RequestMapping({"/admin", "/admin/dashboard"}), nghĩa là hai URL
 * phải cùng ra một trang; đó là một phép kiểm riêng.
 */
@WebMvcTest(value = AdminDashboardController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "admin@uth.edu.vn", roles = "ADMIN")
@DisplayName("FR7.1 — Bảng điều khiển quản trị")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    private static Map<String, Object> thongKe() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalUsers", 120L);
        m.put("totalStudents", 100L);
        m.put("totalCounselors", 15L);
        m.put("totalAdmins", 5L);
        m.put("totalCareerRoles", 6L);
        m.put("totalRoadmaps", 8L);
        return m;
    }

    @Test
    @DisplayName("FR7.1 · Sáu chỉ số thống kê được rải đúng tên vào model")
    void dashboard_raiDuSauChiSo() throws Exception {
        when(adminDashboardService.getDashboardStats()).thenReturn(thongKe());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("totalUsers", 120L))
                .andExpect(model().attribute("totalStudents", 100L))
                .andExpect(model().attribute("totalCounselors", 15L))
                .andExpect(model().attribute("totalAdmins", 5L))
                .andExpect(model().attribute("totalCareerRoles", 6L))
                .andExpect(model().attribute("totalRoadmaps", 8L));
    }

    @Test
    @DisplayName("FR7.1 · /admin/dashboard cho ra cùng một trang với /admin")
    void dashboard_haiUrlCungMotTrang() throws Exception {
        when(adminDashboardService.getDashboardStats()).thenReturn(thongKe());

        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("totalUsers", 120L));
    }

    @Test
    @DisplayName("FR7.1 · Hệ thống chưa có dữ liệu thì trang vẫn mở được")
    void dashboard_chuaCoDuLieu_vanMoDuoc() throws Exception {
        when(adminDashboardService.getDashboardStats()).thenReturn(Map.of());

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }
}
