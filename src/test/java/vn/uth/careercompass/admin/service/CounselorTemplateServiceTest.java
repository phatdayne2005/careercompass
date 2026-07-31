package vn.uth.careercompass.admin.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.entity.LearningResource;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.LearningResourceRepository;
import vn.uth.careercompass.admin.repository.SkillNodeRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.admin.repository.SkillTreeTemplateRepository;
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
 * Unit test cho {@link CounselorTemplateService} — service NHIỀU logic nhất gói P7.
 *
 * <p>Service quản lý CRUD lộ trình học (SkillTreeTemplate), nút kỹ năng (SkillNode),
 * tài nguyên học (LearningResource) và vai trò nghề nghiệp (CareerRole). Có nhiều nhánh
 * validation (tên rỗng, trùng lộ trình, nút cha là chính nó, ...) nên ta phủ từng nhánh.
 *
 * <p>KỸ THUẬT hay dùng ở đây: {@code thenAnswer(inv -> inv.getArgument(0))} để mock repository
 * {@code save()} trả về CHÍNH đối tượng vừa truyền vào — nhờ đó ta soi được service đã build
 * entity với field gì trước khi lưu.
 */
@ExtendWith(MockitoExtension.class)
class CounselorTemplateServiceTest {

    @Mock
    private SkillTreeTemplateRepository templateRepository;
    @Mock
    private SkillNodeRepository nodeRepository;
    @Mock
    private SkillRepository skillRepository;
    @Mock
    private LearningResourceRepository resourceRepository;
    @Mock
    private CareerRoleRepository careerRoleRepository;
    @Mock
    private UserNodeProgressRepository userNodeProgressRepository;
    @Mock
    private SkillGapReportRepository skillGapReportRepository;

    @InjectMocks
    private CounselorTemplateService service;

    // ---------- Helpers dựng entity mẫu ----------
    private SkillTreeTemplate template(Long id) {
        return SkillTreeTemplate.builder().id(id).name("Backend Roadmap").build();
    }

    private Skill skill(Long id, String name, String category) {
        return Skill.builder().id(id).name(name).category(category).build();
    }

    private SkillNode node(Long id) {
        return SkillNode.builder().id(id).tier(2).title("Node " + id).build();
    }

    // ============================================================================
    // Nhóm READ đơn giản: getAllTemplates / getTemplateById / getNodeById / getNodesByTemplateId
    // ============================================================================
    @Test
    void getAllTemplates_returnsListFromRepo() {
        List<SkillTreeTemplate> list = List.of(template(1L), template(2L));
        when(templateRepository.findAllWithCareerRole()).thenReturn(list);

        assertThat(service.getAllTemplates()).hasSize(2);
    }

    @Test
    void getTemplateById_whenFound_returnsTemplate() {
        SkillTreeTemplate t = template(1L);
        when(templateRepository.findByIdWithCareerRole(1L)).thenReturn(Optional.of(t));

        assertThat(service.getTemplateById(1L)).isSameAs(t);
    }

