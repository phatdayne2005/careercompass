package vn.uth.careercompass.profile.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.profile.service.ProfileService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final AuthenticatedUserService authenticatedUserService;

    public ProfileController(ProfileService profileService, AuthenticatedUserService authenticatedUserService) {
        this.profileService = profileService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public String settingsPage(Authentication authentication, Model model) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        model.addAttribute("user", profileService.getProfile(user));
        return "profile/settings";
    }

    @PostMapping("/github")
    public String updateGithub(@RequestParam String githubUsername,
                               Authentication authentication,
                               Model model) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        profileService.updateGithub(user, githubUsername);
        model.addAttribute("user", user);
        return "profile/settings :: githubSection";
    }

    @PostMapping("/name")
    public String updateName(@RequestParam String fullName,
                             Authentication authentication,
                             RedirectAttributes ra) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        if (fullName == null || fullName.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Họ tên không được để trống.");
            return "redirect:/profile";
        }
        profileService.updateName(user, fullName);
        ra.addFlashAttribute("success", "Đã cập nhật họ tên.");
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 Authentication authentication,
                                 RedirectAttributes ra) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        if (newPassword == null || newPassword.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu mới phải từ 6 ký tự trở lên.");
            return "redirect:/profile";
        }
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Xác nhận mật khẩu không khớp.");
            return "redirect:/profile";
        }
        try {
            profileService.changePassword(user, currentPassword, newPassword);
            ra.addFlashAttribute("success", "Đã đổi mật khẩu.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/email")
    public String updateEmail(@RequestParam String email,
                              Authentication authentication,
                              HttpServletRequest request,
                              RedirectAttributes ra) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        try {
            profileService.updateEmail(user, email);
            // Email là username -> buộc đăng nhập lại bằng email mới
            request.logout();
            return "redirect:/login?emailChanged";
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage() != null ? e.getMessage() : "Đổi email thất bại.");
            return "redirect:/profile";
        }
    }
}
