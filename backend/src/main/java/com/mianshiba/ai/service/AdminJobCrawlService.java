package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskCreateRequest;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskQueryRequest;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskUpdateRequest;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlItemVO;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlRunVO;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlTaskVO;

import java.util.List;

public interface AdminJobCrawlService {
    AdminJobCrawlTaskVO createTask(String authorizationHeader, AdminJobCrawlTaskCreateRequest request);
    AdminJobCrawlTaskVO updateTask(String authorizationHeader, Long id, AdminJobCrawlTaskUpdateRequest request);
    List<AdminJobCrawlTaskVO> listTasks(String authorizationHeader, AdminJobCrawlTaskQueryRequest request);
    AdminJobCrawlTaskVO getTask(String authorizationHeader, Long id);
    AdminJobCrawlTaskVO enableTask(String authorizationHeader, Long id);
    AdminJobCrawlTaskVO disableTask(String authorizationHeader, Long id);
    AdminJobCrawlRunVO runTask(String authorizationHeader, Long id);
    List<AdminJobCrawlRunVO> listTaskRuns(String authorizationHeader, Long taskId);
    List<AdminJobCrawlItemVO> listRunItems(String authorizationHeader, Long runId);
}
