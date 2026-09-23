package vn.uth.careercompass.onboarding.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.admin.entity.CareerRole;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.repository.CareerRoleRepository;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.kernel.service.UserProfileService;
import vn.uth.careercompass.onboarding.service.OnboardingService;
import vn.uth.careercompass.onboarding.service.TranscriptAnalysisService;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiểm thử ba bước khai báo hồ sơ (FR2.1, FR1.3, FR3.1 và quy tắc dẫn xuất FR6.3).
 *
 * Trước lớp này OnboardingController phủ 67% dòng và 57% nhánh. Phần chưa phủ đều là các
 * nhánh "đường vòng" mà người dùng thật vẫn đi vào:
 *
 *   - getIncompleteUser trả null ở CẢ SÁU endpoint khi người dùng đã hoàn tất onboarding.
 *     Đây là hàng rào chống quay lại wizard; hỏng thì người dùng làm lại từ đầu và ghi đè
 *     dữ liệu cũ.
 *   - Ba tham số tuỳ chọn ở bước 2 và 3 (tệp bảng điểm, tên GitHub, danh sách kỹ năng) đều
 *     có ba trạng thái: không gửi, gửi rỗng, gửi thật.
 *   - groupSkillsByCategory phải giữ đúng thứ tự mười nhóm đã quy định, đẩy nhóm lạ xuống
 *     cuối và gom kỹ năng không có nhóm vào "Khác" — không được bỏ sót kỹ năng nào.
 */
