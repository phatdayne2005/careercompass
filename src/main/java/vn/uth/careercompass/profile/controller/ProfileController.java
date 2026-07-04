package vn.uth.careercompass.profile.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
}
