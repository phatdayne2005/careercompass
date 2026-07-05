package vn.uth.careercompass.marketpulse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.uth.careercompass.marketpulse.service.MarketPulseService;

@Controller
@RequiredArgsConstructor
public class MarketPulseController {

    private final MarketPulseService marketPulseService;

    @GetMapping("/market/pulse")
    public String pulsePage(Model model) {
        model.addAttribute("activeNav", "market");
        model.addAttribute("pulse", marketPulseService.getMarketPulse());
        return "market/pulse";
    }
}