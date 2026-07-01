package vn.uth.careercompass.admin.repository;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.SkillNode;

import java.util.List;
 
@Repository
public interface SkillNodeRepository extends JpaRepository<SkillNode, Long> {
    List<SkillNode> findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(Long templateId);
}