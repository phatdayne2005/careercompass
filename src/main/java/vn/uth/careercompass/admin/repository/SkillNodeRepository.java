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
    List<SkillNode> findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(Long templateId);

    @Query("SELECT sn FROM SkillNode sn LEFT JOIN FETCH sn.template LEFT JOIN FETCH sn.skill LEFT JOIN FETCH sn.parent WHERE sn.id = :id")
    Optional<SkillNode> findByIdWithRelations(@Param("id") Long id);

    @Query("SELECT sn FROM SkillNode sn LEFT JOIN FETCH sn.template LEFT JOIN FETCH sn.skill LEFT JOIN FETCH sn.parent WHERE sn.template.id = :templateId ORDER BY sn.tier Asc, sn.orderIndex Asc, sn.id Asc")
    List<SkillNode> findAllByTemplateIdWithRelations(@Param("templateId") Long templateId);
}