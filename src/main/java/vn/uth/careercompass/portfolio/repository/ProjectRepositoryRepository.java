package vn.uth.careercompass.portfolio.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.uth.careercompass.portfolio.entity.ProjectRepository;

@Repository
public interface ProjectRepositoryRepository extends JpaRepository<ProjectRepository, Long> {

    List<ProjectRepository> findByGithubProfileId(Long githubProfileId);

    List<ProjectRepository> findByGithubProfileIdAndIsPublicTrue(Long githubProfileId);

    /** Xoá toàn bộ repo cũ của 1 profile trước khi sync lại (tránh nhân đôi). */
    void deleteByGithubProfileId(Long githubProfileId);
}

