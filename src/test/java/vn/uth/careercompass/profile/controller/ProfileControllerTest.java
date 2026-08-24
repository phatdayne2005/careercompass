package vn.uth.careercompass.profile.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import vn.uth.careercompass.kernel.entity.AuthProvider;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.profile.service.ProfileService;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = ProfileController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    private final User userAccount = User.builder()
            .email("student@uth.edu.vn")
            .fullName("Nguyen Van A")
            .authProvider(AuthProvider.LOCAL)
            .build();

    @Test
    void settingsPage_returnsProfileView() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);
        when(profileService.getProfile(userAccount)).thenReturn(userAccount);

        mockMvc.perform(get("/profile")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/settings"))
                .andExpect(model().attribute("user", userAccount));
    }

    @Test
    void updateGithub_returnsFragmentAndUpdatedUser() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(post("/profile/github")
                        .param("githubUsername", "nguyenvana")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/settings :: githubSection"))
                .andExpect(model().attribute("user", userAccount));

        verify(profileService).updateGithub(userAccount, "nguyenvana");
    }

    @Test
    void updateName_whenBlank_redirectsWithError() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(post("/profile/name")
                        .param("fullName", "   ")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/profile"))
                .andExpect(flash().attribute("error", "Họ tên không được để trống."));
    }

    @Test
    void updateName_whenValid_updatesAndRedirectsWithSuccess() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(post("/profile/name")
                        .param("fullName", "Nguyen Van B")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/profile"))
                .andExpect(flash().attribute("success", "Đã cập nhật họ tên."));

        verify(profileService).updateName(userAccount, "Nguyen Van B");
    }

    @Test
    void changePassword_whenTooShort_returnsError() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(post("/profile/password")
                        .param("currentPassword", "old-password")
                        .param("newPassword", "12345")
                        .param("confirmPassword", "12345")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Mật khẩu mới phải từ 6 ký tự trở lên."));
    }

    @Test
    void changePassword_whenConfirmationDoesNotMatch_returnsError() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(post("/profile/password")
                        .param("currentPassword", "old-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "different-password")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Xác nhận mật khẩu không khớp."));
    }

    @Test
    void changePassword_whenServiceSucceeds_redirectsWithSuccess() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(post("/profile/password")
                        .param("currentPassword", "old-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "new-password")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("success", "Đã đổi mật khẩu."));

        verify(profileService).changePassword(userAccount, "old-password", "new-password");
    }

    @Test
    void changePassword_whenServiceFails_redirectsWithServiceError() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);
        doThrow(new IllegalStateException("Mật khẩu hiện tại không đúng."))
                .when(profileService).changePassword(userAccount, "old-password", "new-password");

        mockMvc.perform(post("/profile/password")
                        .param("currentPassword", "old-password")
                        .param("newPassword", "new-password")
                        .param("confirmPassword", "new-password")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Mật khẩu hiện tại không đúng."));
    }

    @Test
    void updateEmail_whenServiceSucceeds_logsOutAndRedirectsToLogin() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(post("/profile/email")
                        .param("email", "new@uth.edu.vn")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/login?emailChanged"));

        verify(profileService).updateEmail(userAccount, "new@uth.edu.vn");
    }

    @Test
    void updateEmail_whenServiceFails_redirectsWithError() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);
        doThrow(new IllegalStateException("Email này đã được sử dụng bởi tài khoản khác."))
                .when(profileService).updateEmail(userAccount, "taken@uth.edu.vn");

        mockMvc.perform(post("/profile/email")
                        .param("email", "taken@uth.edu.vn")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/profile"))
                .andExpect(flash().attribute("error", "Email này đã được sử dụng bởi tài khoản khác."));
    }
}
