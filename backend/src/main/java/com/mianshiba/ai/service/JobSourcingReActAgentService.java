package com.mianshiba.ai.service;

import com.mianshiba.ai.model.entity.JobCrawlRun;

/**
 * 职位采集 ReAct Agent 服务
 * 负责任务授权检查、职位发现、详情抓取、兜底解析与质量评分的完整采集流程。
 */
public interface JobSourcingReActAgentService {

    /**
     * 执行职位采集任务
     *
     * @param taskId 任务 ID
     * @return 采集运行记录
     */
    JobCrawlRun runTask(Long taskId);
}
