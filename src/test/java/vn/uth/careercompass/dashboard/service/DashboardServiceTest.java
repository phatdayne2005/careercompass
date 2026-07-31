package vn.uth.careercompass.dashboard.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import vn.uth.careercompass.dashboard.dto.DashboardViewDTO;
import vn.uth.careercompass.kernel.entity.ActivityLog;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.repository.ActivityLogRepository;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.service.RoadmapService;
import vn.uth.careercompass.roadmap.service.SkillGapService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link DashboardService}.
 *
 * <p>Dashboard là màn hình tổng hợp: nó KHÔNG tự tính toán mà tái dùng
 * {@link RoadmapService} (tiến độ), {@link SkillGapService} (khoảng cách kỹ năng) và
 * {@link ActivityLogRepository} (hoạt động gần nhất), rồi gói thành 1 DTO cho view.
 * Vì vậy ta mock cả 3 và kiểm: (1) truyền đúng tham số giữa các service, (2) map đúng số liệu.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private RoadmapService roadmapService;

    @Mock
    private SkillGapService skillGapService;

    @Mock
    private ActivityLogRepository activityLogRepository;

    @InjectMocks
    private DashboardService dashboardService;

    // ============================================================
    // Happy path: gộp tiến độ roadmap + skill gap + 5 hoạt động gần nhất
    // ============================================================
    @Test
    void getDashboard_aggregatesRoadmapSkillGapAndRecentActivities() {
        // Given: user bất kỳ (chỉ dùng làm khoá truy vấn, không đọc field bên trong).
        User user = User.builder().email("u@uth.edu.vn").build();

        // RoadmapService trả template id=5 + số liệu tiến độ.
        RoadmapTemplateDTO template = RoadmapTemplateDTO.builder().id(5L).name("Backend").build();
        RoadmapViewDTO roadmap = RoadmapViewDTO.builder()
                .template(template)
                .completionPercent(42.5)
                .completedNodes(3)
                .totalNodes(10)
                .build();
        // Dashboard gọi getRoadmap(user, null) -> null nghĩa "template mặc định".
        when(roadmapService.getRoadmap(eq(user), isNull())).thenReturn(roadmap);

        // SkillGapService phân tích ĐÚNG template đang xem (id=5).
        SkillGapResultDTO skillGap = SkillGapResultDTO.builder()
                .matchedSkillCount(4)
                .missingSkillCount(6)
                .build();
        when(skillGapService.analyze(user, 5L)).thenReturn(skillGap);

        // Repo trả 2 hoạt động gần nhất (đóng gói trong Page).
        ActivityLog log1 = ActivityLog.builder()
                .type("LOGIN").description("Đăng nhập").createdAt(LocalDateTime.now()).build();
        ActivityLog log2 = ActivityLog.builder()
                .type("REGISTER").description("Đăng ký").createdAt(LocalDateTime.now().minusHours(1)).build();
        when(activityLogRepository.findByUserOrderByCreatedAtDesc(eq(user), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(log1, log2)));

        // When
        DashboardViewDTO view = dashboardService.getDashboard(user);

        // Then: số liệu tiến độ map thẳng từ RoadmapViewDTO.
        assertThat(view.getRoadmapCompletionPercent()).isEqualTo(42.5);
        assertThat(view.getCompletedNodes()).isEqualTo(3);
        assertThat(view.getTotalNodes()).isEqualTo(10);
        // Số liệu skill gap map thẳng từ SkillGapResultDTO.
        assertThat(view.getMatchedSkillCount()).isEqualTo(4);
        assertThat(view.getMissingSkillCount()).isEqualTo(6);
        // Hoạt động: map ActivityLog -> ActivityLogDTO, giữ đúng thứ tự.
        assertThat(view.getRecentActivities()).hasSize(2);
        assertThat(view.getRecentActivities().get(0).getType()).isEqualTo("LOGIN");
        assertThat(view.getRecentActivities().get(0).getDescription()).isEqualTo("Đăng nhập");
        assertThat(view.getRecentActivities().get(1).getType()).isEqualTo("REGISTER");

        // Xác nhận skillGap được phân tích với templateId LẤY TỪ roadmap (id=5), không phải hằng số.
        verify(skillGapService).analyze(user, 5L);

        // Xác nhận query hoạt động dùng đúng phân trang: trang 0, kích thước 5.
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(activityLogRepository).findByUserOrderByCreatedAtDesc(eq(user), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    // ============================================================
    // Không có hoạt động nào -> recentActivities rỗng
    // ============================================================
    @Test
    void getDashboard_whenNoActivities_returnsEmptyActivityList() {
        // Given
        User user = User.builder().email("u@uth.edu.vn").build();
        RoadmapTemplateDTO template = RoadmapTemplateDTO.builder().id(1L).build();
        RoadmapViewDTO roadmap = RoadmapViewDTO.builder()
                .template(template)
                .completionPercent(0.0)
                .completedNodes(0)
                .totalNodes(0)
                .build();
        when(roadmapService.getRoadmap(eq(user), isNull())).thenReturn(roadmap);
        when(skillGapService.analyze(user, 1L))
                .thenReturn(SkillGapResultDTO.builder().matchedSkillCount(0).missingSkillCount(0).build());
        // Page rỗng.
        when(activityLogRepository.findByUserOrderByCreatedAtDesc(eq(user), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // When
        DashboardViewDTO view = dashboardService.getDashboard(user);

        // Then
        assertThat(view.getRecentActivities()).isEmpty();
        assertThat(view.getRoadmapCompletionPercent()).isZero();
        assertThat(view.getMatchedSkillCount()).isZero();
    }
}
