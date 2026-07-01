package vn.uth.careercompass.mentor.service;

import org.springframework.stereotype.Service;

import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.mentor.entity.ChatMessage;
import vn.uth.careercompass.mentor.entity.MentorSession;
import vn.uth.careercompass.mentor.entity.Sender;
import vn.uth.careercompass.mentor.repository.ChatMessageRepository;
import vn.uth.careercompass.mentor.repository.MentorSessionRepository;

import java.util.List;

@Service
public class MentorService {

    private final MentorSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final LlmClient llmClient;

    public MentorService(MentorSessionRepository sessionRepo,
                          ChatMessageRepository messageRepo,
                          LlmClient llmClient) {
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.llmClient = llmClient;
    }

    public List<MentorSession> getSessionsForUser(User user) {
        return sessionRepo.findByUserOrderByCreatedAtDesc(user);
    }

    public MentorSession createSession(User user) {
        MentorSession session = new MentorSession();
        session.setUser(user);
        session.setTitle("Cuộc trò chuyện mới");
        return sessionRepo.save(session);
    }

    public List<ChatMessage> getMessages(MentorSession session) {
        return messageRepo.findBySessionOrderByCreatedAtAsc(session);
    }

    public ChatMessage sendMessage(MentorSession session, String userText) {
        // 1. Lưu tin nhắn của user
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setSender(Sender.USER);
        userMsg.setContent(userText);
        messageRepo.save(userMsg);

        // 2. Dựng prompt cá nhân hoá + gọi LLM
        String prompt = buildPrompt(session, userText);
        String aiReply = llmClient.ask(prompt);

        // 3. Lưu phản hồi AI
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSession(session);
        aiMsg.setSender(Sender.AI);
        aiMsg.setContent(aiReply);
        return messageRepo.save(aiMsg);
    }

    private String buildPrompt(MentorSession session, String userText) {
    User user = session.getUser();

    // TODO: khi P7 hoàn thành refactor targetRoleId -> CareerRole,
    // đổi dòng dưới thành user.getTargetRole().getName() để lấy tên role dạng chữ
    String targetRoleInfo = (user.getTargetRoleId() != null)
            ? "Role ID #" + user.getTargetRoleId()
            : "chưa chọn định hướng nghề nghiệp";

    return """
        Bạn là mentor tư vấn định hướng nghề nghiệp.
        Hồ sơ học viên:
        - Định hướng nghề nghiệp: %s
        - GitHub: %s

        Câu hỏi: %s
        """.formatted(
            targetRoleInfo,
            user.getGithubUsername() != null ? user.getGithubUsername() : "chưa liên kết",
            userText
    );
}
}