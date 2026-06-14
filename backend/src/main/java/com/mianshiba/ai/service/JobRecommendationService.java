package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.job.JobRecommendationRefineRequest;
import com.mianshiba.ai.model.vo.application.JobApplicationVO;
import com.mianshiba.ai.model.vo.job.JobRecommendationVO;

import java.util.List;

public interface JobRecommendationService {

    List<JobRecommendationVO> listRecommendations(String authorizationHeader);

    List<JobRecommendationVO> refine(String authorizationHeader, JobRecommendationRefineRequest request);

    void dismiss(String authorizationHeader, Long recommendationId);

    JobApplicationVO apply(String authorizationHeader, Long recommendationId);
}
