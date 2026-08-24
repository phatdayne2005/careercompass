package vn.uth.careercompass.kernel.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.kernel.exception.EmailAlreadyExistsException;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.AuthService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.kernel.service.PasswordResetService;
import vn.uth.careercompass.kernel.web.dto.request.RegisterFormDTO;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
})
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    @Test
    void login_returnsLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registerPage_addsEmptyRegisterForm() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("registerForm"))
                .andExpect(model().attribute("registerForm", org.hamcrest.Matchers.instanceOf(RegisterFormDTO.class)));
    }

    @Test
    void register_whenValidationFails_returnsRegisterView() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "")
                        .param("email", "invalid")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "fullName", "email", "password"));
    }

    @Test
    void register_whenValid_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("fullName", "Nguyen Van A")
                        .param("email", "a@uth.edu.vn")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/login?registered"));

        verify(authService).register("Nguyen Van A", "a@uth.edu.vn", "password");
    }

    @Test
    void register_whenEmailAlreadyExists_returnsFieldError() throws Exception {
        doThrow(new EmailAlreadyExistsException("Email đã được đăng ký"))
                .when(authService).register("Nguyen Van A", "a@uth.edu.vn", "password");

        mockMvc.perform(post("/register")
                        .param("fullName", "Nguyen Van A")
                        .param("email", "a@uth.edu.vn")
                        .param("password", "password"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "email"));
    }

    @Test
    void forgot_returnsForgotView() throws Exception {
        mockMvc.perform(get("/forgot"))
                .andExpect(status().isOk())
                .andExpect(view().name("forgot"));
    }

    @Test
    void forgotSubmit_alwaysRedirectsWithSentFlag() throws Exception {
        mockMvc.perform(post("/forgot").param("email", "a@uth.edu.vn"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/forgot?sent"));

        verify(passwordResetService).createResetToken("a@uth.edu.vn");
    }

    @Test
    void resetPasswordPage_whenTokenIsValid_exposesTrue() throws Exception {
        when(passwordResetService.validateToken("valid-token")).thenReturn(Optional.of(new vn.uth.careercompass.kernel.entity.PasswordResetToken()));

        mockMvc.perform(get("/reset-password").param("token", "valid-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("token", "valid-token"))
                .andExpect(model().attribute("tokenValid", true));
    }

    @Test
    void resetPasswordPage_whenTokenIsInvalid_exposesFalse() throws Exception {
        when(passwordResetService.validateToken("invalid-token")).thenReturn(Optional.empty());

        mockMvc.perform(get("/reset-password").param("token", "invalid-token"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tokenValid", false));
    }

    @Test
    void resetPasswordSubmit_whenPasswordTooShort_returnsError() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "token")
                        .param("newPassword", "12345")
                        .param("confirmPassword", "12345"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("tokenValid", true))
                .andExpect(model().attribute("error", "Mật khẩu phải từ 6 ký tự trở lên."));
    }

    @Test
    void resetPasswordSubmit_whenConfirmationDoesNotMatch_returnsError() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "token")
                        .param("newPassword", "password")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error", "Mật khẩu xác nhận không khớp."));
    }

    @Test
    void resetPasswordSubmit_whenServiceSucceeds_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "token")
                        .param("newPassword", "password")
                        .param("confirmPassword", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/login?resetSuccess"));

        verify(passwordResetService).resetPassword("token", "password");
    }

    @Test
    void resetPasswordSubmit_whenTokenExpires_returnsInvalidTokenError() throws Exception {
        doThrow(new IllegalStateException("Token không hợp lệ"))
                .when(passwordResetService).resetPassword("token", "password");

        mockMvc.perform(post("/reset-password")
                        .param("token", "token")
                        .param("newPassword", "password")
                        .param("confirmPassword", "password"))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("tokenValid", false))
                .andExpect(model().attribute("error", "Link đặt lại đã hết hạn hoặc không hợp lệ. Vui lòng yêu cầu link mới."));
    }
}
