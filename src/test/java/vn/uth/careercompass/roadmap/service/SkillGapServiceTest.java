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
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.entity.UserSkill;
import vn.uth.careercompass.kernel.repository.UserSkillRepository;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapReportDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.dto.SkillSummaryDTO;
import vn.uth.careercompass.roadmap.entity.ProgressStatus;
import vn.uth.careercompass.roadmap.entity.SkillGapReport;
import vn.uth.careercompass.roadmap.entity.UserNodeProgress;
import vn.uth.careercompass.roadmap.repository.SkillGapReportRepository;
import vn.uth.careercompass.roadmap.repository.UserNodeProgressRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link SkillGapService}.
 *
 * <p>TRỌNG TÂM: logic so khớp skill — required (skill trong roadmap) vs acquired (skill user
 * đã có = UserSkill khai onboarding + node đã DONE). Kết quả tách thành matched / missing.
 * Đây là "phép trừ tập hợp" nên ta test bằng danh sách cụ thể để chắc từng nhánh.
 *
 * <p>RoadmapService bị mock: {@code resolveTemplate} và {@code calculatePercent} là trách nhiệm
 * của nó (đã test riêng), ở đây ta chỉ dạy nó trả về giá trị cố định.
 */
@ExtendWith(MockitoExtension.class)
class SkillGapServiceTest {

    @Mock
    private RoadmapService roadmapService;
    @Mock
    private SkillNodeRepository skillNodeRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private UserSkillRepository userSkillRepository;
    @Mock
    private SkillGapReportRepository skillGapReportRepository;
    @Mock
    private UserNodeProgressRepository userNodeProgressRepository;

    @InjectMocks
    private SkillGapService skillGapService;

    // ---------- Helpers ----------

    private Skill skill(Long id, String name) {
        return Skill.builder().id(id).name(name).category("Backend").build();
    }

    private SkillNode node(Long id, SkillTreeTemplate template, Skill skill) {
        return SkillNode.builder().id(id).template(template).skill(skill).title(skill.getName()).build();
    }

    // ============================================================
    // analyze(user, templateId) — so khớp skill
    // ============================================================

    @Test
    void analyze_splitsRequiredIntoMatchedAndMissing() {
        // Given: roadmap yêu cầu 3 skill: Java, Spring, Docker
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).name("Backend").build();
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring");
        Skill docker = skill(3L, "Docker");
        User user = new User();

        when(roadmapService.resolveTemplate(user, 10L)).thenReturn(template);
        when(skillNodeRepository.findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(10L))
                .thenReturn(List.of(node(101L, template, java), node(102L, template, spring), node(103L, template, docker)));

        // User đã khai Java (UserSkill) + đã DONE node Spring (progress) -> acquired = {Java, Spring}
        when(userSkillRepository.findByUser(user))
                .thenReturn(List.of(UserSkill.builder().user(user).skill(java).build()));
        UserNodeProgress springDone = UserNodeProgress.builder()
                .skillNode(node(102L, template, spring)).status(ProgressStatus.DONE).build();
        when(userNodeProgressRepository.findByUserAndSkillNode_Template_Id(user, 10L))
                .thenReturn(List.of(springDone));
        // matchPercent do RoadmapService tính -> mock cố định
        when(roadmapService.calculatePercent(2, 3)).thenReturn(66.67);

        // When
        SkillGapResultDTO result = skillGapService.analyze(user, 10L);

        // Then: đếm đúng
        assertThat(result.getRequiredSkillCount()).isEqualTo(3);
        assertThat(result.getMatchedSkillCount()).isEqualTo(2);
        assertThat(result.getMissingSkillCount()).isEqualTo(1);
        assertThat(result.getMatchPercent()).isEqualTo(66.67);
        assertThat(result.getTemplate().getName()).isEqualTo("Backend");

