package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.JobAnalysis;

public interface AiJobAnalysisService {

    JobAnalysis analyzeJob(com.mianshiba.ai.model.entity.Job job);
}
