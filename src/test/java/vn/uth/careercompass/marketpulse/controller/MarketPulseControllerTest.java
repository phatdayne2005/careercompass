package vn.uth.careercompass.marketpulse.controller;

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
import vn.uth.careercompass.config.OnboardingInterceptor;
import vn.uth.careercompass.config.WebMvcConfig;
import vn.uth.careercompass.kernel.service.MarkdownRenderer;
import vn.uth.careercompass.marketpulse.dto.JobTrendDTO;
import vn.uth.careercompass.marketpulse.dto.KeywordStatDTO;
import vn.uth.careercompass.marketpulse.dto.MarketPulseViewDTO;
import vn.uth.careercompass.marketpulse.service.MarketPulseService;
import vn.uth.careercompass.testsupport.CsrfTestAdvice;
import vn.uth.careercompass.testsupport.TestSecurityConfiguration;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(value = MarketPulseController.class, excludeAutoConfiguration = {
        org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration.class,
        org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration.class
}, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
        classes = {OnboardingInterceptor.class, WebMvcConfig.class}))
@AutoConfigureMockMvc
@Import({CsrfTestAdvice.class, TestSecurityConfiguration.class, MarkdownRenderer.class})
@WithMockUser(username = "student@uth.edu.vn", roles = "STUDENT")
class MarketPulseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarketPulseService marketPulseService;

    @Test
    void pulsePage_rendersSuccessfully() throws Exception {
        MarketPulseViewDTO pulseDTO = MarketPulseViewDTO.builder()
                .topKeywords(List.of(
                        KeywordStatDTO.builder().keyword("Java").count(50).build(),
                        KeywordStatDTO.builder().keyword("Spring Boot").count(35).build()
                ))
                .recentJobs(List.of(
                        JobTrendDTO.builder().id(1L).jobTitle("Java Dev").company("Tech Corp").source("ITViec").createdAt(LocalDateTime.now()).build()
                ))
                .totalJobsScraped(150)
                .build();

        when(marketPulseService.getMarketPulse()).thenReturn(pulseDTO);

        mockMvc.perform(get("/market/pulse"))
                .andExpect(status().isOk())
                .andExpect(view().name("market/pulse"))
                .andExpect(model().attributeExists("pulse"));
    }
}
