package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.interview.InterviewCreateRequest;
import com.mianshiba.ai.model.vo.interview.InterviewSessionVO;
import com.mianshiba.ai.service.InterviewService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class InterviewControllerTest {

    @Mock
    private InterviewService interviewService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new InterviewController(interviewService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void createSession_returnsSuccess() throws Exception {
        String auth = "Bearer token-value";
        InterviewCreateRequest request = new InterviewCreateRequest();
        request.setResumeId(1L);
        request.setTargetPosition("Java开发工程师");
        InterviewSessionVO vo = new InterviewSessionVO();
        vo.setId(1L);
        vo.setResumeId(1L);
        vo.setTargetPosition("Java开发工程师");
        vo.setStatus("pending");
        when(interviewService.createSession(eq(auth), any(InterviewCreateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/interview/session")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.targetPosition").value("Java开发工程师"));
    }

    @Test
    void createSession_missingResumeId_returnsBadRequest() throws Exception {
        String auth = "Bearer token-value";
        InterviewCreateRequest request = new InterviewCreateRequest();
        request.setTargetPosition("Java开发工程师");

        mockMvc.perform(post("/api/interview/session")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void listSessions_returnsList() throws Exception {
        String auth = "Bearer token-value";
        InterviewSessionVO vo1 = new InterviewSessionVO();
        vo1.setId(1L);
        vo1.setTargetPosition("Java开发工程师");
        InterviewSessionVO vo2 = new InterviewSessionVO();
        vo2.setId(2L);
        vo2.setTargetPosition("前端开发工程师");
        when(interviewService.listSessions(auth)).thenReturn(List.of(vo1, vo2));

        mockMvc.perform(get("/api/interview/session/list")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void cancelSession_returnsSuccess() throws Exception {
        String auth = "Bearer token-value";
        doNothing().when(interviewService).cancelSession(auth, 1L);

        mockMvc.perform(post("/api/interview/session/1/cancel")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }
}
