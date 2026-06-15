package vn.uth.careercompass.kernel.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;

import java.util.List;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    List<UserSkill> findByUser(User user);
    @Transactional
    void deleteByUser(User user);
}
