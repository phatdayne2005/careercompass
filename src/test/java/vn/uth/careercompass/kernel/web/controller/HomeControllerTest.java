package vn.uth.careercompass.kernel.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = HomeController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    @Test
    void home_whenUnauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/login"));
    }

    @Test
    @WithMockUser(username = "admin@uth.edu.vn", roles = "ADMIN")
    void home_whenAdmin_redirectsToAdmin() throws Exception {
        mockMvc.perform(get("/")
                        .with(user("admin@uth.edu.vn").roles("ADMIN")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/admin"));
    }

    @Test
    @WithMockUser(username = "counselor@uth.edu.vn", roles = "COUNSELOR")
    void home_whenCounselor_redirectsToCounselorTemplates() throws Exception {
        mockMvc.perform(get("/")
                        .with(user("counselor@uth.edu.vn").roles("COUNSELOR")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/counselor/templates"));
    }

    @Test
    @WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
    void home_whenStudentHasNotCompletedOnboarding_redirectsToStep1() throws Exception {
        User userAccount = User.builder().email("student@uth.edu.vn").onboardingCompleted(false).build();
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(get("/")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/onboarding/step1"));
    }

    @Test
    @WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
    void home_whenStudentCompletedOnboarding_redirectsToDashboard() throws Exception {
        User userAccount = User.builder().email("student@uth.edu.vn").onboardingCompleted(true).build();
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(get("/")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"));
    }
}
