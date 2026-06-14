package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.JobCrawlRun;

public interface JobBatchCrawlService {

    JobCrawlRun runTask(Long taskId);
}
