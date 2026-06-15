package vn.uth.careercompass.portfolio.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
@Controller
@RequestMapping("/portfolio")
public class PortfolioController {

    @GetMapping("/manage")
    public String managePortfolio() {

        return "portfolio/manage"; 
    }
}