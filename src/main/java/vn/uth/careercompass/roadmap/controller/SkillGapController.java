package vn.uth.careercompass.roadmap.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.roadmap.dto.SkillGapAnalyzeRequest;
import vn.uth.careercompass.roadmap.dto.SkillGapReportDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.service.PdfService;
import vn.uth.careercompass.roadmap.service.SkillGapService;

import java.util.List;

@RestController
@RequestMapping("/api/skill-gap")
@RequiredArgsConstructor
public class SkillGapController {
    private final SkillGapService skillGapService;
    private final PdfService pdfService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping("/analyze")
    public SkillGapResultDTO analyze(@RequestBody(required = false) SkillGapAnalyzeRequest request, Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        return skillGapService.analyze(user, request == null ? null : request.getTemplateId());
    }

    @PostMapping("/reports")
    public SkillGapReportDTO createReport(@RequestBody(required = false) SkillGapAnalyzeRequest request, Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        SkillGapResultDTO result = skillGapService.analyze(user, request == null ? null : request.getTemplateId());
        String pdfPath = pdfService.generateSkillGapReport(user, result);
        return skillGapService.saveReport(user, result, pdfPath);
    }

    @GetMapping("/reports")
    public List<SkillGapReportDTO> reports(Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        return skillGapService.getReports(user);
    }

    @GetMapping("/reports/{id}")
    public SkillGapReportDTO report(@PathVariable Long id, Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        return skillGapService.getReport(user, id);
    }
}
