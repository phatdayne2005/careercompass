package vn.uth.careercompass.admin.repository;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.SkillNode;
 
import java.util.List;
import java.util.Optional;
 
@Repository
public interface SkillNodeRepository extends JpaRepository<SkillNode, Long> {
    @Query("SELECT n FROM SkillNode n LEFT JOIN FETCH n.skill LEFT JOIN FETCH n.parentNode p LEFT JOIN FETCH p.skill WHERE n.skillTreeTemplate.id = :templateId")
    List<SkillNode> findAllByTemplateIdWithRelations(@Param("templateId") Long templateId);
 
    @Query("SELECT n FROM SkillNode n LEFT JOIN FETCH n.skill LEFT JOIN FETCH n.learningResources WHERE n.id = :id")
    Optional<SkillNode> findByIdWithRelations(@Param("id") Long id);
}
