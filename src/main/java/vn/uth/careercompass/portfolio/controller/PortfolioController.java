package vn.uth.careercompass.portfolio.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import vn.uth.careercompass.portfolio.entity.ProjectRepository;
import vn.uth.careercompass.portfolio.service.PortfolioService;

@Controller
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/manage")
    public String managePortfolio() {
        return "portfolio/manage"; 
    }

    @PostMapping("/sync")
    public String syncPortfolio(@RequestParam("githubUsername") String githubUsername, Model model) {
    
        List<ProjectRepository> repos = portfolioService.syncGithubRepositories(githubUsername);
        
        model.addAttribute("repositories", repos);
        
        return "portfolio/manage :: #repo-list"; 
    }
}