package vn.uth.careercompass.profile.service;

import org.springframework.stereotype.Service;

import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.UserProfileService;

import java.util.List;

@Service
public class ProfileService {

    private final UserProfileService userProfileService;

    public ProfileService(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    public User getProfile(User user) {
        return user;
    }

    public void updateGithub(User user, String githubUsername) {
        userProfileService.setGithub(user, githubUsername);
    }

    public void updateSkills(User user, List<Long> skillIds) {
        userProfileService.replaceSkills(user, skillIds);
    }
}