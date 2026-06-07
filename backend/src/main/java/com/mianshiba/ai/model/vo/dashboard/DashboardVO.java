package com.mianshiba.ai.model.vo.dashboard;

import com.mianshiba.ai.model.vo.application.ApplicationStatsVO;
import com.mianshiba.ai.model.vo.training.AlgorithmRecommendationVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class DashboardVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<String> todayPriorities;
    private ApplicationStatsVO applicationStats;
    private TrainingPlanVO activePlan;
    private List<TrainingQuestionVO> pendingQuestions;
    private List<String> weakTopics;
    private List<AlgorithmRecommendationVO> algorithmRecommendations;
}
