package vn.uth.careercompass.kernel.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;

import java.util.Collection;

/**
 * Trang đích "/" sau khi đăng nhập.
 * - ADMIN   → /admin
 * - COUNSELOR → /counselor/templates
 * - STUDENT chưa onboarding → /onboarding/step1
 * - STUDENT đã onboarding  → /home (dashboard tạm)
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        // Điều hướng theo role
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        for (GrantedAuthority authority : authorities) {
            String role = authority.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                return "redirect:/admin";
            } else if ("ROLE_COUNSELOR".equals(role)) {
                return "redirect:/counselor/templates";
            }
        }

        // Student: kiểm tra onboarding
        User user = authenticatedUserService.requireCurrentUser(authentication);
        if (!Boolean.TRUE.equals(user.getOnboardingCompleted())) {
            return "redirect:/onboarding/step1";
        }

        model.addAttribute("email", authentication.getName());
        return "home";
    }
}
