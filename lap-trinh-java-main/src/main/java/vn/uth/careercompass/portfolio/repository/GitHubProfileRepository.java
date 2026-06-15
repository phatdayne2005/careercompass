package vn.uth.careercompass.portfolio.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GitHubProfileRepository extends JpaRepository<GitHubProfileRepository, Long> {

    Optional<GitHubProfileRepository> findByUserId(Long userId);
    
    boolean existsByGithubUsername(String githubUsername);
}