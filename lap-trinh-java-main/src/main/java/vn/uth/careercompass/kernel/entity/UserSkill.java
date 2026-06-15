package vn.uth.careercompass.kernel.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import vn.uth.careercompass.admin.entity.Skill;

@Entity
@Table(name = "users_skills",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_skills_user_skill", columnNames = {"user_id", "skill_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull
    private User user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    @NotNull
    private Skill skill;
}
