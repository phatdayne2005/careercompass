package vn.uth.careercompass.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.SkillTreeTemplateRepository;
import vn.uth.careercompass.kernel.entity.RoleName;
import vn.uth.careercompass.kernel.repository.UserRepository;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link AdminDashboardService}.
 *
 * <p>MỤC TIÊU: kiểm tra service gom số liệu thống kê cho trang Dashboard admin.
 * Service này chỉ gọi các method {@code count()} / {@code countByRole_Name(...)} của repository
 * rồi nhét vào 1 {@link Map}. Ta mock hết repository nên KHÔNG cần DB thật.
 *
 * <p>2 nhánh cần phủ: (1) happy path — mọi count trả về giá trị, map đủ 6 khóa;
 * (2) khi 1 repository ném lỗi — service bọc {@code catch(Exception)} rồi ném lại RuntimeException.
 */
@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CareerRoleRepository careerRoleRepository;

    @Mock
    private SkillTreeTemplateRepository skillTreeTemplateRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    // ============================================================================
    // TEST 1 — HAPPY PATH: gom đủ 6 chỉ số vào map
    // ============================================================================
    @Test
    void getDashboardStats_returnsAllSixMetrics() {
        // ---------- ARRANGE (Given): dạy từng count trả về 1 con số riêng biệt ----------
        // WHY dùng số khác nhau cho mỗi loại: để chắc chắn service KHÔNG ghép nhầm
        // giá trị của count này vào khóa của count khác.
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByRole_Name(RoleName.STUDENT)).thenReturn(80L);
        when(userRepository.countByRole_Name(RoleName.COUNSELOR)).thenReturn(15L);
        when(userRepository.countByRole_Name(RoleName.ADMIN)).thenReturn(5L);
        when(careerRoleRepository.count()).thenReturn(12L);
        when(skillTreeTemplateRepository.count()).thenReturn(7L);

        // ---------- ACT (When) ----------
        Map<String, Object> stats = adminDashboardService.getDashboardStats();

        // ---------- ASSERT (Then): đủ 6 khóa + đúng giá trị ----------
        assertThat(stats).hasSize(6);
        assertThat(stats).containsEntry("totalUsers", 100L);
        assertThat(stats).containsEntry("totalStudents", 80L);
        assertThat(stats).containsEntry("totalCounselors", 15L);
        assertThat(stats).containsEntry("totalAdmins", 5L);
        assertThat(stats).containsEntry("totalCareerRoles", 12L);
        assertThat(stats).containsEntry("totalRoadmaps", 7L);
    }

    // ============================================================================
    // TEST 2 — repository ném lỗi: service bọc lại thành RuntimeException
    // ============================================================================
    @Test
    void getDashboardStats_whenRepositoryFails_throwsRuntimeException() {
        // ---------- ARRANGE ----------
        // Dạy mock: count() đầu tiên đã ném lỗi (vd DB rớt) -> nhảy vào catch(Exception).
        // lenient(): các stub count khác KHÔNG được chạy tới (luồng dừng ngay dòng đầu),
        // nhưng ta khai báo phòng hờ; lenient tránh UnnecessaryStubbingException.
        when(userRepository.count()).thenThrow(new RuntimeException("DB connection lost"));

        // ---------- ACT + ASSERT ----------
        // Service log ra System.err rồi ném RuntimeException với message thân thiện,
        // đồng thời GIỮ nguyên nhân gốc (cause) để debug.
        assertThatThrownBy(() -> adminDashboardService.getDashboardStats())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Lấy dữ liệu thống kê Dashboard thất bại!")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    // ============================================================================
    // TEST 3 — map rỗng đầu vào vẫn build được (mọi count = 0)
    // ============================================================================
    @Test
    void getDashboardStats_whenAllZero_returnsZeroedMap() {
        // Given: hệ thống mới toanh, chưa có dữ liệu -> mọi count = 0.
        // lenient vì mọi giá trị đều là default 0L của mock; ta stub tường minh cho rõ ý.
        lenient().when(userRepository.count()).thenReturn(0L);
        lenient().when(userRepository.countByRole_Name(RoleName.STUDENT)).thenReturn(0L);
        lenient().when(userRepository.countByRole_Name(RoleName.COUNSELOR)).thenReturn(0L);
        lenient().when(userRepository.countByRole_Name(RoleName.ADMIN)).thenReturn(0L);
        lenient().when(careerRoleRepository.count()).thenReturn(0L);
        lenient().when(skillTreeTemplateRepository.count()).thenReturn(0L);

        Map<String, Object> stats = adminDashboardService.getDashboardStats();

        assertThat(stats).hasSize(6);
        assertThat(stats.values()).allMatch(v -> v.equals(0L));
    }
}
