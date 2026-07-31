package vn.uth.careercompass.roadmap.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.SkillNodeRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.ActivityLogService;
import vn.uth.careercompass.roadmap.RoadmapActivityType;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link ProgressService}.
 *
 * <p>Điểm ĐÁNG CHÚ Ý: ProgressService phụ thuộc RoadmapService (để hỏi node có bị khóa không).
 * Ở đây RoadmapService bị MOCK — ta tự quyết định "khóa/không khóa" thay vì chạy logic thật
 * của nó (logic đó đã được test riêng trong RoadmapServiceTest). Đây là cách cô lập unit.
 *
 * <p>{@code save()} được stub trả lại chính đối tượng truyền vào (thenAnswer) để ta soi được
 * các field service vừa set (status, completedAt).
 */
@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private SkillNodeRepository skillNodeRepository;
    @Mock
    private UserNodeProgressRepository userNodeProgressRepository;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private RoadmapService roadmapService;

    @InjectMocks
    private ProgressService progressService;

    private SkillNode sampleNode() {
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).build();
        Skill skill = Skill.builder().id(1L).name("Java").build();
        return SkillNode.builder().id(101L).template(template).skill(skill).title("Java").tier(1).build();
    }

    // ============================================================
    // Guard đầu vào
    // ============================================================

    @Test
    void updateProgress_whenSkillNodeIdNull_throwsBadRequest() {
        // Thiếu skillNodeId -> 400, dừng ngay, không tra DB
        assertThatThrownBy(() -> progressService.updateProgress(new User(), null, ProgressStatus.DONE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(skillNodeRepository, never()).findById(any());
        verify(userNodeProgressRepository, never()).save(any());
    }

    @Test
    void updateProgress_whenStatusNull_throwsBadRequest() {
        // Thiếu status -> 400
        assertThatThrownBy(() -> progressService.updateProgress(new User(), 101L, null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userNodeProgressRepository, never()).save(any());
    }

    @Test
    void updateProgress_whenNodeNotFound_throwsNotFound() {
        // Node không tồn tại -> 404
        when(skillNodeRepository.findById(101L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> progressService.updateProgress(new User(), 101L, ProgressStatus.DONE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(userNodeProgressRepository, never()).save(any());
    }

    // ============================================================
    // Gating: node bị khóa
    // ============================================================

    @Test
    void updateProgress_whenNodeLockedAndStatusNotStarted_isForbidden() {
        // Given: node đang bị khóa + user muốn đánh dấu DONE -> 403
        SkillNode node = sampleNode();
        User user = new User();
        when(skillNodeRepository.findById(101L)).thenReturn(Optional.of(node));
        when(roadmapService.isNodeLocked(user, node)).thenReturn(true);

        // When + Then
        assertThatThrownBy(() -> progressService.updateProgress(user, 101L, ProgressStatus.DONE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        // Không lưu tiến độ, không ghi log
        verify(userNodeProgressRepository, never()).save(any());
        verify(activityLogService, never()).log(any(), any(), any());
    }

    @Test
    void updateProgress_whenStatusNotStarted_skipsLockCheck() {
        // Đặt lại về NOT_STARTED (bỏ đánh dấu) -> KHÔNG kiểm khóa (điều kiện !NOT_STARTED.equals(status))
        SkillNode node = sampleNode();
        User user = new User();
        UserNodeProgress existing = UserNodeProgress.builder().user(user).skillNode(node)
                .status(ProgressStatus.DONE).build();
        when(skillNodeRepository.findById(101L)).thenReturn(Optional.of(node));
        when(userNodeProgressRepository.findByUserAndSkillNode(user, node)).thenReturn(Optional.of(existing));
        when(userNodeProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        UserNodeProgress result = progressService.updateProgress(user, 101L, ProgressStatus.NOT_STARTED);

        // Then: status về NOT_STARTED, completedAt bị xoá (null), KHÔNG hỏi RoadmapService
        assertThat(result.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(result.getCompletedAt()).isNull();
        verify(roadmapService, never()).isNodeLocked(any(), any());
    }

    // ============================================================
    // Happy path
    // ============================================================

    @Test
    void updateProgress_whenInProgressOnExisting_updatesWithoutCompletedAtOrLog() {
        // Given: đã có bản ghi, chuyển sang IN_PROGRESS
        SkillNode node = sampleNode();
        User user = new User();
        UserNodeProgress existing = UserNodeProgress.builder().user(user).skillNode(node)
                .status(ProgressStatus.NOT_STARTED).build();
        when(skillNodeRepository.findById(101L)).thenReturn(Optional.of(node));
        when(roadmapService.isNodeLocked(user, node)).thenReturn(false);
        when(userNodeProgressRepository.findByUserAndSkillNode(user, node)).thenReturn(Optional.of(existing));
        when(userNodeProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        UserNodeProgress result = progressService.updateProgress(user, 101L, ProgressStatus.IN_PROGRESS);

        // Then: status cập nhật, completedAt null (chưa DONE), KHÔNG ghi ActivityLog
        assertThat(result.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(result.getCompletedAt()).isNull();
        verify(activityLogService, never()).log(any(), any(), any());
    }

    @Test
    void updateProgress_whenDoneOnNewNode_createsProgressSetsCompletedAtAndLogs() {
        // Given: chưa có bản ghi -> service tự build mới; đánh dấu DONE
        SkillNode node = sampleNode();
        User user = new User();
        when(skillNodeRepository.findById(101L)).thenReturn(Optional.of(node));
        when(roadmapService.isNodeLocked(user, node)).thenReturn(false);
        // orElseGet -> không tìm thấy -> tạo bản ghi mới
        when(userNodeProgressRepository.findByUserAndSkillNode(user, node)).thenReturn(Optional.empty());
        when(userNodeProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        UserNodeProgress result = progressService.updateProgress(user, 101L, ProgressStatus.DONE);

        // Then: DONE + completedAt được set (thời điểm hoàn thành)
        assertThat(result.getStatus()).isEqualTo(ProgressStatus.DONE);
        assertThat(result.getCompletedAt()).isNotNull();
        assertThat(result.getUser()).isEqualTo(user);
        assertThat(result.getSkillNode()).isEqualTo(node);

        // Và: ghi ActivityLog loại NODE_DONE cho Dashboard (P5) đọc
        ArgumentCaptor<String> descCaptor = ArgumentCaptor.forClass(String.class);
        verify(activityLogService).log(eqUser(user), eqType(), descCaptor.capture());
        assertThat(descCaptor.getValue()).isEqualTo("Hoàn thành kỹ năng: Java");
    }

    // Helper nhỏ cho dễ đọc verify (tránh import tĩnh dài dòng)
    private User eqUser(User user) {
        return org.mockito.ArgumentMatchers.eq(user);
    }

    private String eqType() {
        return org.mockito.ArgumentMatchers.eq(RoadmapActivityType.NODE_DONE);
    }
}
