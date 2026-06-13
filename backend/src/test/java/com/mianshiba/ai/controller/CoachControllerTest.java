package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.vo.coach.CoachGenerateResultVO;
import com.mianshiba.ai.model.vo.coach.CoachOverviewVO;
import com.mianshiba.ai.service.CoachService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoachController.class)
class CoachControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private CoachService coachService;

    @Test
    void generate_routesToService() throws Exception {
        when(coachService.generate(eq("Bearer test-token"), any(CoachGenerateRequest.class))).thenReturn(new CoachGenerateResultVO());

        mockMvc.perform(post("/api/coach/generate")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CoachGenerateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void overview_routesToService() throws Exception {
        when(coachService.getOverview("Bearer test-token")).thenReturn(new CoachOverviewVO());

        mockMvc.perform(get("/api/coach/overview").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listEndpoints_routeToService() throws Exception {
        when(coachService.listDiagnoses("Bearer test-token")).thenReturn(List.of());
        when(coachService.listPlans("Bearer test-token")).thenReturn(List.of());

        mockMvc.perform(get("/api/coach/diagnoses").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/coach/plans").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
    }

    @Test
    void detailAndTaskEndpoints_routeToService() throws Exception {
        mockMvc.perform(get("/api/coach/diagnoses/1").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/coach/plans/1").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mockMvc.perform(put("/api/coach/tasks/1/complete").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mockMvc.perform(put("/api/coach/tasks/1/reopen").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
    }
}
