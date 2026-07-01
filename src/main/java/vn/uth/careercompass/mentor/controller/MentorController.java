package vn.uth.careercompass.mentor.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.mentor.entity.MentorSession;
import vn.uth.careercompass.mentor.service.MentorService;

import java.util.List;

@Controller
@RequestMapping("/mentor")
public class MentorController {

    private final MentorService mentorService;

    public MentorController(MentorService mentorService) {
        this.mentorService = mentorService;
    }

    @GetMapping
    public String chatPage(@AuthenticationPrincipal User user, Model model) {
        List<MentorSession> sessions = mentorService.getSessionsForUser(user);

        model.addAttribute("sessions", sessions);

        if (!sessions.isEmpty()) {
            MentorSession current = sessions.get(0);
            model.addAttribute("currentSessionId", current.getId());
            model.addAttribute("messages", mentorService.getMessages(current));
        } else {
            model.addAttribute("currentSessionId", null);
            model.addAttribute("messages", List.of());
        }

        return "mentor/chat"; // trả cả trang khi load lần đầu (GET bình thường)
    }

    @PostMapping("/new")
    public String createSession(@AuthenticationPrincipal User user) {
        MentorSession session = mentorService.createSession(user);
        return "redirect:/mentor?session=" + session.getId();
    }

    @PostMapping("/send")
    public String sendMessage(@AuthenticationPrincipal User user,
                               @RequestParam Long sessionId,
                               @RequestParam String content,
                               Model model) {

        MentorSession session = mentorService.getSessionsForUser(user).stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại hoặc không thuộc về bạn"));

        mentorService.sendMessage(session, content);

        model.addAttribute("messages", mentorService.getMessages(session));
        model.addAttribute("currentSessionId", session.getId());

        return "mentor/chat :: messageList"; // CHỈ trả fragment cho HTMX swap, không load lại cả trang
    }
}