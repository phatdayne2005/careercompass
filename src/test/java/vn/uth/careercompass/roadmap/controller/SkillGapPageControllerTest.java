package vn.uth.careercompass.roadmap.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.uth.careercompass.admin.entity.Skill;
import vn.uth.careercompass.admin.repository.SkillRepository;
import vn.uth.careercompass.kernel.entity.User;
import vn.uth.careercompass.kernel.service.AuthenticatedUserService;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.roadmap.dto.RoadmapTemplateDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapReportDTO;
import vn.uth.careercompass.roadmap.dto.SkillGapResultDTO;
import vn.uth.careercompass.roadmap.service.PdfService;
import vn.uth.careercompass.roadmap.service.RoadmapService;
import vn.uth.careercompass.roadmap.service.SkillGapService;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = SkillGapPageController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
class SkillGapPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthenticatedUserService authenticatedUserService;

    @MockitoBean
    private RoadmapService roadmapService;

    @MockitoBean
    private SkillGapService skillGapService;

    @MockitoBean
    private PdfService pdfService;

    @MockitoBean
    private SkillRepository skillRepository;

    @MockitoBean
    private MarkdownRenderer markdownRenderer;

    @TempDir
    Path tempDir;

    private final User userAccount = User.builder().email("student@uth.edu.vn").build();

    @Test
    void skillGapPage_withTemplateAndReportFlag_rendersSortedSkills() throws Exception {
        SkillGapResultDTO result = result();
        givenCommonPageData(result);
        Skill java = Skill.builder().id(1L).name("Java").build();
        Skill algorithm = Skill.builder().id(2L).name("Algorithm").build();
        when(skillRepository.findAll()).thenReturn(List.of(java, algorithm));

        mockMvc.perform(get("/skill-gap")
                        .param("templateId", "10")
                        .param("reportCreated", "true")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(view().name("skillgap/index"))
                .andExpect(model().attribute("result", result))
                .andExpect(model().attribute("reportCreated", true))
                .andExpect(model().attribute("allSkills", List.of(algorithm, java)));

        verify(skillGapService).analyze(userAccount, 10L);
    }

    @Test
    void skillGapPage_whenTemplateIsStoredInSession_withoutReportFlag_setsFalse() throws Exception {
        SkillGapResultDTO result = result();
        givenCommonPageData(result);
        when(skillRepository.findAll()).thenReturn(List.of());

                mockMvc.perform(get("/skill-gap")
                        .sessionAttr("selectedRoadmapTemplateId", 10L)
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("reportCreated", false));

        verify(skillGapService).analyze(userAccount, 10L);
    }

    @Test
    void addSkill_withTemplateId_redirectsWithTemplateQuery() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

        mockMvc.perform(post("/skill-gap/skills")
                        .param("skillId", "3")
                        .param("templateId", "10")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/skill-gap?templateId=10"));

        verify(skillGapService).addAcquiredSkill(userAccount, 3L, 10L);
    }

    @Test
    void addSkill_withoutTemplateId_redirectsWithoutQuery() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);

                mockMvc.perform(post("/skill-gap/skills")
                        .param("skillId", "3")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/skill-gap"));

        verify(skillGapService).addAcquiredSkill(userAccount, 3L, null);
    }

    @Test
    void createReport_returnsPdfAttachment() throws Exception {
        SkillGapResultDTO result = result();
        SkillGapReportDTO saved = SkillGapReportDTO.builder().id(99L).build();
        Path pdf = tempDir.resolve("skill-gap.pdf");
        Files.writeString(pdf, "pdf");
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);
        when(skillGapService.analyze(userAccount, 10L)).thenReturn(result);
        when(pdfService.generateSkillGapReport(userAccount, result)).thenReturn(pdf.toString());
        when(skillGapService.saveReport(userAccount, result, pdf.toString())).thenReturn(saved);

                mockMvc.perform(post("/skill-gap/reports")
                        .param("templateId", "10")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"skill-gap-report-99.pdf\""));
    }

    @Test
    void downloadReport_whenFileExists_returnsPdf() throws Exception {
        Path pdf = tempDir.resolve("existing-report.pdf");
        Files.writeString(pdf, "pdf");
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);
        when(skillGapService.getReport(userAccount, 7L)).thenReturn(
                SkillGapReportDTO.builder().id(7L).pdfPath(pdf.toString()).build());

                mockMvc.perform(get("/skill-gap/reports/7/download")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"skill-gap-report-7.pdf\""));
    }

    @Test
    void downloadReport_whenFileDoesNotExist_returnsNotFound() throws Exception {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);
        when(skillGapService.getReport(userAccount, 7L)).thenReturn(
                SkillGapReportDTO.builder().id(7L)
                        .pdfPath(tempDir.resolve("missing-report.pdf").toString())
                        .build());

                mockMvc.perform(get("/skill-gap/reports/7/download")
                        .with(user("student@uth.edu.vn").roles("STUDENT")))
                .andExpect(status().isNotFound());
    }

    private void givenCommonPageData(SkillGapResultDTO result) {
        when(authenticatedUserService.requireCurrentUser(any())).thenReturn(userAccount);
        when(skillGapService.analyze(userAccount, 10L)).thenReturn(result);
        when(roadmapService.getActiveTemplates()).thenReturn(List.of());
        when(skillGapService.getAcquiredSkills(userAccount)).thenReturn(List.of());
        when(skillGapService.getReports(userAccount)).thenReturn(List.of());
    }

    private SkillGapResultDTO result() {
        return SkillGapResultDTO.builder()
                .template(RoadmapTemplateDTO.builder().id(10L).name("Default").build())
                .requiredSkillCount(2)
                .matchedSkillCount(1)
                .missingSkillCount(1)
                .matchPercent(50.0)
                .matchedSkills(List.of())
                .missingSkills(List.of())
                .build();
    }
}
