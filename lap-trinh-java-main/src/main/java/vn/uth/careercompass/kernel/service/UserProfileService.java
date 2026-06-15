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

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProfileService {
    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;

    public void setTargetRole(User user, Long targetRoleId){
        user.setTargetRoleId(targetRoleId);
        userRepository.save(user);
    }

    public void setGithub(User user, String githubUsername){
        user.setGithubUsername(githubUsername);
        userRepository.save(user);
    }

    public void storeTranscript(User user, String transcriptPath, Double gpa){
        user.setTranscriptPath(transcriptPath);
        user.setGpa(gpa);
        userRepository.save(user);
    }

    @Transactional
    public void replaceSkills(User user, List<Long> skillIds){
        userSkillRepository.deleteByUser(user);
        List<Skill> skills = skillRepository.findAllById(skillIds);
        for (Skill skill : skills) {
            UserSkill userSkill = UserSkill.builder().user(user).skill(skill).build();
            userSkillRepository.save(userSkill);
        }
    }
}
