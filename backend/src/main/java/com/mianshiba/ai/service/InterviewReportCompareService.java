package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.interview.InterviewReportCompareVO;

public interface InterviewReportCompareService {
    InterviewReportCompareVO compare(String authorizationHeader, Long baseSessionId, Long targetSessionId);
}
