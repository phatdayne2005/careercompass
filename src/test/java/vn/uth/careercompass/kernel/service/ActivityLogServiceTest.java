package vn.uth.careercompass.kernel.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.uth.careercompass.kernel.entity.ActivityLog;
import vn.uth.careercompass.kernel.entity.KernelActivityLogType;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.ActivityLogRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Unit test cho {@link ActivityLogService} — chỉ 1 method build ActivityLog rồi save.
 * Dù đơn giản, vẫn nên có 1 test để chốt "log được ghi đúng user/type/description".
 */
@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private ActivityLogService activityLogService;

    @Test
    void log_buildsAndSavesActivityLogWithGivenFields() {
        User user = User.builder().email("a@uth.edu.vn").build();

        activityLogService.log(user, KernelActivityLogType.LOGIN, "Đăng nhập");

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        ActivityLog saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getType()).isEqualTo(KernelActivityLogType.LOGIN);
        assertThat(saved.getDescription()).isEqualTo("Đăng nhập");
    }
}
