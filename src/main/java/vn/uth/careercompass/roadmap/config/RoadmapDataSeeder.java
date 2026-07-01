package vn.uth.careercompass.roadmap.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.SkillNodeRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.admin.repository.SkillTreeTemplateRepository;

@Component
@RequiredArgsConstructor
public class RoadmapDataSeeder implements CommandLineRunner {
    private final SkillRepository skillRepository;
    private final SkillTreeTemplateRepository skillTreeTemplateRepository;
    private final SkillNodeRepository skillNodeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (skillTreeTemplateRepository.count() > 0) {
            return;
        }

        Skill javaCore = findOrCreateSkill("Java Core", "Programming");
        Skill springBoot = findOrCreateSkill("Spring Boot", "Backend");
        Skill sql = findOrCreateSkill("SQL", "Database");
        Skill restApi = findOrCreateSkill("REST API", "Backend");
        Skill git = findOrCreateSkill("Git", "Tools");

        SkillTreeTemplate template = skillTreeTemplateRepository.save(SkillTreeTemplate.builder()
                .name("Backend Developer Roadmap")
                .description("Default development roadmap for testing P4 Roadmap and Skill Gap APIs.")
                .active(true)
                .build());

        SkillNode javaNode = saveNode(template, javaCore, null, "Java Core", 1, 1);
        SkillNode sqlNode = saveNode(template, sql, null, "SQL Database", 1, 2);
        SkillNode gitNode = saveNode(template, git, null, "Git Version Control", 1, 3);
        SkillNode springNode = saveNode(template, springBoot, javaNode, "Spring Boot", 2, 1);
        saveNode(template, restApi, springNode, "REST API Design", 3, 1);
    }

    private Skill findOrCreateSkill(String name, String category) {
        return skillRepository.findByName(name)
                .orElseGet(() -> skillRepository.save(Skill.builder()
                        .name(name)
                        .category(category)
                        .build()));
    }

    private SkillNode saveNode(SkillTreeTemplate template, Skill skill, SkillNode parent, String title, int tier, int orderIndex) {
        return skillNodeRepository.save(SkillNode.builder()
                .template(template)
                .skill(skill)
                .parent(parent)
                .title(title)
                .tier(tier)
                .orderIndex(orderIndex)
                .requiredLevel(1)
                .build());
    }
}
