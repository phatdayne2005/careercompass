package vn.uth.careercompass.mentor.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.mentor.entity.MentorSession;
import vn.uth.careercompass.mentor.service.MentorService;

import java.util.List;

@Controller
@RequestMapping("/mentor")
public class MentorController {

    private final MentorService mentorService;
    private final AuthenticatedUserService authenticatedUserService;

    public MentorController(MentorService mentorService, AuthenticatedUserService authenticatedUserService) {
        this.mentorService = mentorService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping
    public String chatPage(@RequestParam(name = "session", required = false) Long sessionId,
                           Authentication authentication, Model model) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        List<MentorSession> sessions = mentorService.getSessionsForUser(user);
        model.addAttribute("sessions", sessions);

        // Chọn session theo param nếu hợp lệ, không thì lấy cuộc mới nhất
        MentorSession current = null;
        if (sessionId != null) {
            current = sessions.stream().filter(s -> s.getId().equals(sessionId)).findFirst().orElse(null);
        }
        if (current == null && !sessions.isEmpty()) {
            current = sessions.get(0);
        }

        if (current != null) {
            model.addAttribute("currentSessionId", current.getId());
            model.addAttribute("messages", mentorService.getMessages(current));
        } else {
            model.addAttribute("currentSessionId", null);
            model.addAttribute("messages", List.of());
        }
        return "mentor/chat";
    }

    @PostMapping("/new")
    public String createSession(Authentication authentication) {
        User user = authenticatedUserService.requireCurrentUser(authentication);
        MentorSession session = mentorService.createSession(user);
        return "redirect:/mentor?session=" + session.getId();
    }

    @PostMapping("/send")
    public String sendMessage(@RequestParam Long sessionId,
                              @RequestParam String content,
                              Authentication authentication,
                              Model model) {
        User user = authenticatedUserService.requireCurrentUser(authentication);

        MentorSession session = mentorService.getSessionsForUser(user).stream()
                .filter(s -> s.getId().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Session không tồn tại hoặc không thuộc về bạn"));

        mentorService.sendMessage(session, content);

        model.addAttribute("messages", mentorService.getMessages(session));
        model.addAttribute("currentSessionId", session.getId());
        return "mentor/chat :: messageList"; // fragment cho HTMX swap
    }
}
