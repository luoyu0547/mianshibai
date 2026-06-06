package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.interview.InterviewAnswerRequest;
import com.mianshiba.ai.model.dto.interview.InterviewCreateRequest;
import com.mianshiba.ai.model.vo.interview.InterviewAnswerResultVO;
import com.mianshiba.ai.model.vo.interview.InterviewQuestionVO;
import com.mianshiba.ai.model.vo.interview.InterviewReportVO;
import com.mianshiba.ai.model.vo.interview.InterviewSessionVO;

import java.util.List;

public interface InterviewService {
    InterviewSessionVO createSession(String authorizationHeader, InterviewCreateRequest request);
    InterviewQuestionVO startSession(String authorizationHeader, Long sessionId);
    InterviewAnswerResultVO submitAnswer(String authorizationHeader, Long sessionId, Long turnId, InterviewAnswerRequest request);
    InterviewSessionVO getSession(String authorizationHeader, Long sessionId);
    List<InterviewSessionVO> listSessions(String authorizationHeader);
    InterviewReportVO getReport(String authorizationHeader, Long sessionId);
    void cancelSession(String authorizationHeader, Long sessionId);
}
