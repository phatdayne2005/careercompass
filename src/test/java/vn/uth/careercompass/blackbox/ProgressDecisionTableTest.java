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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * KỸ THUẬT: Bảng quyết định (Decision Table Testing) — chương IV mục 3.
 *
 * <p>Đối tượng: luật chặn trong {@code ProgressService.updateProgress()}:
 * <pre>
 *   if (!NOT_STARTED.equals(status) &amp;&amp; isNodeLocked(user, node)) -&gt; 403 FORBIDDEN
 * </pre>
 *
 * <p>Hai điều kiện đầu vào:
 * <ul>
 *   <li>C1 — Trạng thái muốn đặt: NOT_STARTED | IN_PROGRESS | DONE  (3 giá trị)</li>
 *   <li>C2 — Node có đang bị khoá: Y | N                            (2 giá trị)</li>
 * </ul>
 *
 * <p>Số rule tối đa = 3 × 2 = <b>6</b>. Mỗi rule dưới đây là một test, đặt tên theo mã rule
 * để đối chiếu trực tiếp với bảng quyết định trong báo cáo.
 *
 * <p>BẢNG QUYẾT ĐỊNH (bản đầy đủ):
 * <pre>
 *   Điều kiện / Hành động        R1      R2      R3      R4      R5      R6
 *   C1 Trạng thái muốn đặt       NOT_S   NOT_S   IN_PRG  IN_PRG  DONE    DONE
 *   C2 Node bị khoá              Y       N       Y       N       Y       N
 *   A1 Cho phép lưu              X       X       -       X       -       X
 *   A2 Từ chối (403)             -       -       X       -       X       -
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bảng quyết định — luật chặn cập nhật tiến độ học")
class ProgressDecisionTableTest {

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

    /** Chuẩn bị chung: tìm thấy node, và cho phép lưu thành công. */
    private void givenNodeExistsAndSaveEchoes(SkillNode node, User user) {
        when(skillNodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node));
        when(userNodeProgressRepository.findByUserAndSkillNode(user, node))
                .thenReturn(Optional.empty());
        when(userNodeProgressRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ================================================================
    // NHÓM CHO PHÉP — A1
    // ================================================================

    @Test
    @DisplayName("R1 · NOT_STARTED + node bị khoá → cho phép (bỏ đánh dấu luôn được)")
    void rule1_notStarted_nodeLocked_choPhep() {
        SkillNode node = sampleNode();
        User user = new User();
        givenNodeExistsAndSaveEchoes(node, user);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.NOT_STARTED);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        // Luật chỉ chặn khi trạng thái KHÁC NOT_STARTED -> không thèm hỏi khoá hay không.
        verify(roadmapService, never()).isNodeLocked(any(), any());
    }

    @Test
    @DisplayName("R2 · NOT_STARTED + node mở khoá → cho phép")
    void rule2_notStarted_nodeUnlocked_choPhep() {
        SkillNode node = sampleNode();
        User user = new User();
        givenNodeExistsAndSaveEchoes(node, user);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.NOT_STARTED);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(result.getCompletedAt()).isNull();
        verify(roadmapService, never()).isNodeLocked(any(), any());
    }

    @Test
    @DisplayName("R4 · IN_PROGRESS + node mở khoá → cho phép, không ghi completedAt")
    void rule4_inProgress_nodeUnlocked_choPhep() {
        SkillNode node = sampleNode();
        User user = new User();
        givenNodeExistsAndSaveEchoes(node, user);
        when(roadmapService.isNodeLocked(user, node)).thenReturn(false);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.IN_PROGRESS);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.IN_PROGRESS);
        assertThat(result.getCompletedAt()).isNull();
        verify(activityLogService, never()).log(any(), any(), any());
    }

    @Test
    @DisplayName("R6 · DONE + node mở khoá → cho phép, ghi completedAt và nhật ký")
    void rule6_done_nodeUnlocked_choPhep() {
        SkillNode node = sampleNode();
        User user = new User();
        givenNodeExistsAndSaveEchoes(node, user);
        when(roadmapService.isNodeLocked(user, node)).thenReturn(false);

        UserNodeProgress result =
                progressService.updateProgress(user, NODE_ID, ProgressStatus.DONE);

        assertThat(result.getStatus()).isEqualTo(ProgressStatus.DONE);
        assertThat(result.getCompletedAt()).isNotNull();
        verify(activityLogService).log(any(), any(), any());
    }

    // ================================================================
    // NHÓM TỪ CHỐI — A2
    // ================================================================

    @Test
    @DisplayName("R3 · IN_PROGRESS + node bị khoá → TỪ CHỐI 403")
    void rule3_inProgress_nodeLocked_tuChoi() {
        SkillNode node = sampleNode();
        User user = new User();
        when(skillNodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node));
        when(roadmapService.isNodeLocked(user, node)).thenReturn(true);

        assertThatThrownBy(() ->
                progressService.updateProgress(user, NODE_ID, ProgressStatus.IN_PROGRESS))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

        // Bị chặn thì tuyệt đối không được lưu hay ghi nhật ký.
        verify(userNodeProgressRepository, never()).save(any());
        verify(activityLogService, never()).log(any(), any(), any());
    }

    @Test
    @DisplayName("R5 · DONE + node bị khoá → TỪ CHỐI 403")
    void rule5_done_nodeLocked_tuChoi() {
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
}
