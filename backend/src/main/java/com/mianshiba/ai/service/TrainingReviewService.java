package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.training.TrainingMistakeQueryRequest;
import com.mianshiba.ai.model.vo.training.TrainingMasterySummaryVO;
import com.mianshiba.ai.model.vo.training.TrainingMasteryVO;
import com.mianshiba.ai.model.vo.training.TrainingMistakeVO;

import java.util.List;

public interface TrainingReviewService {
    List<TrainingMistakeVO> listMistakes(String authorizationHeader, TrainingMistakeQueryRequest request);
    List<TrainingMasteryVO> listTopicMastery(String authorizationHeader);
    List<TrainingMasteryVO> listSkillTagMastery(String authorizationHeader);
    TrainingMasterySummaryVO getMasterySummary(String authorizationHeader);
    Boolean rebuildMastery(String authorizationHeader);
    void refreshMasteryForQuestion(Long userId, Long questionId);
}
