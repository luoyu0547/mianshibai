package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.JobMatch;

public interface ResumeJobMatchService {

    JobMatch match(Long userId, Long resumeId, Long jobId);
}
