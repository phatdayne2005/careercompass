package vn.uth.careercompass.admin.web;
 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.uth.careercompass.admin.service.AdminDashboardService;
 
import java.util.Map;
 
@Controller
@RequestMapping({"/admin", "/admin/dashboard"})
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;
 
    @GetMapping
    public String dashboard(Model model) {
        Map<String, Object> stats = adminDashboardService.getDashboardStats();
        model.addAllAttributes(stats);
        return "admin/dashboard";
    }
}
