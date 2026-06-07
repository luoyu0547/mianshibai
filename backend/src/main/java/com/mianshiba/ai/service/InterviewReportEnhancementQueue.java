package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.InterviewReportEnhancement;

public interface InterviewReportEnhancementQueue {
    String STREAM_KEY = "interview.report.enhancement.stream";
    String GROUP_NAME = "interview-report-enhancement-workers";

    void publish(InterviewReportEnhancement enhancement);
}
