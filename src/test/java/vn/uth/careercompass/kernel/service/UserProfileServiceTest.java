package vn.uth.careercompass.kernel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;
import vn.uth.careercompass.kernel.repository.UserRepository;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link UserProfileService} — các method P2/P3/P6 gọi để ghi dữ liệu vào User.
 * Phần lớn là "set field rồi save": test kiểu này ngắn, chỉ cần khẳng định field đúng + có save.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private CareerRoleRepository careerRoleRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    // ---------- setTargetRole ----------

    @Test
    void setTargetRole_whenRoleExists_setsAndSaves() {
        User user = User.builder().build();
        CareerRole careerRole = mock(CareerRole.class); // không cần nội dung, chỉ cần 1 tham chiếu
        when(careerRoleRepository.findById(5L)).thenReturn(Optional.of(careerRole));

        userProfileService.setTargetRole(user, 5L);

        assertThat(user.getCareerRole()).isEqualTo(careerRole);
        verify(userRepository).save(user);
    }

    @Test
    void setTargetRole_whenRoleNotFound_throwsWrappedRuntime() {
        User user = User.builder().build();
        when(careerRoleRepository.findById(99L)).thenReturn(Optional.empty());

        // Service bắt IllegalArgumentException rồi bọc lại thành RuntimeException.
        // Ta kiểm cả message ngoài lẫn nguyên nhân gốc (cause) bên trong.
        assertThatThrownBy(() -> userProfileService.setTargetRole(user, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cập nhật định hướng nghề nghiệp thất bại")
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }

    // ---------- setGithub ----------

    @Test
    void setGithub_setsUsernameAndSaves() {
        User user = User.builder().build();

        userProfileService.setGithub(user, "octocat");

        assertThat(user.getGithubUsername()).isEqualTo("octocat");
        verify(userRepository).save(user);
    }

    // ---------- storeTranscript ----------

    @Test
    void storeTranscript_setsPathAndGpaAndSaves() {
        User user = User.builder().build();

        userProfileService.storeTranscript(user, "/uploads/bang-diem.pdf", 3.5);

        assertThat(user.getTranscriptPath()).isEqualTo("/uploads/bang-diem.pdf");
        assertThat(user.getGpa()).isEqualTo(3.5);
        verify(userRepository).save(user);
    }

    // ---------- storeTranscriptSummary ----------

    @Test
    void storeTranscriptSummary_setsSummaryAndSaves() {
        User user = User.builder().build();

        userProfileService.storeTranscriptSummary(user, "Sinh viên giỏi Java");

        assertThat(user.getTranscriptSummary()).isEqualTo("Sinh viên giỏi Java");
        verify(userRepository).save(user);
    }

    // ---------- completeOnboarding ----------

    @Test
    void completeOnboarding_setsFlagTrueAndSaves() {
        User user = User.builder().build();

        userProfileService.completeOnboarding(user);

        assertThat(user.getOnboardingCompleted()).isTrue();
        verify(userRepository).save(user);
    }

    // ---------- replaceSkills ----------

    @Test
    void replaceSkills_deletesOldThenSavesEachNewSkill() {
        User user = User.builder().build();
        Skill java = Skill.builder().id(1L).name("Java").build();
        Skill spring = Skill.builder().id(2L).name("Spring").build();
        List<Long> skillIds = List.of(1L, 2L);
        when(skillRepository.findAllById(skillIds)).thenReturn(List.of(java, spring));

        userProfileService.replaceSkills(user, skillIds);

        // 1) Phải XOÁ skill cũ TRƯỚC (tránh trùng lặp / dữ liệu cũ tồn đọng)
        verify(userSkillRepository).deleteByUser(user);
        // 2) Rồi lưu đúng 2 UserSkill mới, gắn đúng user + đúng skill
        ArgumentCaptor<UserSkill> captor = ArgumentCaptor.forClass(UserSkill.class);
        verify(userSkillRepository, times(2)).save(captor.capture());
        List<UserSkill> savedList = captor.getAllValues();
        assertThat(savedList).extracting(UserSkill::getSkill).containsExactly(java, spring);
        assertThat(savedList).allSatisfy(us -> assertThat(us.getUser()).isEqualTo(user));
    }
}
