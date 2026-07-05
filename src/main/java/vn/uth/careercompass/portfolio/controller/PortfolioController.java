package vn.uth.careercompass.portfolio.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.portfolio.entity.GitHubProfile;
import vn.uth.careercompass.portfolio.entity.ProjectRepository;
import vn.uth.careercompass.portfolio.service.PortfolioService;

@Controller
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final AuthenticatedUserService authenticatedUserService;

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping("/manage")
    public String managePortfolio(Authentication authentication, Model model) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        GitHubProfile profile = portfolioService.getProfileByUser(user).orElse(null);

        // Prefill ô username: ưu tiên profile đã sync, nếu chưa thì lấy từ hồ sơ user (onboarding)
        model.addAttribute("githubUsername",
                profile != null ? profile.getGithubUsername() : user.getGithubUsername());
        model.addAttribute("profile", profile);
        if (profile != null) {
            model.addAttribute("repositories", portfolioService.getRepos(profile.getId()));
            model.addAttribute("shareUrl", baseUrl + "/p/" + profile.getSlug());
        }
        return "portfolio/manage";
    }

    @PostMapping("/repos/{repoId}/toggle")
    public String toggleRepo(@org.springframework.web.bind.annotation.PathVariable Long repoId,
                             Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        portfolioService.toggleRepoVisibility(user, repoId);
        return "redirect:/portfolio/manage";
    }

    @PostMapping("/sync")
    public String syncPortfolio(@RequestParam("githubUsername") String githubUsername,
                                Authentication authentication, RedirectAttributes ra) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        try {
            List<ProjectRepository> repos = portfolioService.syncGithubRepositories(user, githubUsername);
            ra.addFlashAttribute("message", "Đã đồng bộ " + repos.size() + " repository từ GitHub.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/portfolio/manage";
    }
}
