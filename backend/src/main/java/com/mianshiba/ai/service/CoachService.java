package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.vo.coach.CoachDiagnosisVO;
import com.mianshiba.ai.model.vo.coach.CoachGenerateResultVO;
import com.mianshiba.ai.model.vo.coach.CoachOverviewVO;
import com.mianshiba.ai.model.vo.coach.CoachPlanVO;
import com.mianshiba.ai.model.vo.coach.CoachTaskVO;

import java.util.List;

public interface CoachService {

    CoachGenerateResultVO generate(String authorizationHeader, CoachGenerateRequest request);

    CoachOverviewVO getOverview(String authorizationHeader);

    List<CoachDiagnosisVO> listDiagnoses(String authorizationHeader);

    CoachDiagnosisVO getDiagnosis(String authorizationHeader, Long id);

    List<CoachPlanVO> listPlans(String authorizationHeader);

    CoachPlanVO getPlan(String authorizationHeader, Long id);

    CoachTaskVO completeTask(String authorizationHeader, Long id);

    CoachTaskVO reopenTask(String authorizationHeader, Long id);
}
