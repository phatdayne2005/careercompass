package vn.uth.careercompass.roadmap.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.roadmap.dto.SkillGapReportDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.service.PdfService;
import vn.uth.careercompass.roadmap.service.RoadmapService;
import vn.uth.careercompass.roadmap.service.SkillGapService;

import java.nio.file.Path;

@Controller
@RequiredArgsConstructor
public class SkillGapPageController {
    private final AuthenticatedUserService authenticatedUserService;
    private final RoadmapService roadmapService;
    private final SkillGapService skillGapService;
    private final PdfService pdfService;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    @GetMapping("/skill-gap")
    public String skillGapPage(
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Boolean reportCreated,
            Authentication authentication,
            Model model
    ) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        SkillGapResultDTO result = skillGapService.analyze(user, templateId);
        model.addAttribute("activeNav", "skill-gap");
        model.addAttribute("templates", roadmapService.getActiveTemplates());
        model.addAttribute("result", result);
        model.addAttribute("currentSkills", skillGapService.getAcquiredSkills(user));
        model.addAttribute("allSkills", skillRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(Skill::getName)).toList());
        model.addAttribute("reports", skillGapService.getReports(user));
        model.addAttribute("reportCreated", Boolean.TRUE.equals(reportCreated));
        return "skillgap/index";
    }

    @PostMapping("/skill-gap/skills")
    public String addSkill(
            @RequestParam Long skillId,
            @RequestParam(required = false) Long templateId,
            Authentication authentication
    ) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        // Chọn từ danh sách kỹ năng có sẵn của hệ thống (không cho gõ tự do -> tránh skill "rác"
        // không khớp với node roadmap nào).
        if (skillId != null) {
            skillRepository.findById(skillId).ifPresent(skill -> {
                if (!userSkillRepository.existsByUserAndSkill(user, skill)) {
                    userSkillRepository.save(UserSkill.builder().user(user).skill(skill).build());
                }
            });
        }
        return redirectSkillGap(templateId, false);
    }

    /**
     * "Export PDF": tạo báo cáo + TẢI THẲNG file PDF luôn (đảm bảo luôn là PDF hợp lệ, vừa sinh).
     * Báo cáo cũng được lưu vào lịch sử (hiện ở cột phải khi tải lại trang).
     */
    @PostMapping("/skill-gap/reports")
    public ResponseEntity<Resource> createReport(
            @RequestParam(required = false) Long templateId,
            Authentication authentication
    ) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        SkillGapResultDTO result = skillGapService.analyze(user, templateId);
        String pdfPath = pdfService.generateSkillGapReport(user, result);
        SkillGapReportDTO saved = skillGapService.saveReport(user, result, pdfPath);
        Resource resource = new FileSystemResource(Path.of(pdfPath));
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"skill-gap-report-" + saved.getId() + ".pdf\"")
                .body(resource);
    }

    @GetMapping("/skill-gap/reports/{id}/download")
    public ResponseEntity<Resource> downloadReport(@PathVariable Long id, Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        SkillGapReportDTO report = skillGapService.getReport(user, id);   // đã kiểm report thuộc về user
        Resource resource = new FileSystemResource(Path.of(report.getPdfPath()));
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy file báo cáo");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"skill-gap-report-" + id + ".pdf\"")
                .body(resource);
    }

    private String redirectSkillGap(Long templateId, boolean reportCreated) {
        String redirect = "redirect:/skill-gap";
        boolean hasQuery = false;
        if (templateId != null) {
            redirect += "?templateId=" + templateId;
            hasQuery = true;
        }
        if (reportCreated) {
            redirect += hasQuery ? "&reportCreated=true" : "?reportCreated=true";
        }
        return redirect;
    }
}
