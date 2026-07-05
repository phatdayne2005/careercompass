package vn.uth.careercompass.kernel.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;
    // Thêm repository vào danh sách các dependecy được inject
    private final CareerRoleRepository careerRoleRepository; // thêm dòng này

    public void setTargetRole(User user, Long targetRoleId) {
        try {
            // Tìm thực thể CareerRole theo id, Không thấy thì ném lỗi
            CareerRole careerRole = careerRoleRepository.findById(targetRoleId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy vai trò nghề nghiệp có ID: " + targetRoleId));
            // gán thực thể CareerRole cho user
            user.setCareerRole(careerRole);
            // Lưu user xuống DB
            userRepository.save(user);
        } catch (Exception e) {
            // Xử lý lỗi
            System.err.println("Lỗi khi thiết lập định hướng nghề nghiệp: " + e.getMessage());
            throw new RuntimeException("Cập nhật định hướng nghề nghiệp thất bại", e);
        }
    }

    public void setGithub(User user, String githubUsername) {
        user.setGithubUsername(githubUsername);
        userRepository.save(user);
    }

    public void storeTranscript(User user, String transcriptPath, Double gpa) {
        user.setTranscriptPath(transcriptPath);
        user.setGpa(gpa);
        userRepository.save(user);
    }

    /** Lưu tóm tắt AI của bảng điểm/CV (FR1.3). */
    public void storeTranscriptSummary(User user, String summary) {
        user.setTranscriptSummary(summary);
        userRepository.save(user);
    }

    @Transactional
    public void replaceSkills(User user, List<Long> skillIds) {
        userSkillRepository.deleteByUser(user);
        List<Skill> skills = skillRepository.findAllById(skillIds);
        for (Skill skill : skills) {
            UserSkill userSkill = UserSkill.builder().user(user).skill(skill).build();
            userSkillRepository.save(userSkill);
        }
    }

    /** Đánh dấu user đã hoàn thành 3 bước Onboarding. */
    public void completeOnboarding(User user) {
        user.setOnboardingCompleted(true);
        userRepository.save(user);
    }
}

