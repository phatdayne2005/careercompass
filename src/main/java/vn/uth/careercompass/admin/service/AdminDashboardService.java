package vn.uth.careercompass.admin.service;
 
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.SkillTreeTemplateRepository;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.repository.UserRepository;
 
import java.util.HashMap;
import java.util.Map;
 
@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final UserRepository userRepository;
    private final CareerRoleRepository careerRoleRepository;
    private final SkillTreeTemplateRepository skillTreeTemplateRepository;
 
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            long totalUsers = userRepository.count();
            long totalStudents = userRepository.countByRole_Name(RoleName.STUDENT);
            long totalCounselors = userRepository.countByRole_Name(RoleName.COUNSELOR);
            long totalAdmins = userRepository.countByRole_Name(RoleName.ADMIN);
            long totalCareerRoles = careerRoleRepository.count();
            long totalRoadmaps = skillTreeTemplateRepository.count();
 
            stats.put("totalUsers", totalUsers);
            stats.put("totalStudents", totalStudents);
            stats.put("totalCounselors", totalCounselors);
            stats.put("totalAdmins", totalAdmins);
            stats.put("totalCareerRoles", totalCareerRoles);
            stats.put("totalRoadmaps", totalRoadmaps);
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy dữ liệu thống kê Dashboard: " + e.getMessage());
            throw new RuntimeException("Lấy dữ liệu thống kê Dashboard thất bại!", e);
        }
        return stats;
    }
}
