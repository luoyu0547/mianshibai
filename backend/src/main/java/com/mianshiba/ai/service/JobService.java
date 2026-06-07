package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.job.JobImportRequest;
import com.mianshiba.ai.model.dto.job.JobListQueryRequest;
import com.mianshiba.ai.model.dto.job.JobMatchRequest;
import com.mianshiba.ai.model.vo.job.CompanyVO;
import com.mianshiba.ai.model.vo.job.JobGapAnalysisVO;
import com.mianshiba.ai.model.vo.job.JobImportResultVO;
import com.mianshiba.ai.model.vo.job.JobKeywordVO;
import com.mianshiba.ai.model.vo.job.JobMatchVO;
import com.mianshiba.ai.model.vo.job.JobQuestionPredictionVO;
import com.mianshiba.ai.model.vo.job.JobVO;

import java.util.List;

public interface JobService {

    JobImportResultVO importUrl(String authorizationHeader, JobImportRequest request);

    JobVO getJob(String authorizationHeader, Long jobId);

    CompanyVO getCompany(String authorizationHeader, Long companyId);

    JobMatchVO matchJob(String authorizationHeader, Long jobId, JobMatchRequest request);

    void favoriteJob(String authorizationHeader, Long jobId);

    void unfavoriteJob(String authorizationHeader, Long jobId);

    List<JobVO> listFavorites(String authorizationHeader);

    List<JobVO> listJobs(String authorizationHeader, JobListQueryRequest request);

    JobKeywordVO extractKeywords(String authorizationHeader, Long jobId);

    JobGapAnalysisVO analyzeGap(String authorizationHeader, Long jobId, Long resumeId);

    JobQuestionPredictionVO predictQuestions(String authorizationHeader, Long jobId);
}
