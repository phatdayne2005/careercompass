package vn.uth.careercompass.roadmap.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNodeProgressRepository extends JpaRepository<UserNodeProgress, Long> {
    Optional<UserNodeProgress> findByUserAndSkillNode(User user, SkillNode skillNode);

    List<UserNodeProgress> findByUserAndSkillNode_Template_Id(User user, Long templateId);
}
