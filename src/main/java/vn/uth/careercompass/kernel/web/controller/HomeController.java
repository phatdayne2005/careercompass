package vn.uth.careercompass.kernel.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;

/**
 * [STUB - tạm cho P1 test] Trang đích "/" sau khi đăng nhập.
 * Theo Navigation Flow, "/" thực chất là Dashboard (hub) thuộc gói P5.
 * File này chỉ để P1 kiểm thử luồng Auth end-to-end (login xong thấy được trang).
 * Khi P5 dựng Dashboard thật → thay thế / gỡ bỏ controller + view này.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                String role = authority.getAuthority();
                if ("ROLE_ADMIN".equals(role)) {
                    return "redirect:/admin";
                } else if ("ROLE_COUNSELOR".equals(role)) {
                    return "redirect:/counselor/templates";
                }
            }
        }
        
        model.addAttribute("email", authentication != null ? authentication.getName() : "khách");
        return "home";
    }
}
