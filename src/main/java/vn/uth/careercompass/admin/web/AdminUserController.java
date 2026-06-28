package vn.uth.careercompass.admin.web;
 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.uth.careercompass.admin.dto.UserAdminDto;
import vn.uth.careercompass.admin.mapper.UserAdminMapper;
import vn.uth.careercompass.admin.service.AdminUserService;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;
 
import java.util.List;
 
@Controller
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;
 
    @GetMapping("/admin/users")
    public String listUsers(java.security.Principal principal, Model model) {
        List<UserAdminDto> users = adminUserService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("currentUserEmail", principal != null ? principal.getName() : "");
        return "admin/users";
    }
 
    @PostMapping("/admin/users/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, java.security.Principal principal, Model model) {
        UserAdminDto updatedUserDto = adminUserService.toggleUserStatus(id);
        model.addAttribute("user", updatedUserDto);
        model.addAttribute("currentUserEmail", principal != null ? principal.getName() : "");
        return "admin/fragments/user-row-actions";
    }
 
    @PostMapping("/admin/users/{id}/change-role")
    public String changeRole(@PathVariable Long id, @RequestParam String roleName) {
        adminUserService.changeUserRole(id, roleName);
        return "redirect:/admin/users";
    }
}
