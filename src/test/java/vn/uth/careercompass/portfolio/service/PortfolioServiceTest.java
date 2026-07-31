package vn.uth.careercompass.portfolio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;
import vn.uth.careercompass.mentor.service.LlmClient;
import vn.uth.careercompass.portfolio.dto.OwnerInfoDTO;
import vn.uth.careercompass.portfolio.entity.GitHubProfile;
import vn.uth.careercompass.portfolio.entity.ProjectRepository;
import vn.uth.careercompass.portfolio.repository.GitHubProfileRepository;
import vn.uth.careercompass.portfolio.repository.ProjectRepositoryRepository;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link PortfolioService} — gói P6 E-Portfolio.
 *
 * <p>MỤC TIÊU: test RIÊNG logic service (đảo visibility repo, gom thông tin chủ portfolio,
 * đồng bộ repo GitHub + tóm tắt AI) mà KHÔNG bật Spring, KHÔNG gọi mạng thật.
 *
 * <p>KỸ THUẬT ĐẶC BIỆT ở file này: {@code restTemplate} KHÔNG phải dependency inject qua
 * constructor mà được service tự khởi tạo inline ({@code private final RestTemplate restTemplate = new RestTemplate();}).
 * Vì vậy {@code @InjectMocks} không thể tiêm mock vào đó. Ta phải "tiêm tay" mock RestTemplate
 * bằng {@link ReflectionTestUtils#setField} trong {@code @BeforeEach} — nhờ vậy mới chặn được
 * lời gọi HTTP tới GitHub và kiểm được logic map dữ liệu repo.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private GitHubProfileRepository gitHubProfileRepository;
    @Mock
    private ProjectRepositoryRepository projectRepositoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private UserNodeProgressRepository userNodeProgressRepository;
    @Mock
    private LlmClient llmClient;

    // RestTemplate được service tạo inline nên @InjectMocks bỏ qua field này. Ta chỉ dùng
    // mock này để gán tay vào SUT trong setUp() (xem @BeforeEach).
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PortfolioService portfolioService;

    @BeforeEach
    void setUp() {
        // Thay RestTemplate thật (new inline) bằng mock để không gọi GitHub API thật.
        // Đây là cách chuẩn xử lý dependency được khởi tạo bên trong class thay vì inject.
        ReflectionTestUtils.setField(portfolioService, "restTemplate", restTemplate);
    }

    // ============================================================
    // toggleRepoVisibility(user, repoId)
    // ============================================================

    @Test
    void toggleRepoVisibility_whenRepoBelongsToUser_togglesAndSaves() {
        // Given: user có profile (id=5), repo (id=10) thuộc đúng profile đó, đang public.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).build();
        ProjectRepository repo = ProjectRepository.builder()
                .id(10L).githubProfile(profile).isPublic(true).build();

        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(projectRepositoryRepository.findById(10L)).thenReturn(Optional.of(repo));

        // When
        portfolioService.toggleRepoVisibility(user, 10L);

        // Then: cờ isPublic bị đảo (true -> false) và repo được lưu lại.
        assertThat(repo.isIsPublic()).isFalse();
        verify(projectRepositoryRepository).save(repo);
    }

    @Test
    void toggleRepoVisibility_whenProfileMissing_doesNothing() {
        // Given: user chưa có GitHub profile -> ifPresent không chạy nhánh trong.
        User user = User.builder().id(1L).build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // When
        portfolioService.toggleRepoVisibility(user, 10L);

        // Then: không tra repo, không lưu gì.
        verify(projectRepositoryRepository, never()).findById(any());
        verify(projectRepositoryRepository, never()).save(any());
    }

    @Test
    void toggleRepoVisibility_whenRepoNotFound_doesNothing() {
        // Given: có profile nhưng repoId không tồn tại.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(projectRepositoryRepository.findById(99L)).thenReturn(Optional.empty());

        // When
        portfolioService.toggleRepoVisibility(user, 99L);

        // Then: không lưu.
        verify(projectRepositoryRepository, never()).save(any());
    }

    @Test
    void toggleRepoVisibility_whenRepoBelongsToAnotherProfile_doesNotToggle() {
        // Given: repo thuộc profile khác (id=7) chứ không phải profile của user (id=5).
        // Đây là guard chống user A đổi visibility repo của user B.
        User user = User.builder().id(1L).build();
        GitHubProfile myProfile = GitHubProfile.builder().id(5L).userId(1L).build();
        GitHubProfile otherProfile = GitHubProfile.builder().id(7L).build();
        ProjectRepository repo = ProjectRepository.builder()
                .id(10L).githubProfile(otherProfile).isPublic(true).build();

        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(myProfile));
        when(projectRepositoryRepository.findById(10L)).thenReturn(Optional.of(repo));

        // When
        portfolioService.toggleRepoVisibility(user, 10L);

        // Then: KHÔNG đảo cờ, KHÔNG lưu.
        assertThat(repo.isIsPublic()).isTrue();
        verify(projectRepositoryRepository, never()).save(any());
    }

    @Test
    void toggleRepoVisibility_whenRepoHasNullProfile_doesNotToggle() {
        // Given: repo mồ côi (githubProfile == null) -> guard chặn NPE + không đảo.
        User user = User.builder().id(1L).build();
        GitHubProfile myProfile = GitHubProfile.builder().id(5L).userId(1L).build();
        ProjectRepository repo = ProjectRepository.builder()
                .id(10L).githubProfile(null).isPublic(true).build();

        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(myProfile));
        when(projectRepositoryRepository.findById(10L)).thenReturn(Optional.of(repo));

        // When
        portfolioService.toggleRepoVisibility(user, 10L);

        // Then: giữ nguyên, không lưu.
        assertThat(repo.isIsPublic()).isTrue();
        verify(projectRepositoryRepository, never()).save(any());
    }

    // ============================================================
    // getOwnerInfo(userId)
    // ============================================================

    @Test
    void getOwnerInfo_whenOwnerNotFound_returnsEmptyDto() {
        // Given: userId không tồn tại -> service trả DTO rỗng an toàn (không NPE).
        when(userRepository.findById(404L)).thenReturn(Optional.empty());

        // When
        OwnerInfoDTO dto = portfolioService.getOwnerInfo(404L);

        // Then: mọi field rỗng, KHÔNG tra kỹ năng (dừng sớm).
        assertThat(dto.careerGoal()).isNull();
        assertThat(dto.skills()).isEmpty();
        assertThat(dto.strength()).isNull();
        verify(userSkillRepository, never()).findByUserWithSkill(any());
        verify(userNodeProgressRepository, never()).findByUserAndStatusWithSkill(any(), any());
    }

    @Test
    void getOwnerInfo_whenOwnerExists_mergesSkillsSortedAndDistinct() {
        // Given: owner có mục tiêu nghề, kỹ năng tự khai + kỹ năng hoàn thành trong Roadmap.
        CareerRole careerRole = CareerRole.builder().name("Backend Engineer").build();
        User owner = User.builder()
                .id(1L)
                .careerRole(careerRole)
                .transcriptSummary("Điểm mạnh: nền tảng vững")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        // Kỹ năng tự khai: Java, Docker
        UserSkill usJava = UserSkill.builder().skill(Skill.builder().name("Java").build()).build();
        UserSkill usDocker = UserSkill.builder().skill(Skill.builder().name("Docker").build()).build();
        when(userSkillRepository.findByUserWithSkill(owner)).thenReturn(List.of(usJava, usDocker));

        // Kỹ năng hoàn thành trong Roadmap: Java (trùng), Kubernetes
        UserNodeProgress pJava = progressWithSkill("Java");
        UserNodeProgress pK8s = progressWithSkill("Kubernetes");
        when(userNodeProgressRepository.findByUserAndStatusWithSkill(owner, ProgressStatus.DONE))
                .thenReturn(List.of(pJava, pK8s));

        // When
        OwnerInfoDTO dto = portfolioService.getOwnerInfo(1L);

        // Then: careerGoal lấy từ CareerRole; strength = transcriptSummary.
        assertThat(dto.careerGoal()).isEqualTo("Backend Engineer");
        assertThat(dto.strength()).isEqualTo("Điểm mạnh: nền tảng vững");
        // TreeSet -> khử trùng "Java" + sắp xếp theo alphabet.
        assertThat(dto.skills()).containsExactly("Docker", "Java", "Kubernetes");
    }

    @Test
    void getOwnerInfo_whenNoCareerRoleAndNoSkills_returnsNullGoalAndEmptySkills() {
        // Given: owner chưa chọn nghề, chưa có kỹ năng nào.
        User owner = User.builder().id(2L).careerRole(null).transcriptSummary(null).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(owner));
        when(userSkillRepository.findByUserWithSkill(owner)).thenReturn(List.of());
        when(userNodeProgressRepository.findByUserAndStatusWithSkill(owner, ProgressStatus.DONE))
                .thenReturn(List.of());

        // When
        OwnerInfoDTO dto = portfolioService.getOwnerInfo(2L);

        // Then: careerGoal null (careerRole == null), skills rỗng, strength null.
        assertThat(dto.careerGoal()).isNull();
        assertThat(dto.skills()).isEmpty();
        assertThat(dto.strength()).isNull();
    }

    // ============================================================
    // Các method đọc dữ liệu (delegate thẳng xuống repository)
    // ============================================================

    @Test
    void getProfileByUser_delegatesToRepositoryByUserId() {
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        assertThat(portfolioService.getProfileByUser(user)).contains(profile);
    }

    @Test
    void getProfileBySlug_delegatesToRepositoryBySlug() {
        GitHubProfile profile = GitHubProfile.builder().id(5L).slug("octocat-abc123").build();
        when(gitHubProfileRepository.findBySlug("octocat-abc123")).thenReturn(Optional.of(profile));

        assertThat(portfolioService.getProfileBySlug("octocat-abc123")).contains(profile);
    }

    @Test
    void getRepos_delegatesToRepositoryByProfileId() {
        List<ProjectRepository> repos = List.of(ProjectRepository.builder().id(1L).build());
        when(projectRepositoryRepository.findByGithubProfileId(5L)).thenReturn(repos);

        assertThat(portfolioService.getRepos(5L)).isEqualTo(repos);
    }

    @Test
    void getPublicRepos_delegatesToPublicOnlyQuery() {
        List<ProjectRepository> repos = List.of(ProjectRepository.builder().id(1L).build());
        when(projectRepositoryRepository.findByGithubProfileIdAndIsPublicTrue(5L)).thenReturn(repos);

        assertThat(portfolioService.getPublicRepos(5L)).isEqualTo(repos);
    }

    // ============================================================
    // syncGithubRepositories(user, githubUsername)
    // ============================================================

    @Test
    void syncGithubRepositories_whenUsernameNull_throwsIllegalArgument() {
        // Given: username null -> guard chặn ngay, không đụng repo/HTTP.
        User user = User.builder().id(1L).build();

        assertThatThrownBy(() -> portfolioService.syncGithubRepositories(user, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vui lòng nhập GitHub username!");

        verify(gitHubProfileRepository, never()).findByUserId(any());
        verify(gitHubProfileRepository, never()).save(any());
    }

    @Test
    void syncGithubRepositories_whenUsernameBlank_throwsIllegalArgument() {
        // Given: username toàn khoảng trắng -> cũng bị guard chặn.
        User user = User.builder().id(1L).build();

        assertThatThrownBy(() -> portfolioService.syncGithubRepositories(user, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vui lòng nhập GitHub username!");

        verify(gitHubProfileRepository, never()).save(any());
    }

    @Test
    void syncGithubRepositories_happyPath_savesRepoWithAiSummaryFromMainReadme() {
        // Given: user đã có profile (id=5). GitHub trả 1 repo, README lấy được ở branch main,
        // LLM tóm tắt thành công.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).slug("octocat-old").build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        // save trả về chính đối tượng truyền vào (giữ nguyên id=5 để deleteByGithubProfileId nhận đúng).
        when(gitHubProfileRepository.save(any(GitHubProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> repoData = repoMap("Hello-World",
                "https://github.com/octocat/Hello-World", "Repo demo", 42);
        when(restTemplate.getForObject(
                "https://api.github.com/users/octocat/repos?per_page=100&sort=updated", List.class))
                .thenReturn(List.of(repoData));
        // README lấy được ở branch main -> không đụng master.
        when(restTemplate.getForObject(
                "https://raw.githubusercontent.com/octocat/Hello-World/main/README.md", String.class))
                .thenReturn("# Hello World\nDự án mẫu");
        when(llmClient.ask(anyString())).thenReturn("Đây là dự án mẫu.");

        // when project save trả lại chính repo (để list kết quả có phần tử)
        when(projectRepositoryRepository.save(any(ProjectRepository.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // When
        List<ProjectRepository> result = portfolioService.syncGithubRepositories(user, "octocat");

        // Then: xoá repo cũ theo đúng profileId trước khi lưu mới.
        verify(projectRepositoryRepository).deleteByGithubProfileId(5L);

        // Repo được map đúng field từ JSON GitHub.
        ArgumentCaptor<ProjectRepository> repoCaptor = ArgumentCaptor.forClass(ProjectRepository.class);
        verify(projectRepositoryRepository).save(repoCaptor.capture());
        ProjectRepository saved = repoCaptor.getValue();
        assertThat(saved.getRepoName()).isEqualTo("Hello-World");
        assertThat(saved.getHtmlUrl()).isEqualTo("https://github.com/octocat/Hello-World");
        assertThat(saved.getDescription()).isEqualTo("Repo demo");
        assertThat(saved.getStars()).isEqualTo(42);
        assertThat(saved.isIsPublic()).isTrue();
        assertThat(saved.getGithubProfile()).isEqualTo(profile);
        assertThat(saved.getAiSummary()).isEqualTo("Đây là dự án mẫu.");

        assertThat(result).hasSize(1);
    }

    @Test
    void syncGithubRepositories_whenNoProfileYet_createsProfileWithGeneratedSlug() {
        // Given: user CHƯA có profile -> service tạo mới kèm slug sinh tự động.
        User user = User.builder().id(1L).build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(gitHubProfileRepository.save(any(GitHubProfile.class))).thenAnswer(inv -> {
            GitHubProfile p = inv.getArgument(0);
            p.setId(9L); // giả lập DB gán khoá chính
            return p;
        });
        // GitHub trả rỗng để tập trung kiểm việc tạo profile.
        when(restTemplate.getForObject(
                "https://api.github.com/users/octocat/repos?per_page=100&sort=updated", List.class))
                .thenReturn(List.of());

        // When
        portfolioService.syncGithubRepositories(user, "octocat");

        // Then: profile mới có username + slug dạng "octocat-xxxxxx" (6 ký tự UUID).
        ArgumentCaptor<GitHubProfile> profileCaptor = ArgumentCaptor.forClass(GitHubProfile.class);
        verify(gitHubProfileRepository).save(profileCaptor.capture());
        GitHubProfile savedProfile = profileCaptor.getValue();
        assertThat(savedProfile.getGithubUsername()).isEqualTo("octocat");
        assertThat(savedProfile.getUserId()).isEqualTo(1L);
        assertThat(savedProfile.getSlug()).matches("octocat-[a-z0-9]{6}");
    }

    @Test
    void syncGithubRepositories_whenReadmeOnMasterBranch_usesMasterContent() {
        // Given: branch main lỗi (404/khác) nhưng master có README -> service phải fallback sang master.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).slug("octocat-s").build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(gitHubProfileRepository.save(any(GitHubProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepositoryRepository.save(any(ProjectRepository.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(restTemplate.getForObject(
                "https://api.github.com/users/octocat/repos?per_page=100&sort=updated", List.class))
                .thenReturn(List.of(repoMap("Legacy", "url", "desc", 0)));
        // main ném lỗi mạng -> thử master.
        when(restTemplate.getForObject(
                "https://raw.githubusercontent.com/octocat/Legacy/main/README.md", String.class))
                .thenThrow(new RestClientException("main not found"));
        when(restTemplate.getForObject(
                "https://raw.githubusercontent.com/octocat/Legacy/master/README.md", String.class))
                .thenReturn("Nội dung README ở master");
        when(llmClient.ask(anyString())).thenReturn("Tóm tắt master.");

        // When
        portfolioService.syncGithubRepositories(user, "octocat");

        // Then: prompt gửi cho LLM phải chứa nội dung README lấy từ master.
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).ask(promptCaptor.capture());
        assertThat(promptCaptor.getValue()).contains("Nội dung README ở master");
    }

    @Test
    void syncGithubRepositories_whenNoReadmeAndNoDescription_usesDefaultSummaryWithoutLlm() {
        // Given: cả 2 branch README đều lỗi và repo không có description -> không đủ dữ liệu cho AI.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).slug("octocat-s").build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(gitHubProfileRepository.save(any(GitHubProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepositoryRepository.save(any(ProjectRepository.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // repoMap với description = null (dùng HashMap để cho phép null).
        when(restTemplate.getForObject(
                "https://api.github.com/users/octocat/repos?per_page=100&sort=updated", List.class))
                .thenReturn(List.of(repoMap("Empty", "url", null, 0)));
        when(restTemplate.getForObject(
                "https://raw.githubusercontent.com/octocat/Empty/main/README.md", String.class))
                .thenThrow(new RestClientException("no main"));
        when(restTemplate.getForObject(
                "https://raw.githubusercontent.com/octocat/Empty/master/README.md", String.class))
                .thenThrow(new RestClientException("no master"));

        // When
        portfolioService.syncGithubRepositories(user, "octocat");

        // Then: AI summary = câu mặc định, và KHÔNG gọi LLM (tiết kiệm token/không có input).
        ArgumentCaptor<ProjectRepository> repoCaptor = ArgumentCaptor.forClass(ProjectRepository.class);
        verify(projectRepositoryRepository).save(repoCaptor.capture());
        assertThat(repoCaptor.getValue().getAiSummary())
                .isEqualTo("Dự án chưa có README/mô tả để AI tóm tắt.");
        verify(llmClient, never()).ask(anyString());
    }

    @Test
    void syncGithubRepositories_whenLlmFails_fallbackToBasicSummary() {
        // Given: có README nhưng LLM ném lỗi (thiếu key/API down) -> fallback tóm tắt cơ bản, không chặn sync.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).slug("octocat-s").build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(gitHubProfileRepository.save(any(GitHubProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(projectRepositoryRepository.save(any(ProjectRepository.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(restTemplate.getForObject(
                "https://api.github.com/users/octocat/repos?per_page=100&sort=updated", List.class))
                .thenReturn(List.of(repoMap("MyApp", "url", "desc", 3)));
        when(restTemplate.getForObject(
                "https://raw.githubusercontent.com/octocat/MyApp/main/README.md", String.class))
                .thenReturn("Nội dung readme dùng cho fallback");
        when(llmClient.ask(anyString())).thenThrow(new RuntimeException("LLM down"));

        // When
        portfolioService.syncGithubRepositories(user, "octocat");

        // Then: summary bắt đầu bằng nhãn fallback + tên repo.
        ArgumentCaptor<ProjectRepository> repoCaptor = ArgumentCaptor.forClass(ProjectRepository.class);
        verify(projectRepositoryRepository).save(repoCaptor.capture());
        assertThat(repoCaptor.getValue().getAiSummary())
                .startsWith("[Tóm tắt cơ bản] MyApp — ")
                .contains("Nội dung readme dùng cho fallback");
    }

    @Test
    void syncGithubRepositories_whenReposResponseNull_savesNothingButStillDeletesOld() {
        // Given: GitHub trả null (hiếm) -> fetchRepos quy về List.of() -> vòng lặp không chạy.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).slug("octocat-s").build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(gitHubProfileRepository.save(any(GitHubProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(restTemplate.getForObject(
                "https://api.github.com/users/octocat/repos?per_page=100&sort=updated", List.class))
                .thenReturn(null);

        // When
        List<ProjectRepository> result = portfolioService.syncGithubRepositories(user, "octocat");

        // Then: vẫn xoá repo cũ, nhưng không lưu repo mới, kết quả rỗng.
        verify(projectRepositoryRepository).deleteByGithubProfileId(5L);
        verify(projectRepositoryRepository, never()).save(any());
        assertThat(result).isEmpty();
    }

    @Test
    void syncGithubRepositories_whenGithubUserNotFound_throwsIllegalArgument() {
        // Given: GitHub trả 404 (user không tồn tại) -> dịch sang thông báo thân thiện.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).slug("ghost-s").build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(gitHubProfileRepository.save(any(GitHubProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(restTemplate.getForObject(
                "https://api.github.com/users/ghost/repos?per_page=100&sort=updated", List.class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.NOT_FOUND, "Not Found", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8));

        // When + Then
        assertThatThrownBy(() -> portfolioService.syncGithubRepositories(user, "ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy user GitHub 'ghost'.");

        verify(projectRepositoryRepository, never()).save(any());
    }

    @Test
    void syncGithubRepositories_whenGithubApiRateLimited_throwsIllegalState() {
        // Given: lỗi RestClientException chung (vd rate limit) -> đổi sang IllegalStateException.
        User user = User.builder().id(1L).build();
        GitHubProfile profile = GitHubProfile.builder().id(5L).userId(1L).slug("octocat-s").build();
        when(gitHubProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(gitHubProfileRepository.save(any(GitHubProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(restTemplate.getForObject(
                "https://api.github.com/users/octocat/repos?per_page=100&sort=updated", List.class))
                .thenThrow(new RestClientException("429 rate limited"));

        // When + Then
        assertThatThrownBy(() -> portfolioService.syncGithubRepositories(user, "octocat"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không gọi được GitHub API");

        verify(projectRepositoryRepository, never()).save(any());
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Dựng 1 map dữ liệu repo giống JSON GitHub trả về. Dùng HashMap để cho phép description = null. */
    private Map<String, Object> repoMap(String name, String htmlUrl, String description, int stars) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("html_url", htmlUrl);
        m.put("description", description);
        m.put("stargazers_count", stars);
        return m;
    }

    /** Dựng 1 UserNodeProgress mà chuỗi getSkillNode().getSkill().getName() trả về skillName. */
    private UserNodeProgress progressWithSkill(String skillName) {
        SkillNode node = SkillNode.builder().skill(Skill.builder().name(skillName).build()).build();
        return UserNodeProgress.builder().skillNode(node).build();
    }
}
