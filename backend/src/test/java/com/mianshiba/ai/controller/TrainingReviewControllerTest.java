package com.mianshiba.ai.controller;

import com.mianshiba.ai.model.vo.training.TrainingMistakeVO;
import com.mianshiba.ai.service.TrainingReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TrainingReviewController.class)
class TrainingReviewControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean TrainingReviewService trainingReviewService;

    @Test
    void listMistakes_returnsSuccessResponse() throws Exception {
        TrainingMistakeVO vo = new TrainingMistakeVO();
        vo.setQuestionId(1L);
        vo.setTitle("MySQL 索引");
        when(trainingReviewService.listMistakes(eq("Bearer test-token"), any())).thenReturn(List.of(vo));

        mockMvc.perform(get("/api/training/review/mistakes")
                .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].title").value("MySQL 索引"));
    }
}
