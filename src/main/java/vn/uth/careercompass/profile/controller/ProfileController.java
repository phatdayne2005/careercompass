package vn.uth.careercompass.profile.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.profile.service.ProfileService;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public String settingsPage(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("user", profileService.getProfile(user));
        return "profile/settings";
    }

    @PostMapping("/github")
    public String updateGithub(@AuthenticationPrincipal User user,
                                @RequestParam String githubUsername,
                                Model model) {
        profileService.updateGithub(user, githubUsername);
        model.addAttribute("user", user);
        return "profile/settings :: githubSection";
    }
}