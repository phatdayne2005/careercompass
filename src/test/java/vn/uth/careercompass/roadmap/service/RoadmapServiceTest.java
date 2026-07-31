package vn.uth.careercompass.roadmap.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.entity.LearningResource;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.LearningResourceRepository;
import vn.uth.careercompass.admin.repository.SkillNodeRepository;
import vn.uth.careercompass.admin.repository.SkillTreeTemplateRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;
import vn.uth.careercompass.roadmap.dto.RoadmapNodeDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.RoadmapViewDTO;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link RoadmapService}.
 *
 * <p>WHY: RoadmapService là "trung tâm" của gói P4 — nó ghép SkillNode (do P7 seed) với
 * tiến độ của user (UserNodeProgress) và skill đã khai ở Onboarding (UserSkill) để dựng
 * cây lộ trình có gating theo tầng (tier). Ta mock toàn bộ repository để test RIÊNG logic
 * ghép/tô trạng thái/khóa node, KHÔNG đụng DB thật.
 *
 * <p>Lưu ý: {@code @InjectMocks} tạo RoadmapService THẬT nên các method trong cùng class
 * (vd resolveTemplate, calculatePercent) chạy thật — chỉ repository bị mock.
 */
@ExtendWith(MockitoExtension.class)
class RoadmapServiceTest {

    @Mock
    private SkillTreeTemplateRepository skillTreeTemplateRepository;
    @Mock
    private SkillNodeRepository skillNodeRepository;
    @Mock
    private UserNodeProgressRepository userNodeProgressRepository;
    @Mock
    private LearningResourceRepository learningResourceRepository;
    @Mock
    private UserSkillRepository userSkillRepository;

    @InjectMocks
    private RoadmapService roadmapService;

    // ---------- Helpers dựng dữ liệu test cho gọn ----------

    private Skill skill(Long id, String name) {
        return Skill.builder().id(id).name(name).category("Backend").build();
    }

    private SkillNode node(Long id, SkillTreeTemplate template, Skill skill, int tier) {
        return SkillNode.builder()
                .id(id)
                .template(template)
                .skill(skill)
                .title(skill.getName() + " node")
                .description("desc")
                .tier(tier)
                .orderIndex(0)
                .requiredLevel(1)
                .build();
    }

    // ============================================================
    // getActiveTemplates()
    // ============================================================

    @Test
    void getActiveTemplates_mapsEntitiesToDto() {
        // Given: 2 template active trong DB
        SkillTreeTemplate t1 = SkillTreeTemplate.builder().id(1L).name("Backend").description("d1").build();
        SkillTreeTemplate t2 = SkillTreeTemplate.builder().id(2L).name("Frontend").description("d2").build();
        when(skillTreeTemplateRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(t1, t2));

        // When
        List<RoadmapTemplateDTO> result = roadmapService.getActiveTemplates();

        // Then: đúng số lượng + map đúng field
        assertThat(result).hasSize(2);
        assertThat(result).extracting(RoadmapTemplateDTO::getName).containsExactly("Backend", "Frontend");
    }

    // ============================================================
    // resolveTemplate(user, templateId)
    // ============================================================

    @Test
    void resolveTemplate_whenTemplateIdGiven_returnsThatTemplate() {
        // Given: truyền templateId cụ thể -> tra findById
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).name("Backend").build();
        when(skillTreeTemplateRepository.findById(10L)).thenReturn(Optional.of(template));

        // When
        SkillTreeTemplate result = roadmapService.resolveTemplate(new User(), 10L);

