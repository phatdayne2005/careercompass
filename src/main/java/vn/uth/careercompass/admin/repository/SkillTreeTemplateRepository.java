package vn.uth.careercompass.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillTreeTemplateRepository extends JpaRepository<SkillTreeTemplate, Long> {
    List<SkillTreeTemplate> findByActiveTrueOrderByNameAsc();

    Optional<SkillTreeTemplate> findFirstByActiveTrueOrderByNameAsc();

    Optional<SkillTreeTemplate> findFirstByTargetRoleIdAndActiveTrueOrderByIdAsc(Long targetRoleId);
}
