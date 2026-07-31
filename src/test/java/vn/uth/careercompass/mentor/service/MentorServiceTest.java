package vn.uth.careercompass.mentor.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.mentor.entity.ChatMessage;
import vn.uth.careercompass.mentor.entity.MentorSession;
import vn.uth.careercompass.mentor.entity.Sender;
import vn.uth.careercompass.mentor.repository.ChatMessageRepository;
import vn.uth.careercompass.mentor.repository.MentorSessionRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link MentorService}.
 *
 * <p>MỤC TIÊU: test RIÊNG luồng nghiệp vụ chat mentor mà KHÔNG bật Spring, KHÔNG đụng DB thật.
 * 3 dependency (2 repository + {@link LlmClient}) đều được Mockito giả lập.
 *
 * <p>WHY mock {@link LlmClient}? Vì nó gọi HTTP ra API Gemini bên ngoài — không kiểm soát được
 * trong unit test. Ta chỉ cần dạy nó "trả lời gì" hoặc "ném lỗi gì" để kiểm luồng của MentorService
 * (đặc biệt là cơ chế fallback khi LLM lỗi).
 */
@ExtendWith(MockitoExtension.class)
class MentorServiceTest {

    @Mock
    private MentorSessionRepository sessionRepo;
    @Mock
    private ChatMessageRepository messageRepo;
    @Mock
    private LlmClient llmClient;

    // @InjectMocks: tạo THẬT 1 MentorService rồi nhét 3 mock vào constructor.
    @InjectMocks
    private MentorService mentorService;

    // ============================================================
    // getSessionsForUser — chỉ ủy quyền (delegate) cho repository
    // ============================================================

    @Test
    void getSessionsForUser_delegatesToRepository() {
        // Given
        User user = User.builder().build();
        MentorSession s1 = new MentorSession();
        when(sessionRepo.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(s1));

        // When
        List<MentorSession> result = mentorService.getSessionsForUser(user);

        // Then: trả đúng thứ repository đưa ra, không thêm bớt.
        assertThat(result).containsExactly(s1);
        verify(sessionRepo).findByUserOrderByCreatedAtDesc(user);
    }

    // ============================================================
    // createSession — tạo session mới với tiêu đề mặc định
    // ============================================================

