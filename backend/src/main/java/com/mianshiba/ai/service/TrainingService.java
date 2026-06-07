package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest;
import com.mianshiba.ai.model.dto.training.TrainingPlanGenerateRequest;
import com.mianshiba.ai.model.vo.training.TrainingAnswerVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;

import java.util.List;

/**
 * 八股训练服务接口
 */
public interface TrainingService {

    TrainingPlanVO generatePlan(String authorizationHeader, TrainingPlanGenerateRequest request);

    TrainingPlanVO getActivePlan(String authorizationHeader);

    List<TrainingPlanVO> listPlans(String authorizationHeader);

    TrainingPlanVO getPlan(String authorizationHeader, Long id);

    Boolean archivePlan(String authorizationHeader, Long id);

    Boolean completePlan(String authorizationHeader, Long id);

    TrainingQuestionVO getQuestion(String authorizationHeader, Long id);

    Boolean markQuestionMastered(String authorizationHeader, Long id);

    Boolean skipQuestion(String authorizationHeader, Long id);

    TrainingAnswerVO submitAnswer(String authorizationHeader, Long questionId, TrainingAnswerSubmitRequest request);

    List<TrainingAnswerVO> listQuestionAnswers(String authorizationHeader, Long questionId);

    Boolean completeAlgorithmRecommendation(String authorizationHeader, Long id);

    Boolean reopenAlgorithmRecommendation(String authorizationHeader, Long id);
}
