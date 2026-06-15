package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.JobCrawlRun;

public interface JobSourcingAgentService {
    JobCrawlRun runTask(Long taskId);
}
