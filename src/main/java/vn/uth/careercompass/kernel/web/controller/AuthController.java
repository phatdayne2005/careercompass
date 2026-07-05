package vn.uth.careercompass.kernel.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.uth.careercompass.kernel.exception.EmailAlreadyExistsException;
import vn.uth.careercompass.kernel.service.AuthService;
import vn.uth.careercompass.kernel.service.PasswordResetService;
import vn.uth.careercompass.kernel.web.dto.request.RegisterFormDTO;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterFormDTO());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterFormDTO registerFormDTO, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            authService.register(registerFormDTO.getFullName(), registerFormDTO.getEmail(), registerFormDTO.getPassword());
        } catch (EmailAlreadyExistsException e) {
            result.rejectValue("email", "error.email", e.getMessage());
            return "register";
        }
        return "redirect:/login?registered";

    }

    @GetMapping("/forgot")
    public String forgot() {
        return "forgot";
    }

    /**
     * Xử lý quên mật khẩu: nếu email thuộc user LOCAL → sinh token + gửi link qua email.
     * Luôn redirect về /forgot?sent với thông báo trung lập (không tiết lộ email có tồn
     * tại hay không — best practice bảo mật). Xử lý thật nằm trong PasswordResetService.
     */
    @PostMapping("/forgot")
    public String forgotSubmit(@RequestParam String email) {
        passwordResetService.createResetToken(email);
        return "redirect:/forgot?sent";
    }

    /**
     * Mở link đặt lại từ email. Kiểm token còn hợp lệ (tồn tại + chưa dùng + chưa hết hạn)
     * → hiện form đổi mật khẩu; nếu không → hiện trạng thái "link hết hạn/không hợp lệ".
     */
    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValid", passwordResetService.validateToken(token).isPresent());
        return "reset-password";
    }

    /**
     * Nhận mật khẩu mới. Kiểm độ dài + khớp confirm, rồi đổi mật khẩu qua service.
     * Token có thể hết hạn giữa lúc mở form và submit → bắt lỗi để báo cho người dùng.
     */
    @PostMapping("/reset-password")
    public String resetPasswordSubmit(@RequestParam String token,
                                      @RequestParam String newPassword,
                                      @RequestParam String confirmPassword,
                                      Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValid", true);

        if (newPassword == null || newPassword.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải từ 6 ký tự trở lên.");
            return "reset-password";
        }
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "reset-password";
        }

        try {
            passwordResetService.resetPassword(token, newPassword);
        } catch (Exception e) {
            model.addAttribute("tokenValid", false);
            model.addAttribute("error", "Link đặt lại đã hết hạn hoặc không hợp lệ. Vui lòng yêu cầu link mới.");
            return "reset-password";
        }
        return "redirect:/login?resetSuccess";
    }
}
