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
import vn.uth.careercompass.kernel.web.dto.request.RegisterFormDTO;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
     * [STUB] Xử lý quên mật khẩu — HIỆN CHƯA gửi email thật.
     * Luôn redirect về /forgot?sent với thông báo trung lập (không tiết lộ email có
     * tồn tại hay không — đây là best practice bảo mật). TODO: tích hợp gửi email +
     * tạo token đặt lại mật khẩu ở milestone sau.
     */

    @PostMapping("/forgot")
    public String forgotSubmit(@RequestParam String email) {
        // TODO: nếu email tồn tại → sinh reset-token + gửi link qua email.
        return "redirect:/forgot?sent";
    }
}
