package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.jobsourcing.ExtractedJobCard;
import com.mianshiba.ai.model.dto.jobsourcing.FetchedJobPage;
import com.mianshiba.ai.model.dto.jobsourcing.JobDiscoveryRequest;
import com.mianshiba.ai.model.dto.jobsourcing.JobListEntry;

import java.util.List;

/**
 * 职位平台适配器
 * 定义不同招聘平台的统一采集契约：发现列表、抓取详情、兜底解析、授权检查。
 */
public interface JobPlatformAdapter {

    /**
     * 返回平台标识
     *
     * @return 平台标识，如 BOSS
     */
    String platform();

    /**
     * 是否需要登录授权
     *
     * @return true 表示需要授权
     */
    boolean requiresAuth();

    /**
     * 检查当前授权状态
     *
     * @return 授权检查结果
     */
    BrowserSessionService.AuthCheckResult checkAuth();

    /**
     * 发现职位列表
     *
     * @param request 发现请求
     * @return 职位列表条目
     */
    List<JobListEntry> discover(JobDiscoveryRequest request);

    /**
     * 抓取职位详情页
     *
     * @param url 详情页 URL
     * @return 抓取的页面信息
     */
    FetchedJobPage fetchDetail(String url);

    /**
     * 兜底解析职位卡片
     *
     * @param page 抓取的页面
     * @return 提取的职位卡片
     */
    ExtractedJobCard fallbackExtract(FetchedJobPage page);
}
