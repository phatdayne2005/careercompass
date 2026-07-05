package vn.uth.careercompass.roadmap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNodeProgressRepository extends JpaRepository<UserNodeProgress, Long> {
    Optional<UserNodeProgress> findByUserAndSkillNode(User user, SkillNode skillNode);

    List<UserNodeProgress> findByUserAndSkillNode_Template_Id(User user, Long templateId);

    /** Tiến độ theo trạng thái (JOIN FETCH skill để dùng ngoài transaction). */
    @Query("SELECT p FROM UserNodeProgress p JOIN FETCH p.skillNode sn JOIN FETCH sn.skill "
            + "WHERE p.user = :user AND p.status = :status")
    List<UserNodeProgress> findByUserAndStatusWithSkill(@Param("user") User user,
                                                        @Param("status") ProgressStatus status);

    /** Xoá mọi tiến độ của các node thuộc 1 template (dùng khi P7 xoá lộ trình). */
    void deleteBySkillNode_Template_Id(Long templateId);
}
