package vn.uth.careercompass.mentor.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.kernel.entity.AuthProvider;
import vn.uth.careercompass.kernel.entity.Role;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.mentor.entity.ChatMessage;
import vn.uth.careercompass.mentor.entity.MentorSession;
import vn.uth.careercompass.mentor.entity.Sender;
import vn.uth.careercompass.mentor.service.MentorService;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = MentorController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class, MarkdownRenderer.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
class MentorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MentorService mentorService;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @Test
    void chatPage_rendersSuccessfully() throws Exception {
        User user = User.builder().id(1L).email("student@uth.edu.vn").fullName("Student")
                .role(Role.builder().name(RoleName.STUDENT).build())
                .authProvider(AuthProvider.LOCAL).build();
        MentorSession session = new MentorSession();
        session.setId(1L);
        session.setTitle("Cuộc trò chuyện 1");
        session.setUser(user);

        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        when(mentorService.getSessionsForUser(user)).thenReturn(List.of(session));
        when(mentorService.getMessages(session)).thenReturn(List.of());

        mockMvc.perform(get("/mentor"))
                .andExpect(status().isOk())
                .andExpect(view().name("mentor/chat"))
                .andExpect(model().attributeExists("sessions", "currentSessionId", "messages"));
    }

    @Test
    void sendMessage_returnsMessageListFragment() throws Exception {
        User user = User.builder().id(1L).email("student@uth.edu.vn").fullName("Student")
                .role(Role.builder().name(RoleName.STUDENT).build())
                .authProvider(AuthProvider.LOCAL).build();
        MentorSession session = new MentorSession();
        session.setId(1L);
        session.setTitle("Cuộc trò chuyện 1");
        session.setUser(user);

        ChatMessage userMsg = new ChatMessage();
        userMsg.setId(1L);
        userMsg.setSender(Sender.USER);
        userMsg.setContent("Làm sao để học Java?");
        userMsg.setCreatedAt(LocalDateTime.now());

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setId(2L);
        aiMsg.setSender(Sender.AI);
        aiMsg.setContent("Bạn nên bắt đầu với OOP và Java Core.");
        aiMsg.setCreatedAt(LocalDateTime.now());

        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        when(mentorService.getSessionsForUser(user)).thenReturn(List.of(session));
        when(mentorService.getMessages(session)).thenReturn(List.of(userMsg, aiMsg));

        mockMvc.perform(post("/mentor/send")
                        .param("sessionId", "1")
                        .param("content", "Làm sao để học Java?"))
                .andExpect(status().isOk())
                .andExpect(view().name("mentor/chat :: messageList"))
                .andExpect(model().attributeExists("messages", "currentSessionId"));
    }

    // ================================================================
    // Các nhánh chọn phiên chat (FR1.1)
    //
    // Trước đây chỉ có đường đi "đã truyền sessionId và tìm thấy". Ba nhánh còn lại đều
    // là đường người dùng thật đi qua: mở /mentor lần đầu, mở lại sau khi đã có phiên,
    // và mở bằng một sessionId không thuộc về mình.
    // ================================================================

    private static User nguoiDung() {
        return User.builder().id(1L).email("student@uth.edu.vn").fullName("Student")
                .role(Role.builder().name(RoleName.STUDENT).build())
                .authProvider(AuthProvider.LOCAL).build();
    }

    private static MentorSession phien(long id, User u) {
        MentorSession s = new MentorSession();
        s.setId(id);
        s.setTitle("Cuộc trò chuyện " + id);
        s.setUser(u);
        return s;
    }

    @Test
    void chatPage_chuaCoPhienNao_tuDongTaoPhienDauTien() throws Exception {
        User user = nguoiDung();
        MentorSession moi = phien(9L, user);
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        // Lượt đầu chưa có phiên nào; sau khi tạo thì danh sách có một phiên.
        when(mentorService.getSessionsForUser(user)).thenReturn(List.of(), List.of(moi));
        when(mentorService.createSession(user)).thenReturn(moi);
        when(mentorService.getMessages(moi)).thenReturn(List.of());

        mockMvc.perform(get("/mentor"))
                .andExpect(status().isOk())
                .andExpect(view().name("mentor/chat"))
                .andExpect(model().attribute("currentSessionId", 9L));

        verify(mentorService).createSession(user);
    }

    @Test
    void chatPage_khongTruyenSessionId_layPhienMoiNhat() throws Exception {
        User user = nguoiDung();
        MentorSession moiNhat = phien(5L, user);
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        // Danh sách đã sắp theo thời gian giảm dần nên phần tử đầu là phiên mới nhất.
        when(mentorService.getSessionsForUser(user))
                .thenReturn(List.of(moiNhat, phien(4L, user)));
        when(mentorService.getMessages(moiNhat)).thenReturn(List.of());

        mockMvc.perform(get("/mentor"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentSessionId", 5L));

        verify(mentorService, never()).createSession(any());
    }

    @Test
    void chatPage_sessionIdKhongThuocVeMinh_quayVePhienMoiNhat() throws Exception {
        User user = nguoiDung();
        MentorSession cuaMinh = phien(5L, user);
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        when(mentorService.getSessionsForUser(user)).thenReturn(List.of(cuaMinh));
        when(mentorService.getMessages(cuaMinh)).thenReturn(List.of());

        // sessionId 999 là phiên của người khác — lọc không ra nên rơi về phiên mới nhất
        // của chính mình, KHÔNG được mở phiên của người khác (AC-1.1.3).
        mockMvc.perform(get("/mentor").param("session", "999"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentSessionId", 5L));
    }

    @Test
    void createSession_taoPhienMoiRoiChuyenHuongKemId() throws Exception {
        User user = nguoiDung();
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        when(mentorService.createSession(user)).thenReturn(phien(7L, user));

        mockMvc.perform(post("/mentor/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mentor?session=7"));
    }

    @Test
    void sendMessage_khongTruyenSessionId_tuDongTaoPhien() throws Exception {
        User user = nguoiDung();
        MentorSession moi = phien(3L, user);
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        when(mentorService.createSession(user)).thenReturn(moi);
        when(mentorService.getMessages(moi)).thenReturn(List.of());

        mockMvc.perform(post("/mentor/send").param("content", "Em nên học gì?"))
                .andExpect(status().isOk())
                .andExpect(view().name("mentor/chat :: messageList"))
                .andExpect(model().attribute("currentSessionId", 3L));

        verify(mentorService).createSession(user);
        verify(mentorService).sendMessage(user, moi, "Em nên học gì?");
    }

    @Test
    void sendMessage_sessionIdKhongThuocVeMinh_taoPhienMoiThayViGhiNhoNguoiKhac() throws Exception {
        User user = nguoiDung();
        MentorSession moi = phien(3L, user);
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        when(mentorService.getSessionsForUser(user)).thenReturn(List.of(phien(5L, user)));
        when(mentorService.createSession(user)).thenReturn(moi);
        when(mentorService.getMessages(moi)).thenReturn(List.of());

        mockMvc.perform(post("/mentor/send")
                        .param("sessionId", "999")
                        .param("content", "Em nên học gì?"))
                .andExpect(status().isOk());

        // Tin nhắn KHÔNG được ghi vào phiên 999 của người khác (AC-1.1.3).
        verify(mentorService).sendMessage(user, moi, "Em nên học gì?");
    }
}
