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
import vn.uth.careercompass.admin.dto.UserAdminDto;
import vn.uth.careercompass.admin.service.AdminUserService;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiểm thử tầng web của chức năng quản trị người dùng (FR7.2, FR7.3).
 *
 * AdminUserController bao phủ 0% dòng và 0% nhánh trước lớp này. Hai khối nhánh chưa được
 * phủ đều là loại dễ sai mà khó thấy:
 *
 *   - Nhánh tìm kiếm: keyword null, keyword toàn khoảng trắng và keyword thật dẫn tới ba
 *     đường đi khác nhau. Keyword "   " phải rơi vào nhánh liệt kê TẤT CẢ chứ không phải
 *     đi tìm chuỗi rỗng.
 *   - Nhánh principal null: controller đưa currentUserEmail vào model để giao diện KHÔNG
 *     hiện nút tự khoá chính mình. Nếu principal null mà không xử lý thì trang vỡ.
 */
@WebMvcTest(value = AdminUserController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "admin@uth.edu.vn", roles = "ADMIN")
@DisplayName("FR7.2 / FR7.3 — Quản trị người dùng (tầng web)")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    private static UserAdminDto nguoiDung(long id, String ten, String email, boolean bat) {
        return UserAdminDto.builder()
                .id(id).fullName(ten).email(email).roleName("STUDENT")
                .careerRoleName("Backend Developer").enabled(bat)
                .createdAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .build();
    }

    // ================= FR7.2 — Danh sách và tìm kiếm =================

    @Test
    @DisplayName("FR7.2 · Không có từ khoá thì liệt kê toàn bộ người dùng")
    void listUsers_khongTuKhoa_lietKeTatCa() throws Exception {
        when(adminUserService.getAllUsers()).thenReturn(List.of(
                nguoiDung(1L, "Nguyen Van A", "a@uth.edu.vn", true),
                nguoiDung(2L, "Tran Thi B", "b@uth.edu.vn", false)));

        mockMvc.perform(get("/admin/users").with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users"))
                .andExpect(model().attribute("users", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(model().attribute("roles", RoleName.values()))
                .andExpect(model().attributeDoesNotExist("keyword"));

        verify(adminUserService).getAllUsers();
        verify(adminUserService, never()).searchUsers(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("FR7.2 · Có từ khoá thì tìm kiếm và giữ lại từ khoá trên ô nhập")
    void listUsers_coTuKhoa_timKiem() throws Exception {
        when(adminUserService.searchUsers("nguyen"))
                .thenReturn(List.of(nguoiDung(1L, "Nguyen Van A", "a@uth.edu.vn", true)));

        mockMvc.perform(get("/admin/users").param("keyword", "nguyen")
                        .with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("users", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(model().attribute("keyword", "nguyen"));

        verify(adminUserService).searchUsers("nguyen");
        verify(adminUserService, never()).getAllUsers();
    }

    @Test
    @DisplayName("FR7.2 · Từ khoá thừa khoảng trắng hai đầu được cắt trước khi tìm")
    void listUsers_tuKhoaThuaKhoangTrang_duocCat() throws Exception {
        when(adminUserService.searchUsers("nguyen"))
                .thenReturn(List.of(nguoiDung(1L, "Nguyen Van A", "a@uth.edu.vn", true)));

        mockMvc.perform(get("/admin/users").param("keyword", "   nguyen   ")
                        .with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().isOk())
                // Ô nhập hiện lại bản ĐÃ CẮT, không phải nguyên văn người dùng gõ.
                .andExpect(model().attribute("keyword", "nguyen"));

        verify(adminUserService).searchUsers("nguyen");
    }

    @Test
    @DisplayName("FR7.2 · Từ khoá toàn khoảng trắng bị coi như không tìm kiếm")
    void listUsers_tuKhoaToanKhoangTrang_coiNhuKhongTim() throws Exception {
        when(adminUserService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users").param("keyword", "     ")
                        .with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("keyword"));

        // Nếu rơi nhầm vào nhánh tìm kiếm thì sẽ đi tìm chuỗi rỗng và trả về danh sách trắng.
        verify(adminUserService).getAllUsers();
        verify(adminUserService, never()).searchUsers(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("FR7.2 · Email người đang đăng nhập được đưa vào model để chặn tự khoá mình")
    void listUsers_dayEmailNguoiDangDangNhapVaoModel() throws Exception {
        when(adminUserService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/admin/users").with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentUserEmail", "admin@uth.edu.vn"));
    }

    // ================= FR7.3 — Khoá / đổi vai trò / xoá =================

    @Test
    @DisplayName("FR7.3 · Khoá người dùng trả về fragment dòng đã cập nhật")
    void toggleStatus_traVeFragmentDong() throws Exception {
        when(adminUserService.toggleUserStatus(2L))
                .thenReturn(nguoiDung(2L, "Tran Thi B", "b@uth.edu.vn", false));

        mockMvc.perform(post("/admin/users/2/toggle-status")
                        .with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/fragments/user-row-actions"))
                .andExpect(model().attribute("user",
                        org.hamcrest.Matchers.hasProperty("enabled", org.hamcrest.Matchers.is(false))))
                .andExpect(model().attribute("currentUserEmail", "admin@uth.edu.vn"));

        verify(adminUserService).toggleUserStatus(2L);
    }

    @Test
    @DisplayName("FR7.3 · Đổi vai trò xong quay lại danh sách người dùng")
    void changeRole_quayLaiDanhSach() throws Exception {
        mockMvc.perform(post("/admin/users/2/change-role").param("roleName", "COUNSELOR")
                        .with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminUserService).changeUserRole(2L, "COUNSELOR");
    }

    @Test
    @DisplayName("FR7.3 · Xoá người dùng xong quay lại danh sách người dùng")
    void deleteUser_quayLaiDanhSach() throws Exception {
        mockMvc.perform(post("/admin/users/2/delete")
                        .with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(adminUserService).deleteUser(2L);
    }
}
