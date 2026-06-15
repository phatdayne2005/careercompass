package vn.uth.careercompass.portfolio.service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import vn.uth.careercompass.portfolio.entity.GitHubProfile;
import vn.uth.careercompass.portfolio.entity.ProjectRepository;
import vn.uth.careercompass.portfolio.repository.GitHubProfileRepository;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final GitHubProfileRepository gitHubProfileRepository;
    
    private final Object projectRepositoryRepository;

    public Optional<GitHubProfile> getProfileByUserId(Long userId) {
        
        Object res = gitHubProfileRepository.findByUserId(userId);
        if (res instanceof Optional) {
            @SuppressWarnings("unchecked")
            Optional<?> opt = (Optional<?>) res;
            return opt.map(o -> (GitHubProfile) o);
        }
        return Optional.empty();
    }

    public List<ProjectRepository> getPublicRepositories(Long githubProfileId) {
        try {
            Method m = projectRepositoryRepository.getClass()
                    .getMethod("findByGithubProfileIdAndIsPublicTrue", Long.class);
            Object res = m.invoke(projectRepositoryRepository, githubProfileId);
            @SuppressWarnings("unchecked")
            List<ProjectRepository> list = (List<ProjectRepository>) res;
            return list;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException e) {
            return Collections.emptyList();
        }
    }

    public void toggleRepoVisibility(Long repoId, boolean isPublic) {
        try {
            Method findById = projectRepositoryRepository.getClass().getMethod("findById", Object.class);
            Object opt = findById.invoke(projectRepositoryRepository, repoId);
            if (opt instanceof java.util.Optional) {
                java.util.Optional<?> optional = (java.util.Optional<?>) opt;
                if (optional.isPresent()) {
                    Object repo = optional.get();
                    try {
                        Method setPublic = repo.getClass().getMethod("setPublic", boolean.class);
                        setPublic.invoke(repo, isPublic);
                    } catch (NoSuchMethodException ignored) {
                    }
                    Method save = projectRepositoryRepository.getClass().getMethod("save", Object.class);
                    save.invoke(projectRepositoryRepository, repo);
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException e) {

        }
    }

}