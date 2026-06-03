package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeAiService;
import com.mianshiba.ai.service.ResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResumeAiControllerTest {

    @Mock
    private ResumeAiService resumeAiService;

    @Mock
    private ResumeService resumeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ResumeAiController(resumeAiService, resumeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void generateResumeReturnsDetail() throws Exception {
        String auth = "Bearer token-value";
        AiGenerateRequest request = new AiGenerateRequest();
        request.setTargetPosition("Java开发工程师");

        ResumeDetailVO detail = new ResumeDetailVO();
        detail.setId(1L);
        detail.setTitle("Java开发工程师 - 简历");
        when(resumeAiService.generateResume(eq(auth), any(AiGenerateRequest.class))).thenReturn(detail);

        mockMvc.perform(post("/api/resume/ai/generate")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Java开发工程师 - 简历"));
    }

    @Test
    void optimizeSectionReturnsData() throws Exception {
        String auth = "Bearer token-value";
        AiOptimizeRequest request = new AiOptimizeRequest();
        request.setSectionId(10L);
        request.setSectionType("work");
        request.setSectionData(Map.of("company", "测试公司"));

        ResumeDetailVO detail = new ResumeDetailVO();
        SectionVO basicSection = new SectionVO();
        basicSection.setSectionType("basic");
        basicSection.setSectionData(Map.of("targetPosition", "Java开发"));
        detail.setSections(List.of(basicSection));

        when(resumeService.getResumeDetail(auth, 1L)).thenReturn(detail);
        when(resumeAiService.optimizeSection(any(AiOptimizeRequest.class), eq("Java开发")))
                .thenReturn(Map.of("company", "优化后公司", "highlights", List.of("亮点1")));

        mockMvc.perform(post("/api/resume/1/ai/optimize-section")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.company").value("优化后公司"));
    }

    @Test
    void scoreResumeReturnsScore() throws Exception {
        String auth = "Bearer token-value";
        ResumeDetailVO detail = new ResumeDetailVO();
        SectionVO basicSection = new SectionVO();
        basicSection.setSectionType("basic");
        basicSection.setSectionData(Map.of("targetPosition", "Java开发"));
        detail.setSections(List.of(basicSection));

        when(resumeService.getResumeDetail(auth, 1L)).thenReturn(detail);

        AiScoreVO scoreVO = new AiScoreVO();
        scoreVO.setScore(85);
        when(resumeAiService.scoreResume(any(), eq("Java开发"))).thenReturn(scoreVO);

        mockMvc.perform(post("/api/resume/1/ai/score")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.score").value(85));
    }

    @Test
    void generateReturnsErrorWhenTargetPositionBlank() throws Exception {
        String auth = "Bearer token-value";
        AiGenerateRequest request = new AiGenerateRequest();
        request.setTargetPosition("");

        mockMvc.perform(post("/api/resume/ai/generate")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }
}
