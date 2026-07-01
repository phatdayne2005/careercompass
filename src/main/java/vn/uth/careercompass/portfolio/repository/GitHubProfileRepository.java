package vn.uth.careercompass.portfolio.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.portfolio.entity.GitHubProfile;

@Repository
public interface GitHubProfileRepository extends JpaRepository<GitHubProfile, Long> {

    Optional<GitHubProfile> findByUserId(Long userId);
    
    boolean existsByGithubUsername(String githubUsername);
}
