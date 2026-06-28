package vn.uth.careercompass.admin.repository;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.admin.entity.CareerRole;
 
import java.util.List;
import java.util.Optional;
 
@Repository
public interface CareerRoleRepository extends JpaRepository<CareerRole, Long> {
    Optional<CareerRole> findByName(String name);
 
    @Query("SELECT cr FROM CareerRole cr WHERE cr.id NOT IN (SELECT t.careerRole.id FROM SkillTreeTemplate t)")
    List<CareerRole> findWithoutTemplate();
}