        // Then
        assertThat(result).isEqualTo(template);
    }

    @Test
    void resolveTemplate_whenTemplateIdNotFound_throwsNotFound() {
        // Given: id không tồn tại
        when(skillTreeTemplateRepository.findById(99L)).thenReturn(Optional.empty());

        // When + Then: ResponseStatusException 404
        assertThatThrownBy(() -> roadmapService.resolveTemplate(new User(), 99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void resolveTemplate_whenNoTemplateIdButUserHasTargetRole_usesTargetRoleTemplate() {
        // Given: user có careerRole (targetRoleId=5) -> ưu tiên template của role đó
        CareerRole role = CareerRole.builder().id(5L).name("Backend Dev").build();
        User user = User.builder().careerRole(role).build();
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(7L).name("Backend").build();
        when(skillTreeTemplateRepository.findFirstByTargetRoleIdAndActiveTrueOrderByIdAsc(5L))
                .thenReturn(Optional.of(template));

        // When
        SkillTreeTemplate result = roadmapService.resolveTemplate(user, null);

        // Then
        assertThat(result).isEqualTo(template);
    }

    @Test
    void resolveTemplate_whenTargetRoleHasNoTemplate_fallsBackToFirstActive() {
        // Given: user có targetRole nhưng role đó CHƯA có template -> fallback template active đầu tiên
        CareerRole role = CareerRole.builder().id(5L).name("Backend Dev").build();
        User user = User.builder().careerRole(role).build();
        SkillTreeTemplate fallback = SkillTreeTemplate.builder().id(1L).name("Generic").build();
        when(skillTreeTemplateRepository.findFirstByTargetRoleIdAndActiveTrueOrderByIdAsc(5L))
                .thenReturn(Optional.empty());
        when(skillTreeTemplateRepository.findFirstByActiveTrueOrderByNameAsc())
                .thenReturn(Optional.of(fallback));

        // When
        SkillTreeTemplate result = roadmapService.resolveTemplate(user, null);

        // Then
        assertThat(result).isEqualTo(fallback);
    }

    @Test
    void resolveTemplate_whenNoTemplateIdAndNoTargetRole_usesFirstActive() {
        // Given: user KHÔNG có targetRole -> lấy template active đầu tiên
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(1L).name("Generic").build();
        when(skillTreeTemplateRepository.findFirstByActiveTrueOrderByNameAsc())
                .thenReturn(Optional.of(template));

        // When
        SkillTreeTemplate result = roadmapService.resolveTemplate(new User(), null);

        // Then
        assertThat(result).isEqualTo(template);
    }

    @Test
    void resolveTemplate_whenNoActiveTemplateExists_throwsNotFound() {
        // Given: không có template nào -> 404 "Chưa có roadmap nào"
        when(skillTreeTemplateRepository.findFirstByActiveTrueOrderByNameAsc())
                .thenReturn(Optional.empty());

        // When + Then
        assertThatThrownBy(() -> roadmapService.resolveTemplate(new User(), null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(404));
    }

    // ============================================================
    // calculatePercent(completed, total) — logic thuần
    // ============================================================

    @Test
    void calculatePercent_whenTotalZero_returnsZero() {
        // Chia cho 0 -> phải trả 0.0 (guard), không ném ArithmeticException
        assertThat(roadmapService.calculatePercent(0, 0)).isEqualTo(0.0);
    }

    @Test
    void calculatePercent_roundsToTwoDecimals() {
        // 1/3 = 33.333... -> làm tròn 2 chữ số = 33.33
        assertThat(roadmapService.calculatePercent(1, 3)).isEqualTo(33.33);
        // 1/8 = 12.5 (tròn đẹp)
        assertThat(roadmapService.calculatePercent(1, 8)).isEqualTo(12.5);
        // 4/4 = 100.0
        assertThat(roadmapService.calculatePercent(4, 4)).isEqualTo(100.0);
    }

    // ============================================================
    // isNodeLocked(user, node)
    // ============================================================

    @Test
    void isNodeLocked_whenTierOne_alwaysUnlocked() {
        // Node tầng 1 không bao giờ bị khóa -> return false ngay, không tra DB
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).build();
        SkillNode tier1 = node(101L, template, skill(1L, "Java"), 1);

        assertThat(roadmapService.isNodeLocked(new User(), tier1)).isFalse();
    }

    @Test
    void isNodeLocked_whenLowerTierNotAllDone_returnsLocked() {
        // Given: node tầng 2, còn node tầng 1 CHƯA hoàn thành -> bị khóa
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).build();
        SkillNode tier1 = node(101L, template, skill(1L, "Java"), 1);
        SkillNode tier2 = node(102L, template, skill(2L, "Spring"), 2);
        User user = new User();

        // Chưa có tiến độ nào -> doneNodeIds rỗng
        when(userNodeProgressRepository.findByUserAndSkillNode_Template_Id(user, 10L))
                .thenReturn(List.of());
        when(skillNodeRepository.findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(10L))
                .thenReturn(List.of(tier1, tier2));

        // When + Then: có node tầng thấp hơn chưa DONE -> khóa
        assertThat(roadmapService.isNodeLocked(user, tier2)).isTrue();
    }

    @Test
    void isNodeLocked_whenAllLowerTiersDone_returnsUnlocked() {
        // Given: node tầng 2, node tầng 1 đã DONE hết -> mở khóa
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).build();
        SkillNode tier1 = node(101L, template, skill(1L, "Java"), 1);
        SkillNode tier2 = node(102L, template, skill(2L, "Spring"), 2);
        User user = new User();

        UserNodeProgress done = UserNodeProgress.builder()
                .skillNode(tier1).status(ProgressStatus.DONE).build();
        when(userNodeProgressRepository.findByUserAndSkillNode_Template_Id(user, 10L))
                .thenReturn(List.of(done));
        when(skillNodeRepository.findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(10L))
                .thenReturn(List.of(tier1, tier2));

        // When + Then
        assertThat(roadmapService.isNodeLocked(user, tier2)).isFalse();
    }

    // ============================================================
    // getRoadmap(user, templateId) — ghép node + tiến độ + gating
    // ============================================================

    @Test
    void getRoadmap_promotesAcquiredSkillToDone_gatesUpperTiers_andComputesStats() {
        // Given: 1 roadmap 3 node ở 3 tầng khác nhau
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).name("Backend").build();
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring");
        Skill docker = skill(3L, "Docker");
        SkillNode n1 = node(101L, template, java, 1);
        SkillNode n2 = node(102L, template, spring, 2);
        SkillNode n3 = node(103L, template, docker, 3);
        User user = new User();

        when(skillTreeTemplateRepository.findById(10L)).thenReturn(Optional.of(template));
        when(skillNodeRepository.findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(10L))
                .thenReturn(List.of(n1, n2, n3));
        // Chưa bấm đánh dấu node nào -> không có bản ghi tiến độ
        when(userNodeProgressRepository.findByUserAndSkillNode_Template_Id(user, 10L))
                .thenReturn(List.of());
        // n1 có 1 tài liệu học
        LearningResource res = LearningResource.builder()
                .skillNode(n1).title("Java Docs").url("http://java").resourceType("DOCUMENTATION").build();
        when(learningResourceRepository.findBySkillNode_Template_IdOrderByIdAsc(10L))
                .thenReturn(List.of(res));
        // User đã khai skill Java ở Onboarding -> node Java auto DONE dù chưa bấm
        when(userSkillRepository.findByUserWithSkill(user))
                .thenReturn(List.of(UserSkill.builder().user(user).skill(java).build()));

        // When
        RoadmapViewDTO view = roadmapService.getRoadmap(user, 10L);

        // Then: thống kê tổng quan
        assertThat(view.getTotalNodes()).isEqualTo(3);
        assertThat(view.getCompletedNodes()).isEqualTo(1);         // chỉ node Java DONE
        assertThat(view.getCompletionPercent()).isEqualTo(33.33);  // 1/3
        assertThat(view.getTemplate().getName()).isEqualTo("Backend");

        // Node Java (tầng 1): DONE nhờ acquired skill + có resource
        RoadmapNodeDTO javaNode = findNode(view, 101L);
        assertThat(javaNode.getStatus()).isEqualTo(ProgressStatus.DONE);
        assertThat(javaNode.isLocked()).isFalse();
        assertThat(javaNode.getResources()).hasSize(1);
        assertThat(javaNode.getResources().get(0).getTitle()).isEqualTo("Java Docs");

        // Node Spring (tầng 2): tầng 1 đã xong hết -> KHÔNG khóa, nhưng vẫn NOT_STARTED
        RoadmapNodeDTO springNode = findNode(view, 102L);
        assertThat(springNode.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(springNode.isLocked()).isFalse();

        // Node Docker (tầng 3): tầng 2 CHƯA xong -> bị khóa
        RoadmapNodeDTO dockerNode = findNode(view, 103L);
        assertThat(dockerNode.getStatus()).isEqualTo(ProgressStatus.NOT_STARTED);
        assertThat(dockerNode.isLocked()).isTrue();
    }

    private RoadmapNodeDTO findNode(RoadmapViewDTO view, Long nodeId) {
        return view.getNodes().stream()
                .filter(n -> n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow();
    }
}