        // matched = Java, Spring (sắp theo tên); missing = Docker
        assertThat(result.getMatchedSkills()).extracting(SkillSummaryDTO::getName)
                .containsExactly("Java", "Spring");
        assertThat(result.getMissingSkills()).extracting(SkillSummaryDTO::getName)
                .containsExactly("Docker");
    }

    @Test
    void analyze_whenUserHasNoSkills_allRequiredAreMissing() {
        // Given: user chưa có skill nào -> tất cả required đều "thiếu"
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).name("Backend").build();
        Skill java = skill(1L, "Java");
        User user = new User();

        when(roadmapService.resolveTemplate(user, null)).thenReturn(template);
        when(skillNodeRepository.findByTemplateIdOrderByTierAscOrderIndexAscIdAsc(10L))
                .thenReturn(List.of(node(101L, template, java)));
        when(userSkillRepository.findByUser(user)).thenReturn(List.of());
        when(userNodeProgressRepository.findByUserAndSkillNode_Template_Id(user, 10L)).thenReturn(List.of());
        when(roadmapService.calculatePercent(0, 1)).thenReturn(0.0);

        // When
        SkillGapResultDTO result = skillGapService.analyze(user, null);

        // Then
        assertThat(result.getMatchedSkills()).isEmpty();
        assertThat(result.getMissingSkills()).extracting(SkillSummaryDTO::getName).containsExactly("Java");
        assertThat(result.getMatchPercent()).isEqualTo(0.0);
    }

    // ============================================================
    // getAcquiredSkills(user) — gộp + khử trùng + sắp xếp
    // ============================================================

    @Test
    void getAcquiredSkills_mergesSourcesDeduplicatesAndSortsByName() {
        // Given: UserSkill có Spring, Java; node DONE có Java (trùng) + Docker
        User user = new User();
        Skill java = skill(1L, "Java");
        Skill spring = skill(2L, "Spring");
        Skill docker = skill(3L, "Docker");

        when(userSkillRepository.findByUserWithSkill(user)).thenReturn(List.of(
                UserSkill.builder().user(user).skill(spring).build(),
                UserSkill.builder().user(user).skill(java).build()));

        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).build();
        when(userNodeProgressRepository.findByUserAndStatusWithSkill(user, ProgressStatus.DONE)).thenReturn(List.of(
                UserNodeProgress.builder().skillNode(node(101L, template, java)).status(ProgressStatus.DONE).build(),
                UserNodeProgress.builder().skillNode(node(103L, template, docker)).status(ProgressStatus.DONE).build()));

        // When
        List<SkillSummaryDTO> result = skillGapService.getAcquiredSkills(user);

        // Then: Java xuất hiện 1 lần (khử trùng), sắp theo tên -> Docker, Java, Spring
        assertThat(result).extracting(SkillSummaryDTO::getName).containsExactly("Docker", "Java", "Spring");
    }

    // ============================================================
    // addAcquiredSkill(user, skillId, templateId)
    // ============================================================

    @Test
    void addAcquiredSkill_whenSkillIdNull_throwsBadRequest() {
        assertThatThrownBy(() -> skillGapService.addAcquiredSkill(new User(), null, 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        verify(userSkillRepository, never()).save(any());
    }

    @Test
    void addAcquiredSkill_whenSkillNotFound_throwsNotFound() {
        when(skillRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillGapService.addAcquiredSkill(new User(), 5L, 10L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));

        verify(userSkillRepository, never()).save(any());
    }

    @Test
    void addAcquiredSkill_whenNewSkillAndNoNode_savesUserSkillAndCreatesSupplementalNode() {
        // Given: skill tồn tại, user CHƯA có, roadmap CHƯA có node cho skill này
        User user = new User();
        Skill java = skill(1L, "Java");
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).build();

        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));
        when(userSkillRepository.existsByUserAndSkill(user, java)).thenReturn(false);
        when(roadmapService.resolveTemplate(user, 10L)).thenReturn(template);
        when(skillNodeRepository.findByTemplate_IdAndSkill_Id(10L, 1L)).thenReturn(Optional.empty());
        // nextOrderIndex: chưa có node nào -> orElse(0)
        when(skillNodeRepository.findTopByTemplate_IdOrderByOrderIndexDesc(10L)).thenReturn(Optional.empty());

        // When
        skillGapService.addAcquiredSkill(user, 1L, 10L);

        // Then: lưu UserSkill
        verify(userSkillRepository).save(any(UserSkill.class));
        // Và tạo node bổ sung (customNode=true, tier 1, orderIndex 0)
        ArgumentCaptor<SkillNode> nodeCaptor = ArgumentCaptor.forClass(SkillNode.class);
        verify(skillNodeRepository).save(nodeCaptor.capture());
        SkillNode created = nodeCaptor.getValue();
        assertThat(created.getSkill()).isEqualTo(java);
        assertThat(created.getTemplate()).isEqualTo(template);
        assertThat(created.getTier()).isEqualTo(1);
        assertThat(created.getOrderIndex()).isEqualTo(0);
        assertThat(created.getCustomNode()).isTrue();
    }

    @Test
    void addAcquiredSkill_whenAlreadyHasSkillAndNodeExists_doesNotSaveAnything() {
        // Given: user đã có skill + node đã tồn tại -> không tạo gì thêm (idempotent)
        User user = new User();
        Skill java = skill(1L, "Java");
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).build();

        when(skillRepository.findById(1L)).thenReturn(Optional.of(java));
        when(userSkillRepository.existsByUserAndSkill(user, java)).thenReturn(true);
        when(roadmapService.resolveTemplate(user, 10L)).thenReturn(template);
        when(skillNodeRepository.findByTemplate_IdAndSkill_Id(10L, 1L))
                .thenReturn(Optional.of(node(101L, template, java)));

        // When
        skillGapService.addAcquiredSkill(user, 1L, 10L);

        // Then: không lưu UserSkill mới, không tạo node mới
        verify(userSkillRepository, never()).save(any());
        verify(skillNodeRepository, never()).save(any());
    }

    // ============================================================
    // saveReport(user, result, pdfPath)
    // ============================================================

    @Test
    void saveReport_buildsAndPersistsReportWithJoinedSkillNames() {
        // Given: kết quả phân tích -> lưu thành SkillGapReport
        User user = new User();
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).name("Backend").build();
        SkillGapResultDTO result = SkillGapResultDTO.builder()
                .template(RoadmapTemplateDTO.from(template))
                .requiredSkillCount(3).matchedSkillCount(2).missingSkillCount(1)
                .matchedSkills(List.of(
                        SkillSummaryDTO.builder().id(1L).name("Java").build(),
                        SkillSummaryDTO.builder().id(2L).name("Spring").build()))
                .missingSkills(List.of(SkillSummaryDTO.builder().id(3L).name("Docker").build()))
                .build();

        when(roadmapService.resolveTemplate(user, 10L)).thenReturn(template);
        // save trả lại chính report để DTO.from map được template
        when(skillGapReportRepository.save(any(SkillGapReport.class))).thenAnswer(inv -> inv.getArgument(0));

        // When
        SkillGapReportDTO dto = skillGapService.saveReport(user, result, "reports/x.pdf");

        // Then: soi report thực sự lưu vào DB
        ArgumentCaptor<SkillGapReport> captor = ArgumentCaptor.forClass(SkillGapReport.class);
        verify(skillGapReportRepository).save(captor.capture());
        SkillGapReport saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getTemplate()).isEqualTo(template);
        assertThat(saved.getSummary()).isEqualTo("Đã đạt 2/3 kỹ năng, còn thiếu 1 kỹ năng.");
        assertThat(saved.getMatchedSkillNames()).isEqualTo("Java, Spring");
        assertThat(saved.getMissingSkillNames()).isEqualTo("Docker");
        assertThat(saved.getPdfPath()).isEqualTo("reports/x.pdf");

        // DTO trả về map đúng
        assertThat(dto.getSummary()).isEqualTo("Đã đạt 2/3 kỹ năng, còn thiếu 1 kỹ năng.");
        assertThat(dto.getPdfPath()).isEqualTo("reports/x.pdf");
    }

    // ============================================================
    // getReports / getReport
    // ============================================================

    @Test
    void getReports_mapsAllUserReportsToDto() {
        User user = new User();
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).name("Backend").build();
        SkillGapReport report = SkillGapReport.builder()
                .id(1L).user(user).template(template).summary("s").build();
        when(skillGapReportRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(report));

        List<SkillGapReportDTO> result = skillGapService.getReports(user);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getTemplate().getName()).isEqualTo("Backend");
    }

    @Test
    void getReport_whenExists_returnsDto() {
        User user = new User();
        SkillTreeTemplate template = SkillTreeTemplate.builder().id(10L).name("Backend").build();
        SkillGapReport report = SkillGapReport.builder()
                .id(7L).user(user).template(template).summary("s").build();
        when(skillGapReportRepository.findByIdAndUser(7L, user)).thenReturn(Optional.of(report));

        SkillGapReportDTO dto = skillGapService.getReport(user, 7L);

        assertThat(dto.getId()).isEqualTo(7L);
    }

    @Test
    void getReport_whenNotFound_throwsNotFound() {
        User user = new User();
        when(skillGapReportRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> skillGapService.getReport(user, 99L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
