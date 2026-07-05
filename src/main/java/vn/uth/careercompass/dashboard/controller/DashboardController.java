package vn.uth.careercompass.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.uth.careercompass.dashboard.service.DashboardService;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/dashboard")
    public String dashboardPage(Authentication authentication, Model model) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        model.addAttribute("activeNav", "dashboard");
        model.addAttribute("dashboard", dashboardService.getDashboard(user));
        return "dashboard/home";
    }
}