package vn.uth.careercompass.mentor;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.uth.careercompass.kernel.entity.User; // Đọc entity User từ P1 Kernel

import java.util.List;

@Repository
public interface MentorSessionRepository extends JpaRepository<MentorSession, Long> {
    
    // Lấy danh sách các cuộc trò chuyện của một User, sắp xếp mới nhất lên đầu
    List<MentorSession> findByUserOrderByCreatedAtDesc(User user);
}