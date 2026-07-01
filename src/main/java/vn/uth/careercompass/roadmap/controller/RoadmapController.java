package vn.uth.careercompass.roadmap.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.roadmap.dto.ProgressUpdateRequest;
import vn.uth.careercompass.roadmap.dto.ProgressUpdateResponse;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.service.ProgressService;
import vn.uth.careercompass.roadmap.service.RoadmapService;

import java.util.List;

@RestController
@RequestMapping("/api/roadmap")
@RequiredArgsConstructor
public class RoadmapController {
    private final RoadmapService roadmapService;
    private final ProgressService progressService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/templates")
    public List<RoadmapTemplateDTO> templates() {
        return roadmapService.getActiveTemplates();
    }

    @GetMapping
    public RoadmapViewDTO roadmap(@RequestParam(required = false) Long templateId, Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        return roadmapService.getRoadmap(user, templateId);
    }

    @PostMapping("/progress")
    public ProgressUpdateResponse updateProgress(@RequestBody ProgressUpdateRequest request, Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        UserNodeProgress progress = progressService.updateProgress(user, request.getSkillNodeId(), request.getStatus());
        Long templateId = progress.getSkillNode().getTemplate().getId();
        RoadmapViewDTO roadmap = roadmapService.getRoadmap(user, templateId);

        return ProgressUpdateResponse.builder()
                .skillNodeId(progress.getSkillNode().getId())
                .status(progress.getStatus())
                .completionPercent(roadmap.getCompletionPercent())
                .build();
    }
}
