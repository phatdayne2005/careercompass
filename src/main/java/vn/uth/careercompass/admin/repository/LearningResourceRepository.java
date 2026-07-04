package vn.uth.careercompass.admin.repository;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.LearningResource;
 
import java.util.List;
 
@Repository
public interface LearningResourceRepository extends JpaRepository<LearningResource, Long> {
    @Query("SELECT DISTINCT r.resourceType FROM LearningResource r WHERE r.resourceType IS NOT NULL")
    List<String> findDistinctResourceTypes();

    /** Tất cả tài liệu học của mọi node trong 1 roadmap (P4 đọc để render panel tài nguyên). */
    List<LearningResource> findBySkillNode_Template_IdOrderByIdAsc(Long templateId);
}
