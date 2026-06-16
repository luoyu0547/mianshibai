package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ApplicationRoundMapper;
import com.mianshiba.ai.mapper.ApplicationTodoMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.model.dto.application.ApplicationCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationListQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationStatusUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationRoundCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationRoundUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationRoundResultRequest;
import com.mianshiba.ai.model.entity.ApplicationRound;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.vo.application.ApplicationStatsVO;
import com.mianshiba.ai.model.vo.application.ApplicationRoundVO;
import com.mianshiba.ai.model.vo.application.ApplicationTodoVO;
import com.mianshiba.ai.model.vo.application.JobApplicationVO;
import com.mianshiba.ai.service.ApplicationService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 求职投递服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final Set<String> VALID_STATUSES = Set.of(
            "pending_submit", "submitted", "interviewing", "offer", "rejected", "withdrawn");

    private static final Set<String> VALID_PRIORITIES = Set.of("low", "medium", "high");

    private static final Map<String, String> STATUS_LABELS = Map.ofEntries(
            Map.entry("pending_submit", "待投递"),
            Map.entry("submitted", "已投递"),
            Map.entry("interviewing", "面试中"),
            Map.entry("offer", "Offer"),
            Map.entry("rejected", "拒绝"),
            Map.entry("withdrawn", "放弃")
    );

    private static final Map<String, String> PRIORITY_LABELS = Map.of(
            "low", "低", "medium", "中", "high", "高"
    );

    private final JwtUtils jwtUtils;
    private final JobApplicationMapper applicationMapper;
    private final ApplicationTodoMapper todoMapper;
    private final ResumeMapper resumeMapper;
    private final ApplicationRoundMapper roundMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobApplicationVO createApplication(String authorizationHeader, ApplicationCreateRequest request) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 规范化状态
        String status = normalizeStatus(request.getStatus());

        // 3. 校验 resumeId 是否属于当前用户
        if (request.getResumeId() != null) {
            Resume resume = resumeMapper.selectOne(
                    Wrappers.lambdaQuery(Resume.class)
                            .eq(Resume::getId, request.getResumeId())
                            .eq(Resume::getUserId, userId));
            if (resume == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "简历不属于当前用户");
            }
        }

        // 4. 构建实体并插入
        JobApplication app = new JobApplication();
        app.setUserId(userId);
        app.setResumeId(request.getResumeId());
        app.setCompanyName(request.getCompanyName());
        app.setJobTitle(request.getJobTitle());
        app.setSource(request.getSource());
        app.setStatus(status);
        app.setAppliedAt(request.getAppliedAt());
        app.setNextEventAt(request.getNextEventAt());
        app.setSalaryRange(request.getSalaryRange());
        app.setLocation(request.getLocation());
        app.setContactName(request.getContactName());
        app.setContactInfo(request.getContactInfo());
        app.setNotes(request.getNotes());
        applicationMapper.insert(app);

        // 5. 返回详情
        return toApplicationVO(app, Collections.emptyList(), Collections.emptyList());
    }

    @Override
    public List<JobApplicationVO> listApplications(String authorizationHeader, ApplicationListQueryRequest request) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 构建查询条件
        LambdaQueryWrapper<JobApplication> wrapper = Wrappers.lambdaQuery(JobApplication.class)
                .eq(JobApplication::getUserId, userId);
        if (StringUtils.hasText(request.getKeyword())) {
            wrapper.and(w -> w.like(JobApplication::getCompanyName, request.getKeyword())
                    .or().like(JobApplication::getJobTitle, request.getKeyword()));
        }
        if (StringUtils.hasText(request.getStatus())) {
            wrapper.eq(JobApplication::getStatus, request.getStatus());
        }
        if (StringUtils.hasText(request.getLocation())) {
            wrapper.eq(JobApplication::getLocation, request.getLocation());
        }
        if (StringUtils.hasText(request.getSource())) {
            wrapper.eq(JobApplication::getSource, request.getSource());
        }
        if (request.getResumeId() != null) {
            wrapper.eq(JobApplication::getResumeId, request.getResumeId());
        }
        // 3. 排序：nextEventAt asc nulls last，createTime desc
        wrapper.orderByDesc(JobApplication::getCreateTime);

        List<JobApplication> applications = applicationMapper.selectList(wrapper);

        // 4. 为每条记录查询未完成待办数和待办列表
        return applications.stream().map(app -> {
            List<ApplicationTodo> todos = todoMapper.selectList(
                    Wrappers.lambdaQuery(ApplicationTodo.class)
                            .eq(ApplicationTodo::getApplicationId, app.getId())
                            .orderByAsc(ApplicationTodo::getDueAt));
            List<ApplicationRound> rounds = roundMapper.selectList(
                    Wrappers.lambdaQuery(ApplicationRound.class)
                            .eq(ApplicationRound::getApplicationId, app.getId())
                            .orderByAsc(ApplicationRound::getRoundOrder));
            return toApplicationVO(app, todos, rounds);
        }).collect(Collectors.toList());
    }

    @Override
    public JobApplicationVO getApplication(String authorizationHeader, Long id) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询投递记录并校验归属
        JobApplication app = applicationMapper.selectById(id);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND_ERROR);
        }

        // 3. 查询待办列表
        List<ApplicationTodo> todos = todoMapper.selectList(
                Wrappers.lambdaQuery(ApplicationTodo.class)
                        .eq(ApplicationTodo::getApplicationId, id)
                        .orderByAsc(ApplicationTodo::getDueAt));

        // 4. 查询轮次列表
        List<ApplicationRound> rounds = roundMapper.selectList(
                Wrappers.lambdaQuery(ApplicationRound.class)
                        .eq(ApplicationRound::getApplicationId, id)
                        .orderByAsc(ApplicationRound::getRoundOrder));

        return toApplicationVO(app, todos, rounds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobApplicationVO updateApplication(String authorizationHeader, Long id, ApplicationUpdateRequest request) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询并校验归属
        JobApplication app = applicationMapper.selectById(id);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND_ERROR);
        }

        // 3. 更新非空字段
        if (request.getResumeId() != null) {
            // 3.1 校验 resumeId 归属
            Resume resume = resumeMapper.selectOne(
                    Wrappers.lambdaQuery(Resume.class)
                            .eq(Resume::getId, request.getResumeId())
                            .eq(Resume::getUserId, userId));
            if (resume == null) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "简历不属于当前用户");
            }
            app.setResumeId(request.getResumeId());
        }
        if (request.getCompanyName() != null) {
            app.setCompanyName(request.getCompanyName());
        }
        if (request.getJobTitle() != null) {
            app.setJobTitle(request.getJobTitle());
        }
        if (request.getSource() != null) {
            app.setSource(request.getSource());
        }
        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
            app.setStatus(request.getStatus());
        }
        if (request.getAppliedAt() != null) {
            app.setAppliedAt(request.getAppliedAt());
        }
        if (request.getNextEventAt() != null) {
            app.setNextEventAt(request.getNextEventAt());
        }
        if (request.getSalaryRange() != null) {
            app.setSalaryRange(request.getSalaryRange());
        }
        if (request.getLocation() != null) {
            app.setLocation(request.getLocation());
        }
        if (request.getContactName() != null) {
            app.setContactName(request.getContactName());
        }
        if (request.getContactInfo() != null) {
            app.setContactInfo(request.getContactInfo());
        }
        if (request.getNotes() != null) {
            app.setNotes(request.getNotes());
        }
        applicationMapper.updateById(app);

        // 4. 查询待办列表和轮次列表并返回
        List<ApplicationTodo> todos = todoMapper.selectList(
                Wrappers.lambdaQuery(ApplicationTodo.class)
                        .eq(ApplicationTodo::getApplicationId, id)
                        .orderByAsc(ApplicationTodo::getDueAt));
        List<ApplicationRound> rounds = roundMapper.selectList(
                Wrappers.lambdaQuery(ApplicationRound.class)
                        .eq(ApplicationRound::getApplicationId, id)
                        .orderByAsc(ApplicationRound::getRoundOrder));
        return toApplicationVO(app, todos, rounds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobApplicationVO updateStatus(String authorizationHeader, Long id, ApplicationStatusUpdateRequest request) {
        // 1. 校验状态值
        validateStatus(request.getStatus());

        // 2. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 3. 查询并校验归属
        JobApplication app = applicationMapper.selectById(id);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND_ERROR);
        }

        // 4. 更新状态
        app.setStatus(request.getStatus());
        applicationMapper.updateById(app);

        // 5. 查询待办列表和轮次列表并返回
        List<ApplicationTodo> todos = todoMapper.selectList(
                Wrappers.lambdaQuery(ApplicationTodo.class)
                        .eq(ApplicationTodo::getApplicationId, id)
                        .orderByAsc(ApplicationTodo::getDueAt));
        List<ApplicationRound> rounds = roundMapper.selectList(
                Wrappers.lambdaQuery(ApplicationRound.class)
                        .eq(ApplicationRound::getApplicationId, id)
                        .orderByAsc(ApplicationRound::getRoundOrder));
        return toApplicationVO(app, todos, rounds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteApplication(String authorizationHeader, Long id) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询并校验归属
        JobApplication app = applicationMapper.selectById(id);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND_ERROR);
        }

        // 3. 逻辑删除投递记录
        applicationMapper.deleteById(id);

        // 4. 逻辑删除关联待办
        todoMapper.delete(
                Wrappers.lambdaQuery(ApplicationTodo.class)
                        .eq(ApplicationTodo::getApplicationId, id)
                        .eq(ApplicationTodo::getUserId, userId));
    }

    @Override
    public ApplicationStatsVO getStats(String authorizationHeader) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询该用户所有投递记录
        List<JobApplication> applications = applicationMapper.selectList(
                Wrappers.lambdaQuery(JobApplication.class)
                        .eq(JobApplication::getUserId, userId));

        // 3. 按状态分组统计
        Map<String, Long> statusCount = applications.stream()
                .collect(Collectors.groupingBy(
                        app -> app.getStatus() != null ? app.getStatus() : "pending_submit",
                        Collectors.counting()));

        // 4. 构建统计 VO
        ApplicationStatsVO stats = new ApplicationStatsVO();
        stats.setTotal((long) applications.size());
        stats.setPendingSubmit(statusCount.getOrDefault("pending_submit", 0L));
        stats.setSubmitted(statusCount.getOrDefault("submitted", 0L));
        stats.setInterviewing(statusCount.getOrDefault("interviewing", 0L));
        stats.setOffer(statusCount.getOrDefault("offer", 0L));
        stats.setClosed(statusCount.getOrDefault("rejected", 0L) + statusCount.getOrDefault("withdrawn", 0L));
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationTodoVO createApplicationTodo(String authorizationHeader, Long applicationId, ApplicationTodoCreateRequest request) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 校验投递记录归属
        JobApplication app = applicationMapper.selectById(applicationId);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND_ERROR);
        }

        // 3. 创建待办
        ApplicationTodo todo = buildTodo(userId, applicationId, request);
        todoMapper.insert(todo);

        return toTodoVO(todo, app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationTodoVO createGlobalTodo(String authorizationHeader, ApplicationTodoCreateRequest request) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 创建全局待办（不关联投递记录）
        ApplicationTodo todo = buildTodo(userId, null, request);
        todoMapper.insert(todo);

        return toTodoVO(todo, null);
    }

    @Override
    public List<ApplicationTodoVO> listTodos(String authorizationHeader, ApplicationTodoQueryRequest request) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 构建查询条件
        LambdaQueryWrapper<ApplicationTodo> wrapper = Wrappers.lambdaQuery(ApplicationTodo.class)
                .eq(ApplicationTodo::getUserId, userId);
        if (request.getApplicationId() != null) {
            wrapper.eq(ApplicationTodo::getApplicationId, request.getApplicationId());
        }
        if (request.getCompleted() != null) {
            wrapper.eq(ApplicationTodo::getCompleted, request.getCompleted() ? 1 : 0);
        }
        if (StringUtils.hasText(request.getPriority())) {
            wrapper.eq(ApplicationTodo::getPriority, request.getPriority());
        }
        if (Boolean.TRUE.equals(request.getOverdue())) {
            wrapper.lt(ApplicationTodo::getDueAt, LocalDateTime.now());
        }
        // 3. 排序：priority desc, dueAt asc
        wrapper.orderByDesc(ApplicationTodo::getPriority)
                .orderByAsc(ApplicationTodo::getDueAt);

        List<ApplicationTodo> todos = todoMapper.selectList(wrapper);

        // 4. 查询关联的投递记录信息
        return todos.stream().map(todo -> {
            JobApplication app = todo.getApplicationId() != null
                    ? applicationMapper.selectById(todo.getApplicationId()) : null;
            return toTodoVO(todo, app);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationTodoVO updateTodo(String authorizationHeader, Long todoId, ApplicationTodoUpdateRequest request) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询并校验归属
        ApplicationTodo todo = todoMapper.selectById(todoId);
        if (todo == null || !todo.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_TODO_NOT_FOUND_ERROR);
        }

        // 3. 更新非空字段
        if (StringUtils.hasText(request.getTitle())) {
            todo.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            todo.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getPriority())) {
            normalizePriority(request.getPriority());
            todo.setPriority(request.getPriority());
        }
        if (request.getDueAt() != null) {
            todo.setDueAt(request.getDueAt());
        }
        todoMapper.updateById(todo);

        // 4. 查询关联投递记录
        JobApplication app = todo.getApplicationId() != null
                ? applicationMapper.selectById(todo.getApplicationId()) : null;
        return toTodoVO(todo, app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationTodoVO completeTodo(String authorizationHeader, Long todoId) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询并校验归属
        ApplicationTodo todo = todoMapper.selectById(todoId);
        if (todo == null || !todo.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_TODO_NOT_FOUND_ERROR);
        }

        // 3. 标记完成
        todo.setCompleted(1);
        todo.setCompletedAt(LocalDateTime.now());
        todoMapper.updateById(todo);

        // 4. 查询关联投递记录
        JobApplication app = todo.getApplicationId() != null
                ? applicationMapper.selectById(todo.getApplicationId()) : null;
        return toTodoVO(todo, app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationTodoVO reopenTodo(String authorizationHeader, Long todoId) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询并校验归属
        ApplicationTodo todo = todoMapper.selectById(todoId);
        if (todo == null || !todo.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_TODO_NOT_FOUND_ERROR);
        }

        // 3. 重新开启
        todo.setCompleted(0);
        todo.setCompletedAt(null);
        todoMapper.updateById(todo);

        // 4. 查询关联投递记录
        JobApplication app = todo.getApplicationId() != null
                ? applicationMapper.selectById(todo.getApplicationId()) : null;
        return toTodoVO(todo, app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTodo(String authorizationHeader, Long todoId) {
        // 1. 解析用户身份
        Long userId = resolveUserId(authorizationHeader);

        // 2. 查询并校验归属
        ApplicationTodo todo = todoMapper.selectById(todoId);
        if (todo == null || !todo.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_TODO_NOT_FOUND_ERROR);
        }

        // 3. 逻辑删除
        todoMapper.deleteById(todoId);
    }

    @Override
    public List<ApplicationRoundVO> listRounds(String authorizationHeader, Long applicationId) {
        resolveUserId(authorizationHeader);
        List<ApplicationRound> rounds = roundMapper.selectList(
                Wrappers.lambdaQuery(ApplicationRound.class)
                        .eq(ApplicationRound::getApplicationId, applicationId)
                        .orderByAsc(ApplicationRound::getRoundOrder));
        return rounds.stream().map(this::toRoundVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationRoundVO createRound(String authorizationHeader, Long applicationId, ApplicationRoundCreateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        JobApplication app = applicationMapper.selectById(applicationId);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND_ERROR);
        }
        if ("submitted".equals(app.getStatus())) {
            app.setStatus("interviewing");
            applicationMapper.updateById(app);
        }
        Long count = roundMapper.selectCount(
                Wrappers.lambdaQuery(ApplicationRound.class)
                        .eq(ApplicationRound::getApplicationId, applicationId));
        int nextOrder = count != null ? count.intValue() : 0;

        ApplicationRound round = new ApplicationRound();
        round.setApplicationId(applicationId);
        round.setRoundName(request.getRoundName());
        round.setRoundOrder(nextOrder);
        round.setScheduledAt(request.getScheduledAt());
        round.setResult("pending");
        round.setNotes(request.getNotes());
        roundMapper.insert(round);
        return toRoundVO(round);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationRoundVO updateRound(String authorizationHeader, Long applicationId, Long roundId, ApplicationRoundUpdateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        validateRoundOwnership(roundId, applicationId, userId);
        ApplicationRound round = roundMapper.selectById(roundId);
        if (request.getRoundName() != null) round.setRoundName(request.getRoundName());
        if (request.getScheduledAt() != null) round.setScheduledAt(request.getScheduledAt());
        if (request.getNotes() != null) round.setNotes(request.getNotes());
        roundMapper.updateById(round);
        return toRoundVO(round);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ApplicationRoundVO setRoundResult(String authorizationHeader, Long applicationId, Long roundId, ApplicationRoundResultRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        validateRoundOwnership(roundId, applicationId, userId);
        String result = request.getResult();
        if (!Set.of("pass", "fail").contains(result)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "轮次结果不合法");
        }
        ApplicationRound round = roundMapper.selectById(roundId);
        round.setResult(result);
        roundMapper.updateById(round);

        if ("fail".equals(result)) {
            JobApplication app = applicationMapper.selectById(applicationId);
            if (app != null) {
                app.setStatus("rejected");
                applicationMapper.updateById(app);
            }
        }
        return toRoundVO(round);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRound(String authorizationHeader, Long applicationId, Long roundId) {
        Long userId = resolveUserId(authorizationHeader);
        validateRoundOwnership(roundId, applicationId, userId);
        roundMapper.deleteById(roundId);
    }

    private void validateRoundOwnership(Long roundId, Long applicationId, Long userId) {
        ApplicationRound round = roundMapper.selectById(roundId);
        if (round == null || !round.getApplicationId().equals(applicationId)) {
            throw new BusinessException(ErrorCode.APPLICATION_ROUND_NOT_FOUND_ERROR);
        }
        JobApplication app = applicationMapper.selectById(applicationId);
        if (app == null || !app.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.APPLICATION_NOT_FOUND_ERROR);
        }
    }

    private ApplicationRoundVO toRoundVO(ApplicationRound round) {
        ApplicationRoundVO vo = new ApplicationRoundVO();
        vo.setId(round.getId());
        vo.setApplicationId(round.getApplicationId());
        vo.setRoundName(round.getRoundName());
        vo.setRoundOrder(round.getRoundOrder());
        vo.setScheduledAt(round.getScheduledAt());
        vo.setResult(round.getResult());
        vo.setNotes(round.getNotes());
        return vo;
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        return claims.userId();
    }

    private void validateStatus(String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "投递状态不合法");
        }
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "pending_submit";
        }
        validateStatus(status);
        return status;
    }

    private String normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return "medium";
        }
        if (!VALID_PRIORITIES.contains(priority)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "待办优先级不合法");
        }
        return priority;
    }

    private JobApplicationVO toApplicationVO(JobApplication app, List<ApplicationTodo> todos, List<ApplicationRound> rounds) {
        JobApplicationVO vo = new JobApplicationVO();
        vo.setId(app.getId());
        vo.setUserId(app.getUserId());
        vo.setResumeId(app.getResumeId());
        vo.setCompanyName(app.getCompanyName());
        vo.setJobTitle(app.getJobTitle());
        vo.setSource(app.getSource());
        vo.setStatus(app.getStatus());
        vo.setStatusLabel(STATUS_LABELS.getOrDefault(app.getStatus(), app.getStatus()));
        vo.setAppliedAt(app.getAppliedAt());
        vo.setNextEventAt(app.getNextEventAt());
        vo.setSalaryRange(app.getSalaryRange());
        vo.setLocation(app.getLocation());
        vo.setContactName(app.getContactName());
        vo.setContactInfo(app.getContactInfo());
        vo.setNotes(app.getNotes());
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());

        // 设置待办列表和未完成计数
        List<ApplicationTodoVO> todoVOs = todos.stream()
                .map(todo -> {
                    JobApplication todoApp = todo.getApplicationId() != null ? app : null;
                    return toTodoVO(todo, todoApp);
                })
                .collect(Collectors.toList());
        vo.setTodos(todoVOs);
        vo.setUnfinishedTodoCount((int) todos.stream()
                .filter(t -> t.getCompleted() == null || t.getCompleted() == 0)
                .count());

        // 设置轮次列表
        List<ApplicationRoundVO> roundVOs = rounds.stream().map(this::toRoundVO).collect(Collectors.toList());
        vo.setRounds(roundVOs);

        return vo;
    }

    private ApplicationTodoVO toTodoVO(ApplicationTodo todo, JobApplication app) {
        ApplicationTodoVO vo = new ApplicationTodoVO();
        vo.setId(todo.getId());
        vo.setApplicationId(todo.getApplicationId());
        vo.setApplicationCompanyName(app != null ? app.getCompanyName() : null);
        vo.setApplicationJobTitle(app != null ? app.getJobTitle() : null);
        vo.setTitle(todo.getTitle());
        vo.setDescription(todo.getDescription());
        vo.setPriority(todo.getPriority());
        vo.setPriorityLabel(todo.getPriority() != null ? PRIORITY_LABELS.getOrDefault(todo.getPriority(), todo.getPriority()) : "中");
        vo.setDueAt(todo.getDueAt());
        vo.setCompleted(todo.getCompleted() != null && todo.getCompleted() == 1);
        vo.setCompletedAt(todo.getCompletedAt());
        vo.setCreateTime(todo.getCreateTime());
        vo.setUpdateTime(todo.getUpdateTime());
        return vo;
    }

    private ApplicationTodo buildTodo(Long userId, Long applicationId, ApplicationTodoCreateRequest request) {
        ApplicationTodo todo = new ApplicationTodo();
        todo.setUserId(userId);
        todo.setApplicationId(applicationId);
        todo.setTitle(request.getTitle());
        todo.setDescription(request.getDescription());
        todo.setPriority(normalizePriority(request.getPriority()));
        todo.setDueAt(request.getDueAt());
        todo.setCompleted(0);
        return todo;
    }
}
