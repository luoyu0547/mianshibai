package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.InterviewReport;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.vo.interview.InterviewReportEnhancementVO;

public interface InterviewReportEnhancementService {
    void createTaskIfAbsent(InterviewSession session, InterviewReport report);

    InterviewReportEnhancementVO getEnhancement(String authorizationHeader, Long sessionId);

    InterviewReportEnhancementVO retry(String authorizationHeader, Long sessionId);

    void runTask(Long enhancementId);
}
