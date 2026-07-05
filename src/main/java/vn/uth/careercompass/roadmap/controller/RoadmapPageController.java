package vn.uth.careercompass.roadmap.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.roadmap.dto.RoadmapNodeDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.service.ProgressService;
import vn.uth.careercompass.roadmap.service.RoadmapService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RoadmapPageController {
    private final RoadmapService roadmapService;
    private final ProgressService progressService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/roadmap")
    public String roadmapPage(
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long nodeId,
            Authentication authentication,
            Model model
    ) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        RoadmapViewDTO roadmap = roadmapService.getRoadmap(user, templateId);
        RoadmapNodeDTO selectedNode = selectNode(roadmap.getNodes(), nodeId);

        model.addAttribute("activeNav", "roadmap");
        model.addAttribute("templates", roadmapService.getActiveTemplates());
        model.addAttribute("roadmap", roadmap);
        model.addAttribute("selectedNode", selectedNode);
        model.addAttribute("tier1Nodes", filterByTier(roadmap.getNodes(), 1));
        model.addAttribute("tier2Nodes", filterByTier(roadmap.getNodes(), 2));
        model.addAttribute("tier3Nodes", filterByTier(roadmap.getNodes(), 3));
        model.addAttribute("estimateText", estimateFor(selectedNode));
        model.addAttribute("prerequisiteText", prerequisiteFor(roadmap.getNodes(), selectedNode));
        return "roadmap/index";
    }

    @PostMapping("/roadmap/progress")
    public String updateProgress(
            @RequestParam Long skillNodeId,
            @RequestParam ProgressStatus status,
            @RequestParam(required = false) Long templateId,
            Authentication authentication
    ) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        progressService.updateProgress(user, skillNodeId, status);
        String redirect = "redirect:/roadmap?nodeId=" + skillNodeId;
        if (templateId != null) {
            redirect += "&templateId=" + templateId;
        }
        return redirect;
    }

    private RoadmapNodeDTO selectNode(List<RoadmapNodeDTO> nodes, Long nodeId) {
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        if (nodeId != null) {
            return nodes.stream()
                    .filter(node -> nodeId.equals(node.getId()))
                    .findFirst()
                    .orElse(nodes.get(0));
        }
        return nodes.stream()
                .filter(node -> ProgressStatus.IN_PROGRESS.equals(node.getStatus()))
                .findFirst()
                .or(() -> nodes.stream().filter(node -> !ProgressStatus.DONE.equals(node.getStatus())).findFirst())
                .orElse(nodes.get(0));
    }

    private List<RoadmapNodeDTO> filterByTier(List<RoadmapNodeDTO> nodes, int tier) {
        return nodes.stream()
                .filter(node -> node.getTier() != null && node.getTier() == tier)
                .toList();
    }

    private String prerequisiteFor(List<RoadmapNodeDTO> nodes, RoadmapNodeDTO selectedNode) {
        if (selectedNode == null || selectedNode.getTier() == null) {
            return "Không có";
        }
        return switch (selectedNode.getTier()) {
            case 1 -> "Không có";
            case 2 -> "Hoàn thành tầng Nền tảng";
            default -> "Hoàn thành tầng Nền tảng + Cốt lõi";
        };
    }

    private String estimateFor(RoadmapNodeDTO selectedNode) {
        if (selectedNode == null || selectedNode.getTier() == null) {
            return "~1 tuần";
        }
        return switch (selectedNode.getTier()) {
            case 1 -> "~1 tuần";
            case 2 -> "~3 tuần";
            default -> "~4 tuần";
        };
    }
}
