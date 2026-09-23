package vn.uth.careercompass.admin.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.entity.LearningResource;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.entity.SkillNode;
import vn.uth.careercompass.admin.entity.SkillTreeTemplate;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.LearningResourceRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.admin.service.CounselorTemplateService;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiểm thử tầng web của chức năng biên soạn Skill Tree Template (FR8.1 → FR8.4).
 *
 * Trước lớp này CounselorTemplateController có độ bao phủ 0% — 82 dòng lệnh và 20 nhánh
 * không có test đơn vị nào chạm tới, dù đây là controller lớn nhất dự án. Phần nghiệp vụ
 * nằm ở CounselorTemplateService đã phủ 100%, nhưng bản thân controller còn hai khối
 * logic RIÊNG mà service không có: lọc danh sách kỹ năng còn dùng được
 * (getAvailableSkills) và chuẩn hoá danh sách loại tài nguyên (getCleanResourceTypes).
 * Hai khối này chỉ chạy qua đường HTTP nên phải kiểm ở đây.
 */
@WebMvcTest(value = CounselorTemplateController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "counselor@uth.edu.vn", roles = "COUNSELOR")
@DisplayName("FR8 — Biên soạn Skill Tree Template (tầng web)")
class CounselorTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CounselorTemplateService counselorTemplateService;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private LearningResourceRepository resourceRepository;

    @MockitoBean
    private CareerRoleRepository careerRoleRepository;

    /** GlobalModelAdvice nạp sẵn vào mọi trang nên lát cắt web nào cũng cần bean này. */
    @MockitoBean
    private vn.uth.careercompass.kernel.service.MarkdownRenderer markdownRenderer;

    private static final CareerRole VAI_TRO = CareerRole.builder()
            .id(1L).name("Backend Developer").build();

    private static final SkillTreeTemplate TEMPLATE = SkillTreeTemplate.builder()
            .id(10L).name("Lộ trình Backend").description("Mô tả").careerRole(VAI_TRO).build();

    private static SkillNode node(long id, long skillId, String tenKyNang) {
        return SkillNode.builder()
                .id(id)
                .template(TEMPLATE)
                .skill(Skill.builder().id(skillId).name(tenKyNang).category("Backend").build())
                .tier(1)
                .build();
    }

    // ================= FR8.1 — Tạo, sửa, xoá template =================

    @Test
    @DisplayName("FR8.1 · GET /counselor/templates trả danh sách template")
    void listTemplates_traVeDanhSach() throws Exception {
        when(counselorTemplateService.getAllTemplates()).thenReturn(List.of(TEMPLATE));

        mockMvc.perform(get("/counselor/templates"))
                .andExpect(status().isOk())
                .andExpect(view().name("counselor/templates"))
                .andExpect(model().attribute("templates", List.of(TEMPLATE)));
    }

    @Test
    @DisplayName("FR8.1 · Tạo template hợp lệ thì chuyển hướng và không kèm lỗi")
    void createTemplate_hopLe_chuyenHuong() throws Exception {
        mockMvc.perform(post("/counselor/templates")
                        .param("name", "Lộ trình Frontend")
                        .param("description", "Mô tả")
                        .param("careerRoleName", "Frontend Developer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/counselor/templates"))
                .andExpect(flash().attributeCount(0));

        verify(counselorTemplateService)
                .createTemplate("Lộ trình Frontend", "Mô tả", "Frontend Developer");
    }

    @Test
    @DisplayName("FR8.1 · Tên template trùng thì hiện thông báo lỗi, không vỡ trang")
    void createTemplate_tenTrung_hienLoi() throws Exception {
        when(counselorTemplateService.createTemplate(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Tên template đã tồn tại."));

        mockMvc.perform(post("/counselor/templates")
                        .param("name", "Lộ trình Backend")
                        .param("description", "Mô tả")
                        .param("careerRoleName", "Backend Developer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/counselor/templates"))
                .andExpect(flash().attribute("error", "Tên template đã tồn tại."));
    }

    @Test
    @DisplayName("FR8.1 · Sửa template hợp lệ thì chuyển hướng về danh sách")
    void updateTemplate_hopLe_chuyenHuong() throws Exception {
        mockMvc.perform(post("/counselor/templates/10/update")
                        .param("name", "Tên mới")
                        .param("description", "Mô tả mới")
                        .param("careerRoleName", "Backend Developer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/counselor/templates"));

        verify(counselorTemplateService)
                .updateTemplate(10L, "Tên mới", "Mô tả mới", "Backend Developer");
    }

    @Test
    @DisplayName("FR8.1 · Sửa template không tồn tại thì hiện lỗi thay vì HTTP 500")
    void updateTemplate_khongTonTai_hienLoi() throws Exception {
        when(counselorTemplateService.updateTemplate(eq(99L), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Không tìm thấy template."));

        mockMvc.perform(post("/counselor/templates/99/update")
                        .param("name", "Tên mới")
                        .param("description", "Mô tả mới")
                        .param("careerRoleName", "Backend Developer"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Không tìm thấy template."));
    }

    @Test
    @DisplayName("FR8.1 · Xoá template hợp lệ thì chuyển hướng về danh sách")
    void deleteTemplate_hopLe_chuyenHuong() throws Exception {
        mockMvc.perform(post("/counselor/templates/10/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/counselor/templates"));

        verify(counselorTemplateService).deleteTemplate(10L);
    }

    @Test
    @DisplayName("FR8.1 · Xoá template đang được dùng thì hiện lỗi")
    void deleteTemplate_dangDuocDung_hienLoi() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Template đang được sử dụng."))
                .when(counselorTemplateService).deleteTemplate(10L);

        mockMvc.perform(post("/counselor/templates/10/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("error", "Template đang được sử dụng."));
    }

    // ============ Trình soạn thảo — hai khối logic riêng của controller ============

    @Test
    @DisplayName("FR8.2 · Trình soạn thảo chỉ đề xuất kỹ năng CHƯA có trong cây")
    void editor_loaiBoKyNangDaDung() throws Exception {
        Skill daDung = Skill.builder().id(1L).name("Java").category("Backend").build();
        Skill conTrong = Skill.builder().id(2L).name("Docker").category("DevOps").build();

        when(counselorTemplateService.getTemplateById(10L)).thenReturn(TEMPLATE);
        when(counselorTemplateService.getNodesByTemplateId(10L))
                .thenReturn(List.of(node(100L, 1L, "Java")));
        when(skillRepository.findAll()).thenReturn(List.of(daDung, conTrong));
        when(skillRepository.findDistinctCategories()).thenReturn(List.of("Backend", "DevOps"));
        when(resourceRepository.findDistinctResourceTypes()).thenReturn(List.of());

        mockMvc.perform(get("/counselor/templates/10/editor"))
                .andExpect(status().isOk())
                .andExpect(view().name("counselor/editor"))
                .andExpect(model().attribute("template", TEMPLATE))
                // Java đã nằm trong cây nên không được đề xuất lại; chỉ còn Docker.
                .andExpect(model().attribute("allSkills", List.of(conTrong)))
                .andExpect(model().attribute("currentNode", (Object) null));
    }

    @Test
    @DisplayName("FR8.3 · Loại tài nguyên trong CSDL được chuẩn hoá và gộp vào danh sách chuẩn")
    void editor_chuanHoaLoaiTaiNguyen() throws Exception {
        when(counselorTemplateService.getTemplateById(10L)).thenReturn(TEMPLATE);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of());
        when(skillRepository.findAll()).thenReturn(List.of());
        when(skillRepository.findDistinctCategories()).thenReturn(List.of());
        // Dữ liệu bẩn thật sự gặp trong CSDL: null, chuỗi trắng, chữ thường, thừa khoảng trắng,
        // và một giá trị đã nằm sẵn trong danh sách chuẩn.
        when(resourceRepository.findDistinctResourceTypes())
                .thenReturn(java.util.Arrays.asList(null, "   ", "  podcast ", "video", "WEBINAR"));

        var ketQua = mockMvc.perform(get("/counselor/templates/10/editor"))
                .andExpect(status().isOk())
                .andReturn();

        @SuppressWarnings("unchecked")
        List<String> loai = (List<String>) ketQua.getModelAndView()
                .getModel().get("resourceTypes");

        assertThat(loai)
                .as("sáu loại chuẩn luôn đứng đầu, theo đúng thứ tự khai báo")
                .startsWith("VIDEO", "ARTICLE", "COURSE", "DOCUMENTATION", "BOOK", "SLIDES")
                .as("giá trị bẩn được viết hoa và cắt khoảng trắng")
                .contains("PODCAST", "WEBINAR")
                .as("null và chuỗi trắng bị loại, 'video' không nhân đôi với 'VIDEO'")
                .doesNotContainNull()
                .doesNotHaveDuplicates()
                .hasSize(8);
    }

    // ================= FR8.2 — Thêm / xoá node kỹ năng =================

    @Test
    @DisplayName("FR8.2 · Thêm node bằng kỹ năng có sẵn thì chuyển id sang kiểu số")
    void addNode_kyNangCoSan_chuyenIdSangSo() throws Exception {
        when(counselorTemplateService.getTemplateById(10L)).thenReturn(TEMPLATE);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of());
        when(skillRepository.findAll()).thenReturn(List.of());
        when(skillRepository.findDistinctCategories()).thenReturn(List.of());

        mockMvc.perform(post("/counselor/templates/10/nodes")
                        .param("skillId", " 7 ")
                        .param("tier", "2")
                        .param("parentId", " 3 "))
                .andExpect(status().isOk())
                .andExpect(view().name("counselor/editor :: editor-left-pane"));

        verify(counselorTemplateService).addNode(10L, 7L, null, null, 2, 3L);
    }

    @Test
    @DisplayName("FR8.2 · Thêm node bằng kỹ năng mới: skillId và parentId rỗng thành null")
    void addNode_kyNangMoi_thamSoRongThanhNull() throws Exception {
        when(counselorTemplateService.getTemplateById(10L)).thenReturn(TEMPLATE);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of());
        when(skillRepository.findAll()).thenReturn(List.of());
        when(skillRepository.findDistinctCategories()).thenReturn(List.of());

        mockMvc.perform(post("/counselor/templates/10/nodes")
                        .param("skillId", "   ")
                        .param("newSkillName", "Kubernetes")
                        .param("newSkillCategory", "DevOps")
                        .param("tier", "1")
                        .param("parentId", ""))
                .andExpect(status().isOk());

        // Chuỗi trắng phải thành null chứ không được ném NumberFormatException.
        verify(counselorTemplateService)
                .addNode(eq(10L), isNull(), eq("Kubernetes"), eq("DevOps"), eq(1), isNull());
    }

    @Test
    @DisplayName("FR8.2 · Không truyền skillId lẫn parentId thì vẫn thêm được node gốc")
    void addNode_khongTruyenThamSoTuyChon() throws Exception {
        when(counselorTemplateService.getTemplateById(10L)).thenReturn(TEMPLATE);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of());
        when(skillRepository.findAll()).thenReturn(List.of());
        when(skillRepository.findDistinctCategories()).thenReturn(List.of());

        mockMvc.perform(post("/counselor/templates/10/nodes")
                        .param("newSkillName", "Redis")
                        .param("tier", "1"))
                .andExpect(status().isOk());

        verify(counselorTemplateService)
                .addNode(eq(10L), isNull(), eq("Redis"), isNull(), eq(1), isNull());
    }

    @Test
    @DisplayName("FR8.2 · Xoá node trả về fragment cây đã cập nhật")
    void deleteNode_traVeFragmentCay() throws Exception {
        when(counselorTemplateService.getTemplateById(10L)).thenReturn(TEMPLATE);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of());
        when(skillRepository.findAll()).thenReturn(List.of());
        when(skillRepository.findDistinctCategories()).thenReturn(List.of());

        mockMvc.perform(delete("/counselor/templates/10/nodes/100"))
                .andExpect(status().isOk())
                .andExpect(view().name("counselor/editor :: editor-left-pane"))
                .andExpect(model().attribute("nodes", List.of()));

        verify(counselorTemplateService).deleteNode(100L);
    }

    // ================= FR8.4 — Sắp xếp thứ tự ưu tiên =================

    @Test
    @DisplayName("FR8.4 · Đổi tầng và node cha rồi quay lại trình soạn thảo")
    void updateNode_doiTangVaCha() throws Exception {
        mockMvc.perform(post("/counselor/templates/10/nodes/100/update")
                        .param("tier", "3")
                        .param("parentId", "50"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/counselor/templates/10/editor"));

        verify(counselorTemplateService).updateNode(100L, 3, 50L);
    }

    @Test
    @DisplayName("FR8.4 · Bỏ trống node cha thì node trở thành node gốc")
    void updateNode_boTrongCha_thanhNodeGoc() throws Exception {
        mockMvc.perform(post("/counselor/templates/10/nodes/100/update")
                        .param("tier", "1")
                        .param("parentId", "  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/counselor/templates/10/editor"));

        verify(counselorTemplateService).updateNode(eq(100L), eq(1), isNull());
    }

    // ================= FR8.3 — Gắn tài liệu học cho node =================

    @Test
    @DisplayName("FR8.3 · Xem chi tiết node nạp kèm danh sách tài liệu của node đó")
    void nodeDetails_napKemTaiLieu() throws Exception {
        SkillNode node = node(100L, 1L, "Java");
        LearningResource taiLieu = LearningResource.builder()
                .id(1L).skillNode(node).title("Spring Boot Guide")
                .url("https://spring.io").resourceType("DOCUMENTATION").build();

        when(counselorTemplateService.getNodeById(100L)).thenReturn(node);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of(node));
        when(resourceRepository.findDistinctResourceTypes()).thenReturn(List.of());
        when(resourceRepository.findBySkillNode_IdOrderByIdAsc(100L)).thenReturn(List.of(taiLieu));

        mockMvc.perform(get("/counselor/nodes/100/details"))
                .andExpect(status().isOk())
                .andExpect(view().name("counselor/editor :: node-details"))
                .andExpect(model().attribute("currentNode", node))
                // SkillNode không khai báo @OneToMany nên tài liệu phải nạp riêng —
                // trước đây template gọi thẳng currentNode.learningResources và ném lỗi 500.
                .andExpect(model().attribute("resources", List.of(taiLieu)));
    }

    @Test
    @DisplayName("FR8.3 · Thêm tài liệu xong trả lại fragment chi tiết đã cập nhật")
    void addResource_traLaiFragmentChiTiet() throws Exception {
        SkillNode node = node(100L, 1L, "Java");
        when(counselorTemplateService.getNodeById(100L)).thenReturn(node);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of(node));
        when(resourceRepository.findDistinctResourceTypes()).thenReturn(List.of());
        when(resourceRepository.findBySkillNode_IdOrderByIdAsc(100L)).thenReturn(List.of());

        mockMvc.perform(post("/counselor/nodes/100/resources")
                        .param("title", "Baeldung")
                        .param("url", "https://baeldung.com")
                        .param("resourceType", "ARTICLE")
                        .param("description", "Bài viết nền tảng"))
                .andExpect(status().isOk())
                .andExpect(view().name("counselor/editor :: node-details"));

        verify(counselorTemplateService).addResource(
                100L, "Baeldung", "https://baeldung.com", "ARTICLE", "Bài viết nền tảng");
    }

    @Test
    @DisplayName("FR8.3 · Mô tả tài liệu là tuỳ chọn, bỏ trống vẫn thêm được")
    void addResource_khongCoMoTa() throws Exception {
        SkillNode node = node(100L, 1L, "Java");
        when(counselorTemplateService.getNodeById(100L)).thenReturn(node);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of(node));
        when(resourceRepository.findDistinctResourceTypes()).thenReturn(List.of());
        when(resourceRepository.findBySkillNode_IdOrderByIdAsc(100L)).thenReturn(List.of());

        mockMvc.perform(post("/counselor/nodes/100/resources")
                        .param("title", "Video ôn tập")
                        .param("url", "https://youtube.com/watch")
                        .param("resourceType", "VIDEO"))
                .andExpect(status().isOk());

        verify(counselorTemplateService).addResource(
                eq(100L), eq("Video ôn tập"), eq("https://youtube.com/watch"), eq("VIDEO"), isNull());
    }

    @Test
    @DisplayName("FR8.3 · Xoá tài liệu xong trả lại fragment chi tiết")
    void deleteResource_traLaiFragmentChiTiet() throws Exception {
        SkillNode node = node(100L, 1L, "Java");
        when(counselorTemplateService.getNodeById(100L)).thenReturn(node);
        when(counselorTemplateService.getNodesByTemplateId(10L)).thenReturn(List.of(node));
        when(resourceRepository.findDistinctResourceTypes()).thenReturn(List.of());
        when(resourceRepository.findBySkillNode_IdOrderByIdAsc(100L)).thenReturn(List.of());

        mockMvc.perform(delete("/counselor/nodes/100/resources/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("counselor/editor :: node-details"));

        verify(counselorTemplateService).deleteResource(5L);
        verify(counselorTemplateService, never()).deleteNode(any());
    }
}
