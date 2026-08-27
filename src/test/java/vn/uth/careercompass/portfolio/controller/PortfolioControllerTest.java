package vn.uth.careercompass.portfolio.controller;

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
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.portfolio.dto.OwnerInfoDTO;
import vn.uth.careercompass.portfolio.entity.GitHubProfile;
import vn.uth.careercompass.portfolio.entity.ProjectRepository;
import vn.uth.careercompass.portfolio.service.PortfolioService;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = {PortfolioController.class, PublicPortfolioController.class}, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class, MarkdownRenderer.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
class PortfolioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PortfolioService portfolioService;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    void managePortfolio_whenNoProfile_rendersSuccessfully() throws Exception {
        User user = User.builder().id(1L).email("student@uth.edu.vn").fullName("Student")
                .role(Role.builder().name(RoleName.STUDENT).build())
                .authProvider(AuthProvider.LOCAL).build();
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        when(portfolioService.getProfileByUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/portfolio/manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/manage"));
    }

    @Test
    void managePortfolio_whenProfileExists_rendersWithRepos() throws Exception {
        User user = User.builder().id(1L).email("student@uth.edu.vn").fullName("Student")
                .role(Role.builder().name(RoleName.STUDENT).build())
                .authProvider(AuthProvider.LOCAL).build();
        GitHubProfile profile = GitHubProfile.builder().id(1L).userId(1L).githubUsername("testuser").slug("testuser-slug").build();
        ProjectRepository repo = ProjectRepository.builder().id(1L).repoName("my-repo").htmlUrl("https://github.com/testuser/my-repo").description("Repo desc").aiSummary("AI Summary").stars(5).isPublic(true).githubProfile(profile).build();

        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(user);
        when(portfolioService.getProfileByUser(any())).thenReturn(Optional.of(profile));
        when(portfolioService.getRepos(1L)).thenReturn(List.of(repo));

        mockMvc.perform(get("/portfolio/manage"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/manage"))
                .andExpect(model().attributeExists("repositories", "shareUrl"));
    }

    @Test
    void publicPortfolio_rendersSuccessfully() throws Exception {
        User owner = User.builder().id(1L).email("student@uth.edu.vn").fullName("Student")
                .role(Role.builder().name(RoleName.STUDENT).build())
                .authProvider(AuthProvider.LOCAL).build();
        GitHubProfile profile = GitHubProfile.builder().id(1L).userId(1L).githubUsername("testuser").slug("testuser-slug").build();
        ProjectRepository repo = ProjectRepository.builder().id(1L).repoName("my-repo").htmlUrl("https://github.com/testuser/my-repo").description("Repo desc").aiSummary("AI Summary").stars(5).isPublic(true).githubProfile(profile).build();
        OwnerInfoDTO ownerInfo = new OwnerInfoDTO("Backend Developer", List.of("Java", "Spring Boot"), "Phân tích điểm mạnh...");

        when(portfolioService.getProfileBySlug("testuser-slug")).thenReturn(Optional.of(profile));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(portfolioService.getOwnerInfo(1L)).thenReturn(ownerInfo);
        when(portfolioService.getPublicRepos(1L)).thenReturn(List.of(repo));

        mockMvc.perform(get("/p/testuser-slug"))
                .andExpect(status().isOk())
                .andExpect(view().name("portfolio/public"))
                .andExpect(model().attributeExists("profile", "owner", "ownerInfo", "repositories"));
    }
}
