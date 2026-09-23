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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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

    // ========== DEF-013: hai biên trên của mật khẩu ==========
    // Trước khi sửa, đường đặt lại mật khẩu KHÔNG có biên trên nào. Hai phép kiểm dưới
    // đây là chỗ duy nhất trong bộ hộp trắng đi qua hai nhánh đó.

    @Test
    void resetPasswordSubmit_matKhauQua30KyTu_baoLoiRoRang() throws Exception {
        mockMvc.perform(post("/reset-password")
                        .param("token", "token")
                        .param("newPassword", "p".repeat(31))
                        .param("confirmPassword", "p".repeat(31)))
                .andExpect(status().isOk())
                .andExpect(view().name("reset-password"))
                .andExpect(model().attribute("error", "Mật khẩu không được quá 30 ký tự."));

        verify(passwordResetService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void resetPasswordSubmit_matKhauTiengVietVuot72Byte_baoLoiTiengViet() throws Exception {
        // 25 ký tự tiếng Việt toàn dấu = 75 byte: thoả giới hạn 30 ký tự nhưng vượt giới
        // hạn cứng của BCrypt. Trước khi sửa, đường này bắt Exception rồi báo SAI rằng
        // "link đã hết hạn" — người dùng xin link mới mãi không xong.
        String matKhau = "ậ".repeat(25);

        mockMvc.perform(post("/reset-password")
                        .param("token", "token")
                        .param("newPassword", matKhau)
                        .param("confirmPassword", matKhau))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error",
                        "Mật khẩu chứa quá nhiều ký tự có dấu hoặc biểu tượng "
                                + "(vượt 72 byte). Vui lòng rút ngắn lại."));

        verify(passwordResetService, never()).resetPassword(anyString(), anyString());
    }

    @Test
    void register_matKhauTiengVietVuot72Byte_traVeFormKemLoi() throws Exception {
        // @Size(max = 30) của DTO đếm KÝ TỰ nên mật khẩu này lọt qua validation; chặn được
        // là nhờ phép kiểm byte bổ sung trong controller.
        mockMvc.perform(post("/register")
                        .param("fullName", "Nguyen Van A")
                        .param("email", "a@uth.edu.vn")
                        .param("password", "ậ".repeat(25)))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));

        verify(authService, never()).register(anyString(), anyString(), anyString());
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
