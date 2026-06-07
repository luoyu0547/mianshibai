package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.GlobalExceptionHandler;
import com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest;
import com.mianshiba.ai.model.dto.training.TrainingPlanGenerateRequest;
import com.mianshiba.ai.model.vo.training.TrainingAnswerVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;
import com.mianshiba.ai.service.TrainingService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingControllerTest {

    @Mock
    private TrainingService trainingService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TrainingController(trainingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Test
    void generatePlan_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        TrainingPlanGenerateRequest request = new TrainingPlanGenerateRequest();

        TrainingPlanVO vo = new TrainingPlanVO();
        vo.setId(1L);
        vo.setTitle("Java 后端八股强化");

        when(trainingService.generatePlan(eq(auth), any(TrainingPlanGenerateRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/training/plan/generate")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Java 后端八股强化"));
    }

    @Test
    void getActivePlan_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        TrainingPlanVO vo = new TrainingPlanVO();
        vo.setId(1L);
        vo.setTitle("Java 后端八股强化");

        when(trainingService.getActivePlan(auth)).thenReturn(vo);

        mockMvc.perform(get("/api/training/plan/active")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("Java 后端八股强化"));
    }

    @Test
    void listPlans_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        when(trainingService.listPlans(auth)).thenReturn(List.of());

        mockMvc.perform(get("/api/training/plan")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getPlan_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        TrainingPlanVO vo = new TrainingPlanVO();
        vo.setId(5L);
        vo.setTitle("算法冲刺");

        when(trainingService.getPlan(auth, 5L)).thenReturn(vo);

        mockMvc.perform(get("/api/training/plan/5")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    void archivePlan_returnsTrue() throws Exception {
        String auth = "Bearer test-token";
        when(trainingService.archivePlan(auth, 1L)).thenReturn(true);

        mockMvc.perform(put("/api/training/plan/1/archive")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void completePlan_returnsTrue() throws Exception {
        String auth = "Bearer test-token";
        when(trainingService.completePlan(auth, 1L)).thenReturn(true);

        mockMvc.perform(put("/api/training/plan/1/complete")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void getQuestion_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        TrainingQuestionVO vo = new TrainingQuestionVO();
        vo.setId(10L);
        vo.setTitle("HashMap 底层原理");

        when(trainingService.getQuestion(auth, 10L)).thenReturn(vo);

        mockMvc.perform(get("/api/training/question/10")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("HashMap 底层原理"));
    }

    @Test
    void markQuestionMastered_returnsTrue() throws Exception {
        String auth = "Bearer test-token";
        when(trainingService.markQuestionMastered(auth, 10L)).thenReturn(true);

        mockMvc.perform(put("/api/training/question/10/master")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void skipQuestion_returnsTrue() throws Exception {
        String auth = "Bearer test-token";
        when(trainingService.skipQuestion(auth, 10L)).thenReturn(true);

        mockMvc.perform(put("/api/training/question/10/skip")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void submitAnswer_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        TrainingAnswerSubmitRequest request = new TrainingAnswerSubmitRequest();

        TrainingAnswerVO vo = new TrainingAnswerVO();
        vo.setId(1L);

        when(trainingService.submitAnswer(eq(auth), eq(10L), any(TrainingAnswerSubmitRequest.class))).thenReturn(vo);

        mockMvc.perform(post("/api/training/question/10/answer")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void listQuestionAnswers_returnsSuccess() throws Exception {
        String auth = "Bearer test-token";
        when(trainingService.listQuestionAnswers(auth, 10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/training/question/10/answers")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void completeAlgorithm_returnsTrue() throws Exception {
        String auth = "Bearer test-token";
        when(trainingService.completeAlgorithmRecommendation(auth, 3L)).thenReturn(true);

        mockMvc.perform(put("/api/training/algorithm/3/complete")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void reopenAlgorithm_returnsTrue() throws Exception {
        String auth = "Bearer test-token";
        when(trainingService.reopenAlgorithmRecommendation(auth, 3L)).thenReturn(true);

        mockMvc.perform(put("/api/training/algorithm/3/reopen")
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }
}
