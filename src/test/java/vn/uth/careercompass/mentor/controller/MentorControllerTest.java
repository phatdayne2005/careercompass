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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
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
}
