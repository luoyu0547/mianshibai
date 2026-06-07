package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.application.ApplicationCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoQueryRequest;
import com.mianshiba.ai.model.vo.application.ApplicationTodoVO;
import com.mianshiba.ai.model.vo.application.JobApplicationVO;
import com.mianshiba.ai.service.ApplicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ApplicationController(applicationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void create_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        ApplicationCreateRequest request = new ApplicationCreateRequest();
        request.setCompanyName("TestCompany");
        request.setJobTitle("Dev");

        JobApplicationVO vo = new JobApplicationVO();
        vo.setId(1L);
        vo.setCompanyName("TestCompany");
        vo.setJobTitle("Dev");
        vo.setStatus("pending_submit");

        when(applicationService.createApplication(eq(auth), any(ApplicationCreateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/application")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void listTodos_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        when(applicationService.listTodos(eq(auth), any(ApplicationTodoQueryRequest.class)))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/application/todo")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void completeTodo_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        ApplicationTodoVO todoVO = new ApplicationTodoVO();
        todoVO.setId(1L);
        todoVO.setCompleted(true);
        todoVO.setCompletedAt(LocalDateTime.now());

        when(applicationService.completeTodo(eq(auth), eq(1L))).thenReturn(todoVO);

        mockMvc.perform(put("/api/application/todo/1/complete")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.completed").value(true));
    }
}