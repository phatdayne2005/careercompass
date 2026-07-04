package vn.uth.careercompass.onboarding.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.UserProfileService;
import vn.uth.careercompass.onboarding.service.OnboardingService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final AuthenticatedUserService authenticatedUserService;
    private final UserProfileService userProfileService;
    private final OnboardingService onboardingService;
    private final CareerRoleRepository careerRoleRepository;
    private final SkillRepository skillRepository;

    // ─── Helper: lấy user và kiểm tra đã hoàn thành chưa ────────────────────

    /**
     * Lấy user hiện tại.
     * Nếu đã hoàn thành onboarding rồi → trả về null (để controller redirect về /).
     */
    private User getIncompleteUser(Authentication auth) {
        User user = authenticatedUserService.requireCurrentUser(auth);
        // Nếu đã xong → không cho vào lại onboarding (tránh vòng lặp)
        if (Boolean.TRUE.equals(user.getOnboardingCompleted())) {
            return null;
        }
        return user;
    }

    // ─── BƯỚC 1: Chọn Target Role ────────────────────────────────────────────

    @GetMapping("/step1")
    public String step1(Model model, Authentication authentication) {
        User user = getIncompleteUser(authentication);
        if (user == null) return "redirect:/";

        model.addAttribute("roles", careerRoleRepository.findAll());
        model.addAttribute("currentStep", 1);
        return "onboarding/step1_role";
    }

    @PostMapping("/step1")
    public String step1Submit(@RequestParam(required = false) Long roleId,
                              @RequestParam(defaultValue = "false") boolean skip,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = getIncompleteUser(authentication);
        if (user == null) return "redirect:/";

        if (skip) {
            return "redirect:/onboarding/step2";
        }
        if (roleId == null) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng chọn một vị trí nghề nghiệp.");
            return "redirect:/onboarding/step1";
        }
        userProfileService.setTargetRole(user, roleId);
        return "redirect:/onboarding/step2";
    }

    // ─── BƯỚC 2: Upload transcript & GitHub ──────────────────────────────────

    @GetMapping("/step2")
    public String step2(Model model, Authentication authentication) {
        User user = getIncompleteUser(authentication);
        if (user == null) return "redirect:/";

        model.addAttribute("currentStep", 2);
        model.addAttribute("githubUsername", user.getGithubUsername());
        model.addAttribute("hasTranscript", user.getTranscriptPath() != null);
        return "onboarding/step2_sources";
    }

    @PostMapping("/step2")
    public String step2Submit(@RequestParam(required = false) MultipartFile transcriptFile,
                              @RequestParam(required = false) String githubUsername,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = getIncompleteUser(authentication);
        if (user == null) return "redirect:/";

        // Xử lý file transcript
        if (transcriptFile != null && !transcriptFile.isEmpty()) {
            try {
                String savedPath = onboardingService.saveTranscript(user, transcriptFile);
                userProfileService.storeTranscript(user, savedPath, null);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi tải lên bảng điểm: " + e.getMessage());
                return "redirect:/onboarding/step2";
            }
        }

        // Xử lý GitHub username
        if (githubUsername != null && !githubUsername.isBlank()) {
            userProfileService.setGithub(user, githubUsername.trim());
        }

        return "redirect:/onboarding/step3";
    }

    // ─── BƯỚC 3: Chọn Kỹ năng ────────────────────────────────────────────────

    @GetMapping("/step3")
    public String step3(Model model, Authentication authentication) {
        User user = getIncompleteUser(authentication);
        if (user == null) return "redirect:/";

        model.addAttribute("currentStep", 3);
        model.addAttribute("skillsByCategory", groupSkillsByCategory());
        List<Long> existingSkillIds = onboardingService.getUserSkillIds(user);
        model.addAttribute("existingSkillIds", existingSkillIds);
        return "onboarding/step3_skills";
    }

    @PostMapping("/step3")
    public String step3Submit(@RequestParam(value = "skillIds", required = false) List<Long> skillIds,
                              Authentication authentication) {
        User user = getIncompleteUser(authentication);
        if (user == null) return "redirect:/";

        // Lưu kỹ năng (nếu có chọn)
        if (skillIds != null && !skillIds.isEmpty()) {
            userProfileService.replaceSkills(user, skillIds);
        }

        // ✅ Đánh dấu hoàn thành onboarding — lần sau login sẽ đi thẳng vào app
        userProfileService.completeOnboarding(user);

        return "redirect:/";
    }

    // ─── Helper: nhóm skill theo category (dễ chọn + gọn trên CV) ────────────

    /** Thứ tự nhóm hiển thị ở step3; nhóm ngoài danh sách này xếp cuối. */
    private static final List<String> CATEGORY_ORDER = List.of(
            "Ngôn ngữ", "Frontend", "Backend", "Database", "Mobile",
            "Data & AI", "DevOps & Cloud", "Network", "Tools", "Nền tảng");

    private Map<String, List<Skill>> groupSkillsByCategory() {
        Map<String, List<Skill>> byCategory = skillRepository.findAll().stream()
                .collect(Collectors.groupingBy(s -> s.getCategory() == null ? "Khác" : s.getCategory()));

        Map<String, List<Skill>> ordered = new LinkedHashMap<>();
        for (String category : CATEGORY_ORDER) {
            List<Skill> list = byCategory.remove(category);
            if (list != null) {
                list.sort(Comparator.comparing(Skill::getName));
                ordered.put(category, list);
            }
        }
        // Nhóm lạ (nếu có) xếp sau cùng, không bỏ sót skill nào
        byCategory.forEach((category, list) -> {
            list.sort(Comparator.comparing(Skill::getName));
            ordered.put(category, list);
        });
        return ordered;
    }
}
