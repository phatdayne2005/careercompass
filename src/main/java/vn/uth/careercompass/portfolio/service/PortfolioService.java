package vn.uth.careercompass.portfolio.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import vn.uth.careercompass.portfolio.entity.GitHubProfile;
import vn.uth.careercompass.portfolio.entity.ProjectRepository;
import vn.uth.careercompass.portfolio.repository.GitHubProfileRepository;
import vn.uth.careercompass.portfolio.repository.ProjectRepositoryRepository;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final GitHubProfileRepository gitHubProfileRepository;
    private final ProjectRepositoryRepository projectRepositoryRepository;
    private final RestTemplate restTemplate = new RestTemplate(); 

    public List<ProjectRepository> syncGithubRepositories(String githubUsername) {
        Long mockUserId = 1L; 

        GitHubProfile profile = gitHubProfileRepository.findByUserId(mockUserId)
                .orElseGet(() -> createGitHubProfile(githubUsername));

        String githubApiUrl = "https://api.github.com/users/" + githubUsername + "/repos";
        List<Map<String, Object>> response = restTemplate.getForObject(githubApiUrl, List.class);
        List<ProjectRepository> savedRepos = new ArrayList<>();

        if (response != null) {
            for (Map<String, Object> repoData : response) {
                String repoName = (String) repoData.get("name");
                String htmlUrl = (String) repoData.get("html_url");
                String description = (String) repoData.get("description");

                String readmeContent = fetchReadmeContent(githubUsername, repoName);

                String aiSummaryText;
                if (!readmeContent.isEmpty()) {
                    aiSummaryText = generateAiSummary(repoName, readmeContent);
                } else {
                    aiSummaryText = "Dự án chưa có file README.md. " + (description != null ? description : "Chưa có mô tả.");
                }

                ProjectRepository repository = ProjectRepository.builder()
                        .repoName(repoName)
                        .htmlUrl(htmlUrl)
                        .description(description)
                        .isPublic(true)
                        .githubProfile(profile)
                        .aiSummary(aiSummaryText) 
                        .build();

                savedRepos.add(projectRepositoryRepository.save(repository));
            }
        }
        return savedRepos;
    }

    private GitHubProfile createGitHubProfile(String githubUsername) {
        Long mockUserId = 1L;
        GitHubProfile profile = GitHubProfile.builder()
                .userId(mockUserId)
                .githubUsername(githubUsername)
                .build();
        return gitHubProfileRepository.save(profile);
    }

    private String fetchReadmeContent(String username, String repoName) {
        String readmeUrl = "https://raw.githubusercontent.com/" + username + "/" + repoName + "/main/README.md";
        try {
            return restTemplate.getForObject(readmeUrl, String.class);
        } catch (HttpClientErrorException.NotFound e) {
            
            try {
                String fallbackUrl = "https://raw.githubusercontent.com/" + username + "/" + repoName + "/master/README.md";
                return restTemplate.getForObject(fallbackUrl, String.class);
            } catch (HttpClientErrorException ex) {
                return ""; 
            }
        } catch (HttpClientErrorException e) {
            return "";
        } catch (org.springframework.web.client.RestClientException e) {
            return "";
        }
    }

    private String generateAiSummary(String repoName, String readmeContent) {
        String safeReadme = readmeContent == null ? "" : readmeContent;
        String truncatedReadme = safeReadme.length() > 2000 ? safeReadme.substring(0, 2000) : safeReadme;

        return "[AI Phân Tích]: Dự án triển khai hệ thống '" + repoName + "' ứng dụng các công nghệ hiện đại. " +
               "Tối ưu hóa kiến trúc xử lý dữ liệu giúp nâng cao trải nghiệm người dùng và sẵn sàng tích hợp hệ thống." +
               " Nội dung README mẫu: " + truncatedReadme;
    }
}
