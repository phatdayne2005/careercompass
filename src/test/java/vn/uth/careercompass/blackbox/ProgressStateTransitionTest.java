package vn.uth.careercompass.blackbox;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;
import vn.uth.careercompass.roadmap.service.ProgressService;
import vn.uth.careercompass.roadmap.service.RoadmapService;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KỸ THUẬT: Chuyển đổi trạng thái (State Transition Testing) — chương IV mục 3.
 *
 * <p>Đối tượng: máy trạng thái tiến độ học của một node kỹ năng.
 * Ba trạng thái {@link ProgressStatus}: NOT_STARTED, IN_PROGRESS, DONE.
 * Sự kiện: người dùng bấm đánh dấu trạng thái mới.
 *
 * <p>Khác bảng quyết định ở chỗ: bảng quyết định xét tổ hợp điều kiện tại MỘT thời điểm,
 * còn ở đây ta xét CHUỖI thời gian — trạng thái đang có ảnh hưởng tới kết quả và tác dụng phụ.
 *
 * <p>BẢNG CHUYỂN TRẠNG THÁI:
 * <pre>
 *   Mã     Trạng thái đầu   Sự kiện                        Trạng thái cuối       Hợp lệ
 *   ST-01  NOT_STARTED      Đánh dấu IN_PROGRESS (mở)      IN_PROGRESS           Có
 *   ST-02  IN_PROGRESS      Đánh dấu DONE (mở)             DONE + completedAt    Có
 *   ST-03  DONE             Bỏ đánh dấu về NOT_STARTED     NOT_STARTED, xoá giờ  Có
 *   ST-04  DONE             Hạ về IN_PROGRESS              IN_PROGRESS, xoá giờ  Có
 *   ST-05  NOT_STARTED      Đánh dấu DONE (node mở)        DONE + completedAt    Có
 *   ST-06  IN_PROGRESS      Bỏ đánh dấu về NOT_STARTED     NOT_STARTED           Có
 *   ST-07  NOT_STARTED      Đánh dấu DONE khi node KHOÁ    Bị chặn 403           KHÔNG
 *   ST-08  IN_PROGRESS      Đánh dấu DONE khi node KHOÁ    Bị chặn 403           KHÔNG
 *   ST-09  DONE             Hạ về IN_PROGRESS khi node KHOÁ Bị chặn 403           KHÔNG
 * </pre>
 *
 * <p>Sáu dòng đầu phủ ĐỦ 6 cạnh chuyển giữa 3 trạng thái (3 × 2 = 6 cặp có thứ tự).
 * Hai dòng cuối là chuyển đổi bị chặn — phần mà kiểm thử đi đường thuận sẽ bỏ sót.
 *
 * <p>Hai dòng cuối là <b>chuyển đổi không hợp lệ</b> — phần mà kiểm thử chỉ đi đường thuận
 * sẽ bỏ sót. Chúng kiểm chứng chốt chặn có thật sự hoạt động.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Chuyển đổi trạng thái — tiến độ học node kỹ năng")
class ProgressStateTransitionTest {

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

    private static final long NODE_ID = 101L;

