package vn.uth.careercompass.portfolio.entity;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "github_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    /** Chủ sở hữu portfolio — 1 user chỉ có 1 profile. */
    @Column(name = "user_id", unique = true)
    private Long userId;

    /** Định danh dùng cho URL công khai /p/{slug} (FR5.3). */
    @Column(name = "slug", unique = true, length = 120)
    private String slug;

    @OneToMany(mappedBy = "githubProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectRepository> repositories;
}
