package vn.uth.careercompass.admin.repository;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.Skill;
 
import java.util.List;
import java.util.Optional;
 
@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByName(String name);
    Optional<Skill> findByNameIgnoreCase(String name);
 
    @Query("SELECT DISTINCT s.category FROM Skill s WHERE s.category IS NOT NULL")
    List<String> findDistinctCategories();
}