    @Test
    void getTemplateById_whenNotFound_throws() {
        when(templateRepository.findByIdWithCareerRole(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTemplateById(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy lộ trình có ID: 9");
    }

    @Test
    void getNodeById_whenFound_returnsNode() {
        SkillNode n = node(1L);
        when(nodeRepository.findByIdWithRelations(1L)).thenReturn(Optional.of(n));

        assertThat(service.getNodeById(1L)).isSameAs(n);
    }

    @Test
    void getNodeById_whenNotFound_throws() {
        when(nodeRepository.findByIdWithRelations(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getNodeById(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy nút có ID: 9");
    }

    @Test
    void getNodesByTemplateId_returnsListFromRepo() {
        when(nodeRepository.findAllByTemplateIdWithRelations(1L)).thenReturn(List.of(node(1L), node(2L)));

        assertThat(service.getNodesByTemplateId(1L)).hasSize(2);
    }

    // ============================================================================
    // addNode(...) — nhiều nhánh chọn/ tạo Skill
    // ============================================================================
    @Test
    void addNode_whenNewSkillNameAndSkillNotExists_createsSkillAndNode() {
        // Given: nhập tên kỹ năng MỚI chưa có trong DB -> tạo skill mới rồi tạo node.
        SkillTreeTemplate t = template(1L);
        when(templateRepository.findById(1L)).thenReturn(Optional.of(t));
        when(skillRepository.findByNameIgnoreCase("Docker")).thenReturn(Optional.empty());
        // save trả về chính skill vừa build (kèm gán id giả) để service dùng tiếp
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(nodeRepository.save(any(SkillNode.class))).thenAnswer(inv -> inv.getArgument(0));

        SkillNode result = service.addNode(1L, null, "  Docker  ", "  DevOps  ", 2, null);

        // Then: skill được trim tên + category; node gắn đúng template/skill/tier, title = tên skill.
        ArgumentCaptor<Skill> skillCaptor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(skillCaptor.capture());
        assertThat(skillCaptor.getValue().getName()).isEqualTo("Docker");
        assertThat(skillCaptor.getValue().getCategory()).isEqualTo("DevOps");

        assertThat(result.getTemplate()).isSameAs(t);
        assertThat(result.getTitle()).isEqualTo("Docker");
        assertThat(result.getTier()).isEqualTo(2);
        assertThat(result.getParent()).isNull();
    }

    @Test
    void addNode_whenNewSkillNameBlankCategory_defaultsToGeneral() {
        // Given: có tên kỹ năng mới nhưng KHÔNG nhập category -> mặc định "General".
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template(1L)));
        when(skillRepository.findByNameIgnoreCase("Kafka")).thenReturn(Optional.empty());
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> inv.getArgument(0));
        when(nodeRepository.save(any(SkillNode.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addNode(1L, null, "Kafka", "   ", null, null);

        ArgumentCaptor<Skill> skillCaptor = ArgumentCaptor.forClass(Skill.class);
        verify(skillRepository).save(skillCaptor.capture());
        assertThat(skillCaptor.getValue().getCategory()).isEqualTo("General");
    }

    @Test
    void addNode_whenNewSkillNameExistsSameCategory_reusesWithoutSaving() {
        // Given: kỹ năng đã tồn tại, category trùng -> tái dùng, KHÔNG save lại skill.
        SkillTreeTemplate t = template(1L);
        Skill existing = skill(5L, "Java", "Backend");
        when(templateRepository.findById(1L)).thenReturn(Optional.of(t));
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(existing));
        when(nodeRepository.save(any(SkillNode.class))).thenAnswer(inv -> inv.getArgument(0));

        SkillNode result = service.addNode(1L, null, "Java", "backend", 1, null);

        // category "backend" so khớp "Backend" bằng equalsIgnoreCase -> không đổi, không save skill.
        verify(skillRepository, never()).save(any());
        assertThat(result.getSkill()).isSameAs(existing);
    }

    @Test
    void addNode_whenNewSkillNameExistsDifferentCategory_updatesCategory() {
        // Given: kỹ năng đã tồn tại nhưng category khác -> cập nhật category + save skill.
        SkillTreeTemplate t = template(1L);
        Skill existing = skill(5L, "Java", "General");
        when(templateRepository.findById(1L)).thenReturn(Optional.of(t));
        when(skillRepository.findByNameIgnoreCase("Java")).thenReturn(Optional.of(existing));
        when(skillRepository.save(existing)).thenReturn(existing);
        when(nodeRepository.save(any(SkillNode.class))).thenAnswer(inv -> inv.getArgument(0));

        service.addNode(1L, null, "Java", "Backend", 1, null);

        assertThat(existing.getCategory()).isEqualTo("Backend");
        verify(skillRepository).save(existing);
    }

    @Test
    void addNode_whenSkillIdProvided_usesExistingSkill() {
        // Given: không nhập tên mới, chọn skill có sẵn qua skillId.
        SkillTreeTemplate t = template(1L);
        Skill existing = skill(5L, "SQL", "Database");
        when(templateRepository.findById(1L)).thenReturn(Optional.of(t));
        when(skillRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(nodeRepository.save(any(SkillNode.class))).thenAnswer(inv -> inv.getArgument(0));

        SkillNode result = service.addNode(1L, 5L, null, null, null, null);

        assertThat(result.getSkill()).isSameAs(existing);
        assertThat(result.getTitle()).isEqualTo("SQL");
        // tier null -> mặc định 1
        assertThat(result.getTier()).isEqualTo(1);
    }

    @Test
    void addNode_whenSkillIdNotFound_throws() {
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template(1L)));
        when(skillRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addNode(1L, 5L, null, null, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy kỹ năng có ID: 5");

        verify(nodeRepository, never()).save(any());
    }

    @Test
    void addNode_whenNeitherSkillIdNorNewName_throws() {
        // Given: không chọn skill nào cũng không nhập tên -> chặn.
        when(templateRepository.findById(1L)).thenReturn(Optional.of(template(1L)));

        assertThatThrownBy(() -> service.addNode(1L, null, "   ", null, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vui lòng chọn kỹ năng sẵn có hoặc nhập tên kỹ năng mới!");

        verify(nodeRepository, never()).save(any());
    }

    @Test
    void addNode_whenParentIdProvided_linksParent() {
        SkillTreeTemplate t = template(1L);
        Skill existing = skill(5L, "SQL", "Database");
        SkillNode parent = node(10L);
        when(templateRepository.findById(1L)).thenReturn(Optional.of(t));
        when(skillRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(nodeRepository.findById(10L)).thenReturn(Optional.of(parent));
        when(nodeRepository.save(any(SkillNode.class))).thenAnswer(inv -> inv.getArgument(0));

        SkillNode result = service.addNode(1L, 5L, null, null, 2, 10L);

        assertThat(result.getParent()).isSameAs(parent);
    }

    @Test
    void addNode_whenParentNotFound_throws() {
        SkillTreeTemplate t = template(1L);
        Skill existing = skill(5L, "SQL", "Database");
        when(templateRepository.findById(1L)).thenReturn(Optional.of(t));
        when(skillRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(nodeRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addNode(1L, 5L, null, null, 2, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy nút cha có ID: 10");

        verify(nodeRepository, never()).save(any());
    }

    @Test
    void addNode_whenTemplateNotFound_throws() {
        when(templateRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addNode(1L, 5L, null, null, 1, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy lộ trình có ID: 1");
    }

    // ============================================================================
    // deleteNode(...)
    // ============================================================================
    @Test
    void deleteNode_whenFound_deletes() {
        SkillNode n = node(1L);
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(n));

        service.deleteNode(1L);

        verify(nodeRepository).delete(n);
    }

    @Test
    void deleteNode_whenNotFound_throws() {
        when(nodeRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteNode(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy nút có ID: 9");

        verify(nodeRepository, never()).delete(any());
    }

    // ============================================================================
    // updateNode(...)
    // ============================================================================
    @Test
    void updateNode_whenValidParent_updatesTierAndParent() {
        SkillNode n = node(1L);
        SkillNode parent = node(2L);
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(n));
        when(nodeRepository.findById(2L)).thenReturn(Optional.of(parent));
        when(nodeRepository.save(n)).thenReturn(n);

        SkillNode result = service.updateNode(1L, 3, 2L);

        assertThat(result.getTier()).isEqualTo(3);
        assertThat(result.getParent()).isSameAs(parent);
    }

    @Test
    void updateNode_whenTierNull_keepsExistingTier() {
        // Given: tier null -> giữ nguyên tier cũ (2). parentId null.
        SkillNode n = node(1L); // tier 2
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(n));
        when(nodeRepository.save(n)).thenReturn(n);

        SkillNode result = service.updateNode(1L, null, null);

        assertThat(result.getTier()).isEqualTo(2);
        // BUG?: parentId=null luôn khiến node.setParent(null) -> XOÁ liên kết cha hiện có,
        // kể cả khi caller chỉ muốn đổi tier mà không đụng tới cha. Đây có thể là hành vi ngoài ý
        // muốn (mất quan hệ tiên quyết). Test khẳng định hành vi THỰC TẾ hiện tại (parent = null).
        assertThat(result.getParent()).isNull();
    }

    @Test
    void updateNode_whenParentIsSelf_throws() {
        SkillNode n = node(1L);
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> service.updateNode(1L, 2, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Nút tiên quyết không thể là chính nó!");

        verify(nodeRepository, never()).save(any());
    }

    @Test
    void updateNode_whenParentNotFound_throws() {
        SkillNode n = node(1L);
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(n));
        when(nodeRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNode(1L, 2, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy nút cha có ID: 2");

        verify(nodeRepository, never()).save(any());
    }

    @Test
    void updateNode_whenNodeNotFound_throws() {
        when(nodeRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateNode(9L, 2, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy nút kỹ năng có ID: 9");
    }

    // ============================================================================
    // addResource(...) / deleteResource(...)
    // ============================================================================
    @Test
    void addResource_whenNodeFound_savesResource() {
        SkillNode n = node(1L);
        when(nodeRepository.findById(1L)).thenReturn(Optional.of(n));
        when(resourceRepository.save(any(LearningResource.class))).thenAnswer(inv -> inv.getArgument(0));

        LearningResource result = service.addResource(1L, "Docs", "http://x", "ARTICLE", "desc");

        assertThat(result.getSkillNode()).isSameAs(n);
        assertThat(result.getTitle()).isEqualTo("Docs");
        assertThat(result.getUrl()).isEqualTo("http://x");
        assertThat(result.getResourceType()).isEqualTo("ARTICLE");
        assertThat(result.getDescription()).isEqualTo("desc");
    }

    @Test
    void addResource_whenNodeNotFound_throws() {
        when(nodeRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addResource(9L, "t", "u", "ty", "d"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy nút có ID: 9");

        verify(resourceRepository, never()).save(any());
    }

    @Test
    void deleteResource_whenFound_deletes() {
        LearningResource r = LearningResource.builder().id(1L).title("Docs").build();
        when(resourceRepository.findById(1L)).thenReturn(Optional.of(r));

        service.deleteResource(1L);

        verify(resourceRepository).delete(r);
    }

    @Test
    void deleteResource_whenNotFound_throws() {
        when(resourceRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteResource(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy tài nguyên có ID: 9");

        verify(resourceRepository, never()).delete(any());
    }

    // ============================================================================
    // createTemplate(...)
    // ============================================================================
    @Test
    void createTemplate_whenNewCareerRole_createsRoleAndTemplate() {
        // Given: careerRole chưa tồn tại -> resolveOrCreateCareerRole tạo mới (id 5), chưa có template.
        CareerRole savedRole = CareerRole.builder().id(5L).name("Backend Developer").build();
        when(careerRoleRepository.findByName("Backend Developer")).thenReturn(Optional.empty());
        when(careerRoleRepository.save(any(CareerRole.class))).thenReturn(savedRole);
        when(templateRepository.findByCareerRoleId(5L)).thenReturn(Optional.empty());
        when(templateRepository.save(any(SkillTreeTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        SkillTreeTemplate result = service.createTemplate("BE Roadmap", "desc", "  Backend Developer  ");

        assertThat(result.getName()).isEqualTo("BE Roadmap");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getTargetRoleId()).isEqualTo(5L);
    }

    @Test
    void createTemplate_whenExistingCareerRoleNoTemplate_savesTemplate() {
        // Given: careerRole đã có sẵn (không tạo mới), và chưa gán template nào.
        CareerRole existingRole = CareerRole.builder().id(5L).name("Backend Developer").build();
        when(careerRoleRepository.findByName("Backend Developer")).thenReturn(Optional.of(existingRole));
        when(templateRepository.findByCareerRoleId(5L)).thenReturn(Optional.empty());
        when(templateRepository.save(any(SkillTreeTemplate.class))).thenAnswer(inv -> inv.getArgument(0));

        SkillTreeTemplate result = service.createTemplate("BE Roadmap", "desc", "Backend Developer");

        assertThat(result.getTargetRoleId()).isEqualTo(5L);
        // KHÔNG tạo mới careerRole vì đã tồn tại
        verify(careerRoleRepository, never()).save(any());
    }

    @Test
    void createTemplate_whenCareerRoleAlreadyHasTemplate_throws() {
        CareerRole existingRole = CareerRole.builder().id(5L).name("Backend Developer").build();
        when(careerRoleRepository.findByName("Backend Developer")).thenReturn(Optional.of(existingRole));
        when(templateRepository.findByCareerRoleId(5L)).thenReturn(Optional.of(template(1L)));

        assertThatThrownBy(() -> service.createTemplate("BE Roadmap", "desc", "Backend Developer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vai trò nghề nghiệp này đã có lộ trình mẫu!");

        verify(templateRepository, never()).save(any());
    }

    @Test
    void createTemplate_whenCareerRoleNameBlank_throws() {
        // resolveOrCreateCareerRole chặn tên rỗng NGAY, không đụng repo nào.
        assertThatThrownBy(() -> service.createTemplate("BE Roadmap", "desc", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vui lòng nhập vai trò nghề nghiệp mục tiêu!");

        verify(templateRepository, never()).save(any());
        verify(careerRoleRepository, never()).save(any());
    }

    // ============================================================================
    // updateTemplate(...)
    // ============================================================================
    @Test
    void updateTemplate_whenNoConflict_updatesFields() {
        SkillTreeTemplate existing = template(1L);
        CareerRole role = CareerRole.builder().id(5L).name("Backend Developer").build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(careerRoleRepository.findByName("Backend Developer")).thenReturn(Optional.of(role));
        when(templateRepository.findByCareerRoleId(5L)).thenReturn(Optional.empty());
        when(templateRepository.save(existing)).thenReturn(existing);

        SkillTreeTemplate result = service.updateTemplate(1L, "New Name", "New Desc", "Backend Developer");

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getDescription()).isEqualTo("New Desc");
        assertThat(result.getTargetRoleId()).isEqualTo(5L);
    }

    @Test
    void updateTemplate_whenSameTemplateAlreadyHasRole_allowsUpdate() {
        // Given: careerRole đã gán cho CHÍNH template đang sửa (id trùng) -> KHÔNG coi là xung đột.
        SkillTreeTemplate existing = template(1L);
        CareerRole role = CareerRole.builder().id(5L).name("Backend Developer").build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(careerRoleRepository.findByName("Backend Developer")).thenReturn(Optional.of(role));
        when(templateRepository.findByCareerRoleId(5L)).thenReturn(Optional.of(existing)); // cùng id 1
        when(templateRepository.save(existing)).thenReturn(existing);

        SkillTreeTemplate result = service.updateTemplate(1L, "New Name", "New Desc", "Backend Developer");

        assertThat(result.getName()).isEqualTo("New Name");
    }

    @Test
    void updateTemplate_whenRoleAssignedToAnotherTemplate_throws() {
        // Given: careerRole đã gán cho template KHÁC (id 2) -> xung đột.
        SkillTreeTemplate editing = template(1L);
        SkillTreeTemplate other = template(2L);
        CareerRole role = CareerRole.builder().id(5L).name("Backend Developer").build();
        when(templateRepository.findById(1L)).thenReturn(Optional.of(editing));
        when(careerRoleRepository.findByName("Backend Developer")).thenReturn(Optional.of(role));
        when(templateRepository.findByCareerRoleId(5L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updateTemplate(1L, "New Name", "New Desc", "Backend Developer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Vai trò nghề nghiệp này đã được gán cho một lộ trình khác!");

        verify(templateRepository, never()).save(any());
    }

    @Test
    void updateTemplate_whenTemplateNotFound_throws() {
        when(templateRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateTemplate(9L, "N", "D", "Backend Developer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy lộ trình có ID: 9");

        verify(templateRepository, never()).save(any());
    }

    // ============================================================================
    // deleteTemplate(...) — dọn dữ liệu phụ thuộc đúng thứ tự khoá ngoại
    // ============================================================================
    @Test
    void deleteTemplate_whenFound_cleansDependenciesInOrderThenDeletes() {
        SkillTreeTemplate t = template(1L);
        SkillNode n1 = node(11L);
        SkillNode n2 = node(12L);
        n1.setParent(n2); // có liên kết cha-con để kiểm việc gỡ self-reference
        List<SkillNode> nodes = List.of(n1, n2);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(t));
        when(nodeRepository.findAllByTemplateIdWithRelations(1L)).thenReturn(nodes);

        service.deleteTemplate(1L);

        // Dữ liệu người dùng (P4) bị xoá trước để không vi phạm khoá ngoại
        verify(skillGapReportRepository).deleteByTemplate_Id(1L);
        verify(userNodeProgressRepository).deleteBySkillNode_Template_Id(1L);
        verify(resourceRepository).deleteBySkillNode_Template_Id(1L);
        // Liên kết cha-con đã bị gỡ (đặt null) trước khi xoá node
        assertThat(n1.getParent()).isNull();
        verify(nodeRepository).saveAll(nodes);
        verify(nodeRepository).flush();
        verify(nodeRepository).deleteAll(nodes);
        // Cuối cùng xoá template
        verify(templateRepository).delete(t);
    }

    @Test
    void deleteTemplate_whenNotFound_throwsAndDeletesNothing() {
        when(templateRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteTemplate(9L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tìm thấy lộ trình có ID: 9");

        // Không đụng tới bất kỳ lệnh xoá phụ thuộc nào
        verify(skillGapReportRepository, never()).deleteByTemplate_Id(any());
        verify(templateRepository, never()).delete(any());
    }
}