@WebMvcTest(value = OnboardingController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
@DisplayName("FR2.1 / FR1.3 / FR3.1 — Ba bước khai báo hồ sơ")
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private OnboardingService onboardingService;

    @MockitoBean
    private TranscriptAnalysisService transcriptAnalysisService;

    @MockitoBean
    private CareerRoleRepository careerRoleRepository;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    private static User nguoiDung(boolean daHoanTat) {
        return User.builder().id(1L).email("student@uth.edu.vn")
                .onboardingCompleted(daHoanTat).build();
    }

    private void dangLamDoDang() {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(nguoiDung(false));
    }

    private void daHoanTatRoi() {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(nguoiDung(true));
    }

    // ================= FR2.1 — Bước 1: chọn vai trò mục tiêu =================

    @Test
    @DisplayName("AC-2.1.1 · Bước 1 hiển thị danh sách vai trò nghề nghiệp")
    void step1_hienDanhSachVaiTro() throws Exception {
        dangLamDoDang();
        when(careerRoleRepository.findAll()).thenReturn(List.of(
                CareerRole.builder().id(1L).name("Backend Developer").build(),
                CareerRole.builder().id(2L).name("Frontend Developer").build()));

        mockMvc.perform(get("/onboarding/step1"))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding/step1_role"))
                .andExpect(model().attribute("currentStep", 1))
                .andExpect(model().attribute("roles", org.hamcrest.Matchers.hasSize(2)));
    }

    @Test
    @DisplayName("AC-2.1.2 · Không chọn vai trò và không bỏ qua thì quay lại kèm thông báo")
    void step1Submit_khongChonVaKhongBoQua_quayLaiKemLoi() throws Exception {
        dangLamDoDang();

        mockMvc.perform(post("/onboarding/step1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step1"))
                .andExpect(flash().attribute("error", "Vui lòng chọn một vị trí nghề nghiệp."));

        verify(userProfileService, never()).setTargetRole(any(), anyLong());
    }

    @Test
    @DisplayName("AC-2.1.2 · Chọn vai trò hợp lệ thì lưu và sang bước 2")
    void step1Submit_chonVaiTro_luuVaSangBuoc2() throws Exception {
        dangLamDoDang();

        mockMvc.perform(post("/onboarding/step1").param("roleId", "1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step2"));

        verify(userProfileService).setTargetRole(any(User.class), org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    @DisplayName("FR2.1 · Bấm bỏ qua thì sang bước 2 mà không gán vai trò")
    void step1Submit_boQua_khongGanVaiTro() throws Exception {
        dangLamDoDang();

        mockMvc.perform(post("/onboarding/step1").param("skip", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step2"));

        verify(userProfileService, never()).setTargetRole(any(), anyLong());
    }

    @Test
    @DisplayName("FR2.1 · Bỏ qua được ưu tiên hơn roleId nếu người dùng gửi cả hai")
    void step1Submit_boQuaThangRoleId() throws Exception {
        dangLamDoDang();

        mockMvc.perform(post("/onboarding/step1").param("skip", "true").param("roleId", "1"))
                .andExpect(redirectedUrl("/onboarding/step2"));

        verify(userProfileService, never()).setTargetRole(any(), anyLong());
    }

    // ================= FR1.3 — Bước 2: bảng điểm và GitHub =================

    @Test
    @DisplayName("FR1.3 · Bước 2 hiện trạng thái tệp bảng điểm đã có hay chưa")
    void step2_hienTrangThaiTep() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(
                User.builder().id(1L).email("student@uth.edu.vn").onboardingCompleted(false)
                        .githubUsername("phatdayne").transcriptPath("uploads/a.pdf").build());

        mockMvc.perform(get("/onboarding/step2"))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding/step2_sources"))
                .andExpect(model().attribute("currentStep", 2))
                .andExpect(model().attribute("githubUsername", "phatdayne"))
                .andExpect(model().attribute("hasTranscript", true));
    }

    @Test
    @DisplayName("FR1.3 · Chưa tải bảng điểm thì cờ hasTranscript là false")
    void step2_chuaCoTep_hasTranscriptFalse() throws Exception {
        dangLamDoDang();

        mockMvc.perform(get("/onboarding/step2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("hasTranscript", false))
                .andExpect(model().attribute("githubUsername", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    @DisplayName("AC-1.3.1 · Tải bảng điểm hợp lệ thì lưu tệp, lưu tóm tắt và sang bước 3")
    void step2Submit_tepHopLe_luuVaSangBuoc3() throws Exception {
        dangLamDoDang();
        when(onboardingService.saveTranscript(any(), any())).thenReturn("uploads/bangdiem.pdf");
        when(transcriptAnalysisService.analyze(any())).thenReturn("GPA 3.2, mạnh về Java");

        mockMvc.perform(multipart("/onboarding/step2")
                        .file(new MockMultipartFile("transcriptFile", "bangdiem.pdf",
                                "application/pdf", "noi dung".getBytes())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step3"));

        verify(userProfileService).storeTranscript(any(User.class),
                org.mockito.ArgumentMatchers.eq("uploads/bangdiem.pdf"), org.mockito.ArgumentMatchers.isNull());
        verify(userProfileService).storeTranscriptSummary(any(User.class), org.mockito.ArgumentMatchers.eq("GPA 3.2, mạnh về Java"));
    }

    @Test
    @DisplayName("AC-1.3.2 · AI không đọc được bảng điểm thì vẫn sang bước 3 (không chặn luồng)")
    void step2Submit_aiKhongDocDuoc_vanSangBuoc3() throws Exception {
        dangLamDoDang();
        when(onboardingService.saveTranscript(any(), any())).thenReturn("uploads/bangdiem.pdf");
        // analyze trả null khi không trích được nội dung — đây là nhánh "tăng cường, không chặn".
        when(transcriptAnalysisService.analyze(any())).thenReturn(null);

        mockMvc.perform(multipart("/onboarding/step2")
                        .file(new MockMultipartFile("transcriptFile", "bangdiem.pdf",
                                "application/pdf", "noi dung".getBytes())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step3"));

        verify(userProfileService).storeTranscript(any(), anyString(), any());
        verify(userProfileService, never()).storeTranscriptSummary(any(), anyString());
    }

    @Test
    @DisplayName("AC-1.3.3 · Tệp bị từ chối thì quay lại bước 2 kèm thông báo, không lỗi 500")
    void step2Submit_tepBiTuChoi_quayLaiKemLoi() throws Exception {
        dangLamDoDang();
        when(onboardingService.saveTranscript(any(), any()))
                .thenThrow(new IllegalArgumentException("File vượt quá dung lượng tối đa 10MB."));

        mockMvc.perform(multipart("/onboarding/step2")
                        .file(new MockMultipartFile("transcriptFile", "to.pdf",
                                "application/pdf", "x".getBytes())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step2"))
                .andExpect(flash().attribute("error",
                        "Lỗi khi tải lên bảng điểm: File vượt quá dung lượng tối đa 10MB."));

        verify(userProfileService, never()).storeTranscript(any(), anyString(), any());
    }

    @Test
    @DisplayName("FR1.3 · Không gửi tệp nào thì bỏ qua khối xử lý tệp")
    void step2Submit_khongGuiTep_boQuaKhoiXuLyTep() throws Exception {
        dangLamDoDang();

        mockMvc.perform(post("/onboarding/step2").param("githubUsername", "  phatdayne  "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step3"));

        verify(onboardingService, never()).saveTranscript(any(), any());
        // Tên GitHub được cắt khoảng trắng trước khi lưu.
        verify(userProfileService).setGithub(any(User.class), org.mockito.ArgumentMatchers.eq("phatdayne"));
    }

    @Test
    @DisplayName("FR1.3 · Gửi tệp rỗng cũng bị coi như không gửi")
    void step2Submit_tepRong_coiNhuKhongGui() throws Exception {
        dangLamDoDang();

        mockMvc.perform(multipart("/onboarding/step2")
                        .file(new MockMultipartFile("transcriptFile", "", "application/pdf", new byte[0])))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step3"));

        verify(onboardingService, never()).saveTranscript(any(), any());
    }

    @Test
    @DisplayName("FR1.3 · Tên GitHub để trắng thì không ghi đè giá trị cũ")
    void step2Submit_githubDeTrang_khongGhiDe() throws Exception {
        dangLamDoDang();

        mockMvc.perform(post("/onboarding/step2").param("githubUsername", "    "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/onboarding/step3"));

        verify(userProfileService, never()).setGithub(any(), anyString());
    }

    // ================= FR3.1 — Bước 3: chọn kỹ năng hiện có =================

    @Test
    @DisplayName("FR3.1 · Bước 3 nhóm kỹ năng theo đúng thứ tự đã quy định")
    void step3_nhomKyNangDungThuTu() throws Exception {
        dangLamDoDang();
        when(onboardingService.getUserSkillIds(any())).thenReturn(List.of(1L, 2L));
        when(skillRepository.findAll()).thenReturn(List.of(
                Skill.builder().id(1L).name("Spring").category("Backend").build(),
                Skill.builder().id(2L).name("React").category("Frontend").build(),
                Skill.builder().id(3L).name("Java").category("Ngôn ngữ").build(),
                // Nhóm không nằm trong danh sách thứ tự -> phải xếp CUỐI, không được mất.
                Skill.builder().id(4L).name("Figma").category("Thiết kế").build(),
                // Kỹ năng không có nhóm -> gom vào "Khác".
                Skill.builder().id(5L).name("Kỹ năng lạ").category(null).build()));

        var ketQua = mockMvc.perform(get("/onboarding/step3"))
                .andExpect(status().isOk())
                .andExpect(view().name("onboarding/step3_skills"))
                .andExpect(model().attribute("currentStep", 3))
                .andExpect(model().attribute("existingSkillIds", List.of(1L, 2L)))
                .andReturn();

        @SuppressWarnings("unchecked")
        Map<String, List<Skill>> nhom = (Map<String, List<Skill>>) ketQua.getModelAndView()
                .getModel().get("skillsByCategory");

        assertThat(nhom.keySet())
                .as("ba nhóm quen thuộc theo đúng thứ tự quy định, nhóm lạ và Khác xếp sau")
                .containsExactly("Ngôn ngữ", "Frontend", "Backend", "Thiết kế", "Khác");
        assertThat(nhom.values().stream().mapToInt(List::size).sum())
                .as("không kỹ năng nào bị bỏ sót khi nhóm lại")
                .isEqualTo(5);
    }

    @Test
    @DisplayName("FR3.1 · Kỹ năng trong cùng nhóm được sắp xếp theo tên")
    void step3_kyNangTrongNhomSapTheoTen() throws Exception {
        dangLamDoDang();
        when(onboardingService.getUserSkillIds(any())).thenReturn(List.of());
        when(skillRepository.findAll()).thenReturn(List.of(
                Skill.builder().id(1L).name("Spring").category("Backend").build(),
                Skill.builder().id(2L).name("Django").category("Backend").build(),
                Skill.builder().id(3L).name("Express").category("Backend").build()));

        var ketQua = mockMvc.perform(get("/onboarding/step3")).andReturn();

        @SuppressWarnings("unchecked")
        Map<String, List<Skill>> nhom = (Map<String, List<Skill>>) ketQua.getModelAndView()
                .getModel().get("skillsByCategory");

        assertThat(nhom.get("Backend")).extracting(Skill::getName)
                .containsExactly("Django", "Express", "Spring");
    }

    @Test
    @DisplayName("FR3.1 · Chọn kỹ năng rồi hoàn tất thì lưu kỹ năng và bật cờ hoàn thành")
    void step3Submit_chonKyNang_luuVaHoanTat() throws Exception {
        dangLamDoDang();

        mockMvc.perform(post("/onboarding/step3")
                        .param("skillIds", "1", "2", "3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(userProfileService).replaceSkills(any(User.class),
                org.mockito.ArgumentMatchers.eq(List.of(1L, 2L, 3L)));
        verify(userProfileService).completeOnboarding(any(User.class));
    }

    @Test
    @DisplayName("FR3.1 · Không chọn kỹ năng nào vẫn hoàn tất được onboarding")
    void step3Submit_khongChonKyNang_vanHoanTat() throws Exception {
        dangLamDoDang();

        mockMvc.perform(post("/onboarding/step3"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // Không gọi replaceSkills để khỏi xoá trắng kỹ năng đã có.
        verify(userProfileService, never()).replaceSkills(any(), anyList());
        verify(userProfileService).completeOnboarding(any(User.class));
    }

    // ============ FR6.3 — Hàng rào chống quay lại wizard ============

    @Test
    @DisplayName("AC-2.1.3 · Đã hoàn tất rồi thì GET bước 1 bị đưa về trang chủ")
    void step1_daHoanTat_veTrangChu() throws Exception {
        daHoanTatRoi();

        mockMvc.perform(get("/onboarding/step1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(careerRoleRepository, never()).findAll();
    }

    @Test
    @DisplayName("AC-2.1.3 · Đã hoàn tất rồi thì POST bước 1 không ghi đè vai trò")
    void step1Submit_daHoanTat_khongGhiDe() throws Exception {
        daHoanTatRoi();

        mockMvc.perform(post("/onboarding/step1").param("roleId", "2"))
                .andExpect(redirectedUrl("/"));

        verify(userProfileService, never()).setTargetRole(any(), anyLong());
    }

    @Test
    @DisplayName("AC-2.1.3 · Đã hoàn tất rồi thì GET bước 2 bị đưa về trang chủ")
    void step2_daHoanTat_veTrangChu() throws Exception {
        daHoanTatRoi();

        mockMvc.perform(get("/onboarding/step2")).andExpect(redirectedUrl("/"));
    }

    @Test
    @DisplayName("AC-2.1.3 · Đã hoàn tất rồi thì POST bước 2 không ghi đè bảng điểm")
    void step2Submit_daHoanTat_khongGhiDe() throws Exception {
        daHoanTatRoi();

        mockMvc.perform(multipart("/onboarding/step2")
                        .file(new MockMultipartFile("transcriptFile", "x.pdf",
                                "application/pdf", "x".getBytes())))
                .andExpect(redirectedUrl("/"));

        verify(onboardingService, never()).saveTranscript(any(), any());
    }

    @Test
    @DisplayName("AC-2.1.3 · Đã hoàn tất rồi thì GET bước 3 bị đưa về trang chủ")
    void step3_daHoanTat_veTrangChu() throws Exception {
        daHoanTatRoi();

        mockMvc.perform(get("/onboarding/step3")).andExpect(redirectedUrl("/"));

        verify(skillRepository, never()).findAll();
    }

    @Test
    @DisplayName("AC-2.1.3 · Đã hoàn tất rồi thì POST bước 3 không ghi đè kỹ năng")
    void step3Submit_daHoanTat_khongGhiDe() throws Exception {
        daHoanTatRoi();

        mockMvc.perform(post("/onboarding/step3").param("skillIds", "9"))
                .andExpect(redirectedUrl("/"));

        verify(userProfileService, never()).replaceSkills(any(), anyList());
        verify(userProfileService, never()).completeOnboarding(any());
    }
}