    @Test
    void createSession_createsWithDefaultTitleAndSaves() {
        // Given: dạy mock save trả lại chính đối tượng được truyền vào (giống DB gán id xong trả về).
        User user = User.builder().build();
        when(sessionRepo.save(any(MentorSession.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        MentorSession created = mentorService.createSession(user);

        // Then: session gắn đúng user + tiêu đề mặc định "Cuộc trò chuyện mới".
        ArgumentCaptor<MentorSession> captor = ArgumentCaptor.forClass(MentorSession.class);
        verify(sessionRepo).save(captor.capture());
        MentorSession saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTitle()).isEqualTo("Cuộc trò chuyện mới");
        // Giá trị trả về phải chính là session vừa lưu.
        assertThat(created).isSameAs(saved);
    }

    // ============================================================
    // getMessages — ủy quyền cho repository
    // ============================================================

    @Test
    void getMessages_delegatesToRepository() {
        MentorSession session = new MentorSession();
        ChatMessage m1 = new ChatMessage();
        when(messageRepo.findBySessionOrderByCreatedAtAsc(session)).thenReturn(List.of(m1));

        List<ChatMessage> result = mentorService.getMessages(session);

        assertThat(result).containsExactly(m1);
        verify(messageRepo).findBySessionOrderByCreatedAtAsc(session);
    }

    // ============================================================
    // sendMessage — luồng chính (nhiều nhánh)
    // ============================================================

    @Test
    void sendMessage_whenDefaultTitle_setsTitleSavesSessionAndBothMessages() {
        // Given: session còn tiêu đề mặc định -> phải được đổi tên theo câu hỏi đầu tiên.
        User user = User.builder().build();
        MentorSession session = new MentorSession();
        session.setTitle("Cuộc trò chuyện mới");
        String userText = "Làm sao để học Spring Boot?";
        when(llmClient.ask(anyString())).thenReturn("Hãy bắt đầu từ Spring Core.");
        // save trả lại arg để có thể khẳng định giá trị trả về của sendMessage.
        when(messageRepo.save(any(ChatMessage.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        ChatMessage aiMsg = mentorService.sendMessage(user, session, userText);

        // Then 1: tiêu đề session được đổi thành nội dung câu hỏi + session được lưu lại.
        assertThat(session.getTitle()).isEqualTo(userText);
        verify(sessionRepo).save(session);

        // Then 2: lưu ĐÚNG 2 message — USER trước, AI sau.
        ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepo, times(2)).save(msgCaptor.capture());
        List<ChatMessage> saved = msgCaptor.getAllValues();

        ChatMessage userMsg = saved.get(0);
        assertThat(userMsg.getSender()).isEqualTo(Sender.USER);
        assertThat(userMsg.getContent()).isEqualTo(userText);
        assertThat(userMsg.getSession()).isEqualTo(session);

        ChatMessage savedAi = saved.get(1);
        assertThat(savedAi.getSender()).isEqualTo(Sender.AI);
        assertThat(savedAi.getContent()).isEqualTo("Hãy bắt đầu từ Spring Core.");
        assertThat(savedAi.getSession()).isEqualTo(session);

        // Then 3: giá trị trả về chính là message AI vừa lưu.
        assertThat(aiMsg).isSameAs(savedAi);
    }

    @Test
    void sendMessage_whenTitleNull_setsTitleFromUserText() {
        // Given: title = null cũng là trạng thái "chưa đặt tên" -> nhánh set title.
        User user = User.builder().build();
        MentorSession session = new MentorSession();
        session.setTitle(null);
        when(llmClient.ask(anyString())).thenReturn("reply");

        // When
        mentorService.sendMessage(user, session, "Câu hỏi ngắn");

        // Then: title được gán, session được lưu.
        assertThat(session.getTitle()).isEqualTo("Câu hỏi ngắn");
        verify(sessionRepo).save(session);
    }

    @Test
    void sendMessage_whenLongUserText_truncatesTitleTo60CharsPlusEllipsis() {
        // Given: câu hỏi dài > 60 ký tự -> tiêu đề bị cắt còn 60 ký tự + dấu "…".
        User user = User.builder().build();
        MentorSession session = new MentorSession();
        session.setTitle("Cuộc trò chuyện mới");
        String longText = "a".repeat(80); // 80 ký tự
        when(llmClient.ask(anyString())).thenReturn("reply");

        // When
        mentorService.sendMessage(user, session, longText);

        // Then: đúng 60 ký tự đầu + "…" (tránh tiêu đề tràn UI).
        assertThat(session.getTitle()).isEqualTo("a".repeat(60) + "…");
        assertThat(session.getTitle()).hasSize(61); // 60 chữ + 1 ký tự "…"
    }

    @Test
    void sendMessage_whenTitleAlreadyCustom_doesNotRenameNorSaveSession() {
        // Given: session đã có tiêu đề riêng (không phải mặc định) -> KHÔNG đổi tên, KHÔNG save session.
        User user = User.builder().build();
        MentorSession session = new MentorSession();
        session.setTitle("Chủ đề đã đặt tên");
        when(llmClient.ask(anyString())).thenReturn("reply");

        // When
        mentorService.sendMessage(user, session, "Tin nhắn thứ hai");

        // Then: tiêu đề giữ nguyên, session KHÔNG bị lưu thêm.
        assertThat(session.getTitle()).isEqualTo("Chủ đề đã đặt tên");
        verify(sessionRepo, never()).save(any());
        // Nhưng vẫn lưu 2 message như thường.
        verify(messageRepo, times(2)).save(any(ChatMessage.class));
    }

    @Test
    void sendMessage_whenLlmThrows_usesFallbackReplyAndStillSaves() {
        // Given: LLM lỗi (mô phỏng Gemini quá tải / thiếu API key) -> service phải nuốt lỗi và fallback.
        User user = User.builder().build();
        MentorSession session = new MentorSession();
        session.setTitle("Chủ đề cũ"); // custom -> tập trung vào nhánh fallback
        when(llmClient.ask(anyString())).thenThrow(new RuntimeException("LLM down"));

        // When: KHÔNG được ném exception ra ngoài (chat không bị chặn).
        ChatMessage aiMsg = mentorService.sendMessage(user, session, "Xin chào");

        // Then: nội dung AI là câu fallback, vẫn được lưu bình thường.
        ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(messageRepo, times(2)).save(msgCaptor.capture());
        ChatMessage savedAi = msgCaptor.getAllValues().get(1);
        assertThat(savedAi.getSender()).isEqualTo(Sender.AI);
        assertThat(savedAi.getContent()).contains("chưa kết nối được AI Mentor");
    }

    // ============================================================
    // buildPrompt (private) — kiểm gián tiếp qua nội dung prompt gửi cho LLM
    // ============================================================

    @Test
    void sendMessage_buildsPersonalizedPrompt_whenProfileFilled() {
        // Given: user có đủ hồ sơ -> prompt phải chèn role id, github, tóm tắt bảng điểm + câu hỏi.
        CareerRole careerRole = mock(CareerRole.class);
        when(careerRole.getId()).thenReturn(7L);
        User user = User.builder()
                .githubUsername("octocat")
                .transcriptSummary("Sinh viên giỏi Java")
                .careerRole(careerRole)
                .build();
        MentorSession session = new MentorSession();
        session.setTitle("Chủ đề"); // custom để bỏ qua nhánh set title
        when(llmClient.ask(anyString())).thenReturn("ok");

        // When
        mentorService.sendMessage(user, session, "Tôi nên học gì tiếp theo?");

        // Then: bắt lại prompt thực sự gửi cho LLM và soi từng mảnh cá nhân hoá.
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).ask(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("Role ID #7");
        assertThat(prompt).contains("octocat");
        assertThat(prompt).contains("Sinh viên giỏi Java");
        assertThat(prompt).contains("Tôi nên học gì tiếp theo?");
    }

    @Test
    void sendMessage_buildsPrompt_withPlaceholders_whenProfileEmpty() {
        // Given: user chưa có hồ sơ -> prompt dùng các cụm placeholder "chưa...".
        User user = User.builder().build(); // careerRole=null, github=null, transcript=null
        MentorSession session = new MentorSession();
        session.setTitle("Chủ đề");
        when(llmClient.ask(anyString())).thenReturn("ok");

        // When
        mentorService.sendMessage(user, session, "Hỏi gì đó");

        // Then: mỗi nhánh null đều rơi vào chuỗi mặc định tương ứng.
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).ask(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("chưa chọn định hướng nghề nghiệp");
        assertThat(prompt).contains("chưa liên kết");
        assertThat(prompt).contains("chưa có / chưa phân tích bảng điểm");
    }
}
