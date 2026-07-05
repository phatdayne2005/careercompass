package vn.uth.careercompass.portfolio.controller;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.portfolio.entity.GitHubProfile;
import vn.uth.careercompass.portfolio.service.PortfolioService;

/**
 * Trang E-Portfolio CÔNG KHAI mở qua URL chia sẻ /p/{slug} — KHÔNG cần đăng nhập (FR5.3).
 * Chỉ hiển thị các repo được đánh dấu công khai của chủ portfolio.
 */
@Controller
@RequestMapping("/p")
@RequiredArgsConstructor
public class PublicPortfolioController {

    private final PortfolioService portfolioService;
    private final UserRepository userRepository;

    @GetMapping("/{slug}")
    public String publicPortfolio(@PathVariable("slug") String slug, Model model) {
        GitHubProfile profile = portfolioService.getProfileBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy portfolio công khai"));

        User owner = userRepository.findById(profile.getUserId()).orElse(null);
        model.addAttribute("profile", profile);
        model.addAttribute("owner", owner);
        model.addAttribute("ownerInfo", portfolioService.getOwnerInfo(profile.getUserId()));
        model.addAttribute("repositories", portfolioService.getPublicRepos(profile.getId()));
        return "portfolio/public";
    }
}
