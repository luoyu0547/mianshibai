package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.resume.ResumeCreateRequest;
import com.mianshiba.ai.model.dto.resume.SectionCreateRequest;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResumeControllerTest {

    @Mock
    private ResumeService resumeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ResumeController(resumeService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createResumeReturnsVO() throws Exception {
        String auth = "Bearer token-value";
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setTitle("我的简历");
        ResumeVO vo = new ResumeVO();
        vo.setId(1L);
        vo.setTitle("我的简历");
        vo.setTemplateType("minimal_tech");
        vo.setStatus("draft");
        when(resumeService.createResume(eq(auth), any(ResumeCreateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/resume")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("我的简历"));
    }

    @Test
    void listResumesReturnsArray() throws Exception {
        String auth = "Bearer token-value";
        ResumeVO vo1 = new ResumeVO();
        vo1.setId(1L);
        vo1.setTitle("简历一");
        ResumeVO vo2 = new ResumeVO();
        vo2.setId(2L);
        vo2.setTitle("简历二");
        when(resumeService.listResumes(auth)).thenReturn(List.of(vo1, vo2));

        mockMvc.perform(get("/api/resume/list")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void getResumeDetailReturnsSections() throws Exception {
        String auth = "Bearer token-value";
        ResumeDetailVO detail = new ResumeDetailVO();
        detail.setId(1L);
        detail.setTitle("测试简历");
        SectionVO section = new SectionVO();
        section.setId(10L);
        section.setSectionType("education");
        detail.setSections(List.of(section));
        when(resumeService.getResumeDetail(auth, 1L)).thenReturn(detail);

        mockMvc.perform(get("/api/resume/1")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sections").isArray())
                .andExpect(jsonPath("$.data.sections[0].sectionType").value("education"));
    }

    @Test
    void addSectionReturnsSectionVO() throws Exception {
        String auth = "Bearer token-value";
        SectionCreateRequest request = new SectionCreateRequest();
        request.setSectionType("education");
        request.setSectionData(Map.of("school", "清华大学"));
        SectionVO vo = new SectionVO();
        vo.setId(10L);
        vo.setSectionType("education");
        when(resumeService.addSection(eq(auth), eq(1L), any(SectionCreateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/resume/1/section")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.sectionType").value("education"));
    }

    @Test
    void deleteResumeReturnsSuccess() throws Exception {
        String auth = "Bearer token-value";
        doNothing().when(resumeService).deleteResume(auth, 1L);

        mockMvc.perform(delete("/api/resume/1")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void createResumeReturnsErrorWhenTitleBlank() throws Exception {
        String auth = "Bearer token-value";
        ResumeCreateRequest request = new ResumeCreateRequest();
        request.setTitle("");

        mockMvc.perform(post("/api/resume")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }
}