    private SkillNode sampleNode() {
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).build();
        Skill skill = Skill.builder().id(1L).name("Java").build();
        return SkillNode.builder().id(NODE_ID).template(template).skill(skill)
                .title("Java").tier(1).build();
    }

    /**
     * Dựng sẵn bản ghi tiến độ ĐANG Ở trạng thái {@code from} — đây chính là
     * "trạng thái đầu" của bảng chuyển.
     */
    private void givenCurrentState(SkillNode node, User user, ProgressStatus from) {
        UserNodeProgress existing = UserNodeProgress.builder()
                .user(user)
                .skillNode(node)
                .status(from)
                .completedAt(from == ProgressStatus.DONE ? LocalDateTime.now() : null)
                .build();
        when(skillNodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node));
        when(userNodeProgressRepository.findByUserAndSkillNode(user, node))
                .thenReturn(Optional.of(existing));
        when(userNodeProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ================================================================
    // CHUYỂN ĐỔI HỢP LỆ
    // ================================================================

    @Test
    @DisplayName("ST-01 · NOT_STARTED → IN_PROGRESS (node mở khoá)")
    void st01_notStarted_toInProgress() {
        SkillNode node = sampleNode();
        User user = new User();
        givenCurrentState(node, user, ProgressStatus.NOT_STARTED);
        when(roadmapService.isNodeLocked(user, node)).thenReturn(false);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(result.getCompletedAt()).isNull();
        verify(activityLogService, never()).log(any(), any(), any());
    }

    @Test
    @DisplayName("ST-02 · IN_PROGRESS → DONE, phải ghi completedAt và nhật ký")
    void st02_inProgress_toDone() {
        SkillNode node = sampleNode();
        User user = new User();
        givenCurrentState(node, user, ProgressStatus.IN_PROGRESS);
        when(roadmapService.isNodeLocked(user, node)).thenReturn(false);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.DONE);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.DONE);
        // Tác dụng phụ 1: đóng dấu thời điểm hoàn thành
        assertThat(result.getCompletedAt()).isNotNull();
        // Tác dụng phụ 2: ghi nhật ký để Dashboard đọc "hoạt động gần đây"
        verify(activityLogService).log(any(), any(), any());
    }

    @Test
    @DisplayName("ST-03 · DONE → NOT_STARTED, phải XOÁ completedAt")
    void st03_done_toNotStarted() {
        SkillNode node = sampleNode();
        User user = new User();
        givenCurrentState(node, user, ProgressStatus.DONE);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.NOT_STARTED);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        // Quay lại chưa học thì mốc hoàn thành phải bị xoá, nếu không Dashboard sẽ tính sai.
        assertThat(result.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("ST-04 · DONE → IN_PROGRESS, phải XOÁ completedAt")
    void st04_done_toInProgress() {
        SkillNode node = sampleNode();
        User user = new User();
        givenCurrentState(node, user, ProgressStatus.DONE);
        when(roadmapService.isNodeLocked(user, node)).thenReturn(false);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(result.getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("ST-05 · NOT_STARTED → DONE (node mở khoá), nhảy thẳng không qua IN_PROGRESS")
    void st05_notStarted_toDone() {
        SkillNode node = sampleNode();
        User user = new User();
        givenCurrentState(node, user, ProgressStatus.NOT_STARTED);
        when(roadmapService.isNodeLocked(user, node)).thenReturn(false);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.DONE);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.DONE);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(activityLogService).log(any(), any(), any());
    }

    @Test
    @DisplayName("ST-06 · IN_PROGRESS → NOT_STARTED, bỏ đánh dấu giữa chừng")
    void st06_inProgress_toNotStarted() {
        SkillNode node = sampleNode();
        User user = new User();
        givenCurrentState(node, user, ProgressStatus.IN_PROGRESS);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.NOT_STARTED);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(result.getCompletedAt()).isNull();
        // Về NOT_STARTED thì luật chặn không áp dụng -> không hỏi khoá.
        verify(roadmapService, never()).isNodeLocked(any(), any());
    }

    // ================================================================
    // CHUYỂN ĐỔI KHÔNG HỢP LỆ — phải bị chặn
    // ================================================================

    @Test
    @DisplayName("ST-07 · NOT_STARTED → DONE khi node bị khoá: phải bị chặn")
    void st07_notStarted_toDone_nodeLocked_biChan() {
        SkillNode node = sampleNode();
        User user = new User();
        when(skillNodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node));
        when(roadmapService.isNodeLocked(user, node)).thenReturn(true);

        assertThatThrownBy(() ->
                progressService.updateProgress(user, NODE_ID, ProgressStatus.DONE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userNodeProgressRepository, never()).save(any());
    }

    @Test
    @DisplayName("ST-08 · IN_PROGRESS → DONE khi node bị khoá: phải bị chặn")
    void st08_inProgress_toDone_nodeLocked_biChan() {
        SkillNode node = sampleNode();
        User user = new User();
        when(skillNodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node));
        when(roadmapService.isNodeLocked(user, node)).thenReturn(true);

        assertThatThrownBy(() ->
                progressService.updateProgress(user, NODE_ID, ProgressStatus.DONE))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userNodeProgressRepository, never()).save(any());
        verify(activityLogService, never()).log(any(), any(), any());
    }

    @Test
    @DisplayName("ST-09 · DONE → IN_PROGRESS khi node bị khoá: phải bị chặn")
    void st09_done_toInProgress_nodeLocked_biChan() {
        SkillNode node = sampleNode();
        User user = new User();
        when(skillNodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node));
        when(roadmapService.isNodeLocked(user, node)).thenReturn(true);

        assertThatThrownBy(() ->
                progressService.updateProgress(user, NODE_ID, ProgressStatus.IN_PROGRESS))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        verify(userNodeProgressRepository, never()).save(any());
    }
}
