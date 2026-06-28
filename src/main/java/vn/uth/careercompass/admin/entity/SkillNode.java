package vn.uth.careercompass.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "skill_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_tree_template_id", nullable = false)
    private SkillTreeTemplate skillTreeTemplate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private Integer level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private SkillNode parentNode;

    @OneToMany(mappedBy = "parentNode", cascade = CascadeType.ALL)
    @Builder.Default
    private List<SkillNode> childNodes = new ArrayList<>();

    @OneToMany(mappedBy = "skillNode", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LearningResource> learningResources = new ArrayList<>();

}
