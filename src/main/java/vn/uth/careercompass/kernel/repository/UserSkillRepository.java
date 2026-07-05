package vn.uth.careercompass.kernel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;

import java.util.List;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUser(User user);

    /** JOIN FETCH skill để dùng ngoài transaction (open-in-view=false) — tránh LazyInitializationException. */
    @Query("SELECT us FROM UserSkill us JOIN FETCH us.skill WHERE us.user = :user")
    List<UserSkill> findByUserWithSkill(@Param("user") User user);
    boolean existsByUserAndSkill(User user, Skill skill);
    @Transactional
    void deleteByUser(User user);
}
