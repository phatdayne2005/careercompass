package vn.uth.careercompass.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/p")
public class PublicPortfolioController {

    @GetMapping("/{slug}")
    public String publicPortfolio(@PathVariable("slug") String slug) {
    
        return "portfolio/public";
    }
}