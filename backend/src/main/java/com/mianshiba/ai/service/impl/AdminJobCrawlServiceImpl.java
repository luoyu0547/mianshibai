package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.JobCrawlItemMapper;
import com.mianshiba.ai.mapper.JobCrawlRunMapper;
import com.mianshiba.ai.mapper.JobCrawlTaskMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskCreateRequest;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskQueryRequest;
import com.mianshiba.ai.model.dto.admin.jobcrawl.AdminJobCrawlTaskUpdateRequest;
import com.mianshiba.ai.model.entity.JobCrawlItem;
import com.mianshiba.ai.model.entity.JobCrawlRun;
import com.mianshiba.ai.model.entity.JobCrawlTask;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlItemVO;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlRunVO;
import com.mianshiba.ai.model.vo.admin.jobcrawl.AdminJobCrawlTaskVO;
import com.mianshiba.ai.service.AdminJobCrawlService;
import com.mianshiba.ai.service.BrowserSessionService;
import com.mianshiba.ai.service.JobBatchCrawlService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminJobCrawlServiceImpl implements AdminJobCrawlService {

    private static final Set<String> VALID_SOURCE_TYPES = Set.of(
            "company_career_page", "public_feed", "manual_url_list", "platform_entry_url");
    private static final Set<String> VALID_SCHEDULE_TYPES = Set.of(
            "manual", "daily", "weekly", "cron");
    private static final String STATUS_DISABLED = "disabled";
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_RUNNING = "running";

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final JobCrawlTaskMapper jobCrawlTaskMapper;
    private final JobCrawlRunMapper jobCrawlRunMapper;
    private final JobCrawlItemMapper jobCrawlItemMapper;
    private final JobBatchCrawlService jobBatchCrawlService;
    private final BrowserSessionService browserSessionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminJobCrawlTaskVO createTask(String authorizationHeader, AdminJobCrawlTaskCreateRequest request) {
        Long adminId = resolveAdminId(authorizationHeader);

        if (!VALID_SOURCE_TYPES.contains(request.getSourceType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "来源类型无效，有效值: " + String.join(", ", VALID_SOURCE_TYPES));
        }

        if (StringUtils.hasText(request.getScheduleType()) && !VALID_SCHEDULE_TYPES.contains(request.getScheduleType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "调度类型无效，有效值: " + String.join(", ", VALID_SCHEDULE_TYPES));
        }

        JobCrawlTask task = new JobCrawlTask();
        task.setName(request.getName());
        task.setSourceType(request.getSourceType());
        task.setSourceUrl(request.getSourceUrl());
        task.setConfigJson(request.getConfigJson());
        task.setKeywords(request.getKeywords());
        task.setCities(request.getCities());
        task.setExperienceLevels(request.getExperienceLevels());
        task.setScheduleType(StringUtils.hasText(request.getScheduleType()) ? request.getScheduleType() : "manual");
        task.setCronExpression(request.getCronExpression());
        task.setRemark(request.getRemark());
        task.setStatus(STATUS_DISABLED);
        task.setCreatedBy(adminId);

        jobCrawlTaskMapper.insert(task);
        return toTaskVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminJobCrawlTaskVO updateTask(String authorizationHeader, Long id, AdminJobCrawlTaskUpdateRequest request) {
        resolveAdminId(authorizationHeader);

        JobCrawlTask task = getExistingTask(id);

        if (request.getName() != null) {
            task.setName(request.getName());
        }
        if (request.getSourceType() != null) {
            if (!VALID_SOURCE_TYPES.contains(request.getSourceType())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "来源类型无效，有效值: " + String.join(", ", VALID_SOURCE_TYPES));
            }
            task.setSourceType(request.getSourceType());
        }
        if (request.getSourceUrl() != null) {
            task.setSourceUrl(request.getSourceUrl());
        }
        if (request.getConfigJson() != null) {
            task.setConfigJson(request.getConfigJson());
        }
        if (request.getKeywords() != null) {
            task.setKeywords(request.getKeywords());
        }
        if (request.getCities() != null) {
            task.setCities(request.getCities());
        }
        if (request.getExperienceLevels() != null) {
            task.setExperienceLevels(request.getExperienceLevels());
        }
        if (request.getScheduleType() != null) {
            if (!VALID_SCHEDULE_TYPES.contains(request.getScheduleType())) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,
                        "调度类型无效，有效值: " + String.join(", ", VALID_SCHEDULE_TYPES));
            }
            task.setScheduleType(request.getScheduleType());
        }
        if (request.getCronExpression() != null) {
            task.setCronExpression(request.getCronExpression());
        }
        if (request.getRemark() != null) {
            task.setRemark(request.getRemark());
        }

        jobCrawlTaskMapper.updateById(task);
        return toTaskVO(task);
    }

    @Override
    public List<AdminJobCrawlTaskVO> listTasks(String authorizationHeader, AdminJobCrawlTaskQueryRequest request) {
        resolveAdminId(authorizationHeader);

        AdminJobCrawlTaskQueryRequest query = request == null ? new AdminJobCrawlTaskQueryRequest() : request;
        LambdaQueryWrapper<JobCrawlTask> wrapper = Wrappers.lambdaQuery(JobCrawlTask.class);
        if (StringUtils.hasText(query.getName())) {
            wrapper.like(JobCrawlTask::getName, query.getName());
        }
        if (StringUtils.hasText(query.getSourceType())) {
            wrapper.eq(JobCrawlTask::getSourceType, query.getSourceType());
        }
        if (StringUtils.hasText(query.getStatus())) {
            wrapper.eq(JobCrawlTask::getStatus, query.getStatus());
        }
        if (StringUtils.hasText(query.getScheduleType())) {
            wrapper.eq(JobCrawlTask::getScheduleType, query.getScheduleType());
        }
        wrapper.orderByDesc(JobCrawlTask::getCreateTime);

        return jobCrawlTaskMapper.selectList(wrapper).stream().map(this::toTaskVO).toList();
    }

    @Override
    public AdminJobCrawlTaskVO getTask(String authorizationHeader, Long id) {
        resolveAdminId(authorizationHeader);
        return toTaskVO(getExistingTask(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminJobCrawlTaskVO enableTask(String authorizationHeader, Long id) {
        resolveAdminId(authorizationHeader);
        JobCrawlTask task = getExistingTask(id);
        task.setStatus(STATUS_ENABLED);
        task.setNextRunAt(LocalDateTime.now());
        jobCrawlTaskMapper.updateById(task);
        return toTaskVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminJobCrawlTaskVO disableTask(String authorizationHeader, Long id) {
        resolveAdminId(authorizationHeader);
        JobCrawlTask task = getExistingTask(id);
        task.setStatus(STATUS_DISABLED);
        jobCrawlTaskMapper.updateById(task);
        return toTaskVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminJobCrawlRunVO runTask(String authorizationHeader, Long id) {
        resolveAdminId(authorizationHeader);
        getExistingTask(id);

        JobCrawlRun run = jobBatchCrawlService.runTask(id);
        return toRunVO(run);
    }

    @Override
    public List<AdminJobCrawlRunVO> listTaskRuns(String authorizationHeader, Long taskId) {
        resolveAdminId(authorizationHeader);

        LambdaQueryWrapper<JobCrawlRun> wrapper = Wrappers.lambdaQuery(JobCrawlRun.class)
                .eq(JobCrawlRun::getTaskId, taskId)
                .orderByDesc(JobCrawlRun::getCreateTime);

        return jobCrawlRunMapper.selectList(wrapper).stream().map(this::toRunVO).toList();
    }

    @Override
    public List<AdminJobCrawlItemVO> listRunItems(String authorizationHeader, Long runId) {
        resolveAdminId(authorizationHeader);

        LambdaQueryWrapper<JobCrawlItem> wrapper = Wrappers.lambdaQuery(JobCrawlItem.class)
                .eq(JobCrawlItem::getRunId, runId)
                .orderByDesc(JobCrawlItem::getCreateTime);

        return jobCrawlItemMapper.selectList(wrapper).stream().map(this::toItemVO).toList();
    }

    @Override
    public BrowserSessionService.AuthStartResult startPlatformAuth(String authorizationHeader, String platform) {
        // 1. 校验管理员身份
        resolveAdminId(authorizationHeader);

        // 2. 委托浏览器会话服务启动授权
        return browserSessionService.startAuth(platform);
    }

    @Override
    public BrowserSessionService.AuthCheckResult checkPlatformAuth(String authorizationHeader, String platform) {
        // 1. 校验管理员身份
        resolveAdminId(authorizationHeader);

        // 2. 委托浏览器会话服务检查授权状态
        return browserSessionService.checkAuth(platform);
    }

    private Long resolveAdminId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        if (token == null) throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(1).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        if (!"admin".equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return user.getId();
    }

    private JobCrawlTask getExistingTask(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务 ID 不能为空");
        }
        JobCrawlTask task = jobCrawlTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "采集任务不存在");
        }
        return task;
    }

    private AdminJobCrawlTaskVO toTaskVO(JobCrawlTask task) {
        AdminJobCrawlTaskVO vo = new AdminJobCrawlTaskVO();
        vo.setId(task.getId());
        vo.setName(task.getName());
        vo.setSourceType(task.getSourceType());
        vo.setSourceUrl(task.getSourceUrl());
        vo.setConfigJson(task.getConfigJson() != null ? task.getConfigJson().toString() : null);
        vo.setKeywords(task.getKeywords());
        vo.setCities(task.getCities());
        vo.setExperienceLevels(task.getExperienceLevels());
        vo.setScheduleType(task.getScheduleType());
        vo.setCronExpression(task.getCronExpression());
        vo.setStatus(task.getStatus());
        vo.setLastRunAt(task.getLastRunAt());
        vo.setNextRunAt(task.getNextRunAt());
        vo.setCreatedBy(task.getCreatedBy());
        vo.setRemark(task.getRemark());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private AdminJobCrawlRunVO toRunVO(JobCrawlRun run) {
        AdminJobCrawlRunVO vo = new AdminJobCrawlRunVO();
        vo.setId(run.getId());
        vo.setTaskId(run.getTaskId());
        vo.setStatus(run.getStatus());
        vo.setStartedAt(run.getStartedAt());
        vo.setFinishedAt(run.getFinishedAt());
        vo.setTotalCount(run.getTotalCount());
        vo.setSuccessCount(run.getSuccessCount());
        vo.setDuplicateCount(run.getDuplicateCount());
        vo.setFailedCount(run.getFailedCount());
        vo.setErrorMessage(run.getErrorMessage());
        vo.setCreateTime(run.getCreateTime());
        return vo;
    }

    private AdminJobCrawlItemVO toItemVO(JobCrawlItem item) {
        AdminJobCrawlItemVO vo = new AdminJobCrawlItemVO();
        vo.setId(item.getId());
        vo.setRunId(item.getRunId());
        vo.setTaskId(item.getTaskId());
        vo.setSourceUrl(item.getSourceUrl());
        vo.setJobId(item.getJobId());
        vo.setStatus(item.getStatus());
        vo.setErrorMessage(item.getErrorMessage());
        vo.setRawTitle(item.getRawTitle());
        vo.setRawCompanyName(item.getRawCompanyName());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }
}
