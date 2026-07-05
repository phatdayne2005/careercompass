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

    /** Xoá mọi tài liệu học của các node thuộc 1 template (dùng khi xoá lộ trình). */
    void deleteBySkillNode_Template_Id(Long templateId);

    /** Tài liệu học của 1 node (counselor editor hiển thị + quản lý). */
    List<LearningResource> findBySkillNode_IdOrderByIdAsc(Long nodeId);
}
