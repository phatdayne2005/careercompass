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
    Optional<SkillTreeTemplate> findByName(String name);
 
    @Query("SELECT t FROM SkillTreeTemplate t JOIN FETCH t.careerRole")
    List<SkillTreeTemplate> findAllWithCareerRole();
 
    @Query("SELECT t FROM SkillTreeTemplate t JOIN FETCH t.careerRole WHERE t.id = :id")
    Optional<SkillTreeTemplate> findByIdWithCareerRole(@Param("id") Long id);
 
    Optional<SkillTreeTemplate> findByCareerRoleId(Long careerRoleId);
}
