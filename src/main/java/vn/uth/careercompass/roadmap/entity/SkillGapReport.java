package vn.uth.careercompass.roadmap.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.kernel.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "skill_gap_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGapReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "template_id", nullable = false)
    @NotNull
    private SkillTreeTemplate template;

    @Column(length = 500)
    private String summary;

    @Column(length = 500)
    private String pdfPath;

    private Integer requiredSkillCount;

    private Integer matchedSkillCount;

    private Integer missingSkillCount;

    @Column(columnDefinition = "TEXT")
    private String matchedSkillNames;

    @Column(columnDefinition = "TEXT")
    private String missingSkillNames;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
}
