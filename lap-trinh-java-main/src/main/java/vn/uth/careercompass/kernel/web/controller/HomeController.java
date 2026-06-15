package vn.uth.careercompass.kernel.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

/**
 * [STUB - tạm cho P1 test] Trang đích "/" sau khi đăng nhập.
 * Theo Navigation Flow, "/" thực chất là Dashboard (hub) thuộc gói P5.
 * File này chỉ để P1 kiểm thử luồng Auth end-to-end (login xong thấy được trang).
 * Khi P5 dựng Dashboard thật → thay thế / gỡ bỏ controller + view này.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Principal principal, Model model) {
        // principal = người dùng đang đăng nhập (Spring Security tự inject).
        // principal.getName() trả về email (vì getUsername() của ta là email).
        model.addAttribute("email", principal != null ? principal.getName() : "khách");
        return "home";
    }
}
