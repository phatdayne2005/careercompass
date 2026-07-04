package vn.uth.careercompass.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillTreeTemplateRepository extends JpaRepository<SkillTreeTemplate, Long> {
    List<SkillTreeTemplate> findByActiveTrueOrderByNameAsc();

    Optional<SkillTreeTemplate> findFirstByActiveTrueOrderByNameAsc();

    Optional<SkillTreeTemplate> findFirstByTargetRoleIdAndActiveTrueOrderByIdAsc(Long targetRoleId);

    @Query("SELECT t FROM SkillTreeTemplate t LEFT JOIN FETCH t.careerRole")
    List<SkillTreeTemplate> findAllWithCareerRole();

    @Query("SELECT t FROM SkillTreeTemplate t LEFT JOIN FETCH t.careerRole WHERE t.id = :id")
    Optional<SkillTreeTemplate> findByIdWithCareerRole(@Param("id") Long id);

    @Query("SELECT t FROM SkillTreeTemplate t WHERE t.targetRoleId = :targetRoleId")
    Optional<SkillTreeTemplate> findByCareerRoleId(@Param("targetRoleId") Long targetRoleId);
}
