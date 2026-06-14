package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ApplicationTodoMapper;
import com.mianshiba.ai.mapper.CompanyMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.JobMapper;
import com.mianshiba.ai.mapper.JobMatchMapper;
import com.mianshiba.ai.mapper.JobRecommendationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.job.JobRecommendationRefineRequest;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import com.mianshiba.ai.model.entity.Company;
import com.mianshiba.ai.model.entity.Job;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.JobMatch;
import com.mianshiba.ai.model.entity.JobRecommendation;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.application.ApplicationTodoVO;
import com.mianshiba.ai.model.vo.application.JobApplicationVO;
import com.mianshiba.ai.model.vo.job.CompanyVO;
import com.mianshiba.ai.model.vo.job.JobMatchVO;
import com.mianshiba.ai.model.vo.job.JobRecommendationVO;
import com.mianshiba.ai.model.vo.job.JobVO;
import com.mianshiba.ai.service.JobRecommendationService;
import com.mianshiba.ai.service.ResumeJobMatchService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobRecommendationServiceImpl implements JobRecommendationService {

    private final JobRecommendationMapper recommendationMapper;
    private final JobMapper jobMapper;
    private final JobMatchMapper jobMatchMapper;
    private final JobApplicationMapper applicationMapper;
    private final ApplicationTodoMapper todoMapper;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final ResumeJobMatchService resumeJobMatchService;
    private final ResumeMapper resumeMapper;
    private final CompanyMapper companyMapper;
    private final ObjectMapper objectMapper;

    @Override
    public List<JobRecommendationVO> listRecommendations(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);

        List<JobRecommendation> recommendations = recommendationMapper.selectList(
                Wrappers.lambdaQuery(JobRecommendation.class)
                        .eq(JobRecommendation::getUserId, userId)
                        .eq(JobRecommendation::getDismissed, 0)
                        .eq(JobRecommendation::getApplied, 0)
                        .orderByDesc(JobRecommendation::getCreateTime));

        return recommendations.stream()
                .map(this::toJobRecommendationVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<JobRecommendationVO> refine(String authorizationHeader, JobRecommendationRefineRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        Resume resume = resumeMapper.selectOne(
                Wrappers.lambdaQuery(Resume.class)
                        .eq(Resume::getId, request.getResumeId())
                        .eq(Resume::getUserId, userId));
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "简历不存在");
        }

        List<JobRecommendation> recommendations = recommendationMapper.selectList(
                Wrappers.lambdaQuery(JobRecommendation.class)
                        .eq(JobRecommendation::getUserId, userId)
                        .eq(JobRecommendation::getDismissed, 0)
                        .eq(JobRecommendation::getApplied, 0));

        for (JobRecommendation rec : recommendations) {
            JobMatch match = resumeJobMatchService.match(userId, request.getResumeId(), rec.getJobId());
            rec.setMatchId(match.getId());
            rec.setStage("refined");
            recommendationMapper.updateById(rec);
        }

        return recommendations.stream()
                .map(this::toJobRecommendationVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismiss(String authorizationHeader, Long recommendationId) {
        Long userId = resolveUserId(authorizationHeader);

        JobRecommendation rec = recommendationMapper.selectById(recommendationId);
        if (rec == null || !rec.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "推荐不存在");
        }

        rec.setDismissed(1);
        recommendationMapper.updateById(rec);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public JobApplicationVO apply(String authorizationHeader, Long recommendationId) {
        Long userId = resolveUserId(authorizationHeader);

        JobRecommendation rec = recommendationMapper.selectById(recommendationId);
        if (rec == null || !rec.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "推荐不存在");
        }

        Job job = jobMapper.selectById(rec.getJobId());
        if (job == null) {
            throw new BusinessException(ErrorCode.JOB_NOT_FOUND_ERROR);
        }

        List<JobApplication> existingApps = applicationMapper.selectList(
                Wrappers.lambdaQuery(JobApplication.class)
                        .eq(JobApplication::getUserId, userId)
                        .eq(JobApplication::getJobId, rec.getJobId()));
        if (!existingApps.isEmpty()) {
            JobApplication existing = existingApps.get(0);
            List<ApplicationTodo> todos = todoMapper.selectList(
                    Wrappers.lambdaQuery(ApplicationTodo.class)
                            .eq(ApplicationTodo::getApplicationId, existing.getId())
                            .orderByAsc(ApplicationTodo::getDueAt));
            return toApplicationVO(existing, todos);
        }

        JobApplication app = new JobApplication();
        app.setUserId(userId);
        app.setJobId(job.getId());
        app.setResumeId(rec.getResumeId());
        app.setCompanyName(job.getCompanyName());
        app.setJobTitle(job.getTitle());
        app.setSource(job.getSourcePlatform());
        app.setStatus("pending_submit");
        app.setSalaryRange(job.getSalaryRange());
        app.setLocation(job.getCity());
        applicationMapper.insert(app);

        createTodo(userId, app.getId(), "根据 JD 优化简历");
        createTodo(userId, app.getId(), "准备核心技能题");
        createTodo(userId, app.getId(), "投递后 3 天跟进");

        rec.setApplied(1);
        recommendationMapper.updateById(rec);

        List<ApplicationTodo> todos = todoMapper.selectList(
                Wrappers.lambdaQuery(ApplicationTodo.class)
                        .eq(ApplicationTodo::getApplicationId, app.getId())
                        .orderByAsc(ApplicationTodo::getDueAt));
        return toApplicationVO(app, todos);
    }

    private void createTodo(Long userId, Long applicationId, String title) {
        ApplicationTodo todo = new ApplicationTodo();
        todo.setUserId(userId);
        todo.setApplicationId(applicationId);
        todo.setTitle(title);
        todo.setPriority("medium");
        todo.setCompleted(0);
        todoMapper.insert(todo);
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(1).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        return user.getId();
    }

    private JobRecommendationVO toJobRecommendationVO(JobRecommendation rec) {
        JobRecommendationVO vo = new JobRecommendationVO();
        vo.setId(rec.getId());
        vo.setStage(rec.getStage());
        vo.setRoughScore(rec.getRoughScore());
        vo.setRecommendation(rec.getRecommendation());
        vo.setReason(rec.getReason());
        vo.setDismissed(rec.getDismissed() != null && rec.getDismissed() == 1);
        vo.setApplied(rec.getApplied() != null && rec.getApplied() == 1);

        if (rec.getJobId() != null) {
            Job job = jobMapper.selectById(rec.getJobId());
            if (job != null) {
                vo.setJob(toJobVO(job));
            }
        }

        if (rec.getMatchId() != null) {
            JobMatch match = jobMatchMapper.selectById(rec.getMatchId());
            if (match != null) {
                vo.setMatchResult(toJobMatchVO(match));
            }
        }

        vo.setRiskPoints(parseStringList(rec.getRiskPointsJson()));
        vo.setActionSuggestions(parseStringList(rec.getActionSuggestionsJson()));

        return vo;
    }

    private JobVO toJobVO(Job job) {
        JobVO vo = new JobVO();
        vo.setId(job.getId());
        vo.setCompanyName(job.getCompanyName());
        vo.setTitle(job.getTitle());
        vo.setSourcePlatform(job.getSourcePlatform());
        vo.setCity(job.getCity());
        vo.setSalaryRange(job.getSalaryRange());
        vo.setExperienceRequirement(job.getExperienceRequirement());
        vo.setEducationRequirement(job.getEducationRequirement());
        vo.setJobDescription(job.getJobDescription());
        vo.setJobRequirement(job.getJobRequirement());
        vo.setTechStack(job.getTechStack());
        vo.setStatus(job.getStatus());
        return vo;
    }

    private JobMatchVO toJobMatchVO(JobMatch match) {
        JobMatchVO vo = new JobMatchVO();
        vo.setId(match.getId());
        vo.setMatchScore(match.getMatchScore());
        vo.setGrowthScore(match.getGrowthScore());
        vo.setTechGrowthScore(match.getTechGrowthScore());
        vo.setSalaryCityScore(match.getSalaryCityScore());
        vo.setExperienceFitScore(match.getExperienceFitScore());
        vo.setTotalScore(match.getTotalScore());
        vo.setRecommendation(match.getRecommendation());
        vo.setReason(match.getReason());
        vo.setGaps(match.getGaps());
        return vo;
    }

    private List<String> parseStringList(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof String) {
                return objectMapper.readValue((String) value, new TypeReference<List<String>>() {});
            }
            return objectMapper.convertValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 JSON 列表失败", e);
            return null;
        }
    }

    private JobApplicationVO toApplicationVO(JobApplication app, List<ApplicationTodo> todos) {
        JobApplicationVO vo = new JobApplicationVO();
        vo.setId(app.getId());
        vo.setUserId(app.getUserId());
        vo.setJobId(app.getJobId());
        vo.setResumeId(app.getResumeId());
        vo.setCompanyName(app.getCompanyName());
        vo.setJobTitle(app.getJobTitle());
        vo.setSource(app.getSource());
        vo.setStatus(app.getStatus());
        vo.setAppliedAt(app.getAppliedAt());
        vo.setNextEventAt(app.getNextEventAt());
        vo.setSalaryRange(app.getSalaryRange());
        vo.setLocation(app.getLocation());
        vo.setContactName(app.getContactName());
        vo.setContactInfo(app.getContactInfo());
        vo.setNotes(app.getNotes());
        vo.setCreateTime(app.getCreateTime());
        vo.setUpdateTime(app.getUpdateTime());

        List<ApplicationTodoVO> todoVOs = todos.stream()
                .map(todo -> {
                    ApplicationTodoVO tvo = new ApplicationTodoVO();
                    tvo.setId(todo.getId());
                    tvo.setApplicationId(todo.getApplicationId());
                    tvo.setTitle(todo.getTitle());
                    tvo.setDescription(todo.getDescription());
                    tvo.setPriority(todo.getPriority());
                    tvo.setDueAt(todo.getDueAt());
                    tvo.setCompleted(todo.getCompleted() != null && todo.getCompleted() == 1);
                    tvo.setCompletedAt(todo.getCompletedAt());
                    tvo.setCreateTime(todo.getCreateTime());
                    tvo.setUpdateTime(todo.getUpdateTime());
                    return tvo;
                })
                .collect(Collectors.toList());
        vo.setTodos(todoVOs);
        vo.setUnfinishedTodoCount((int) todos.stream()
                .filter(t -> t.getCompleted() == null || t.getCompleted() == 0)
                .count());
        return vo;
    }

    int calculateRoughScore(Job job) {
        int score = 50;
        if (StringUtils.hasText(job.getSalaryRange())) score += 10;
        if (StringUtils.hasText(job.getTechStack())) score += 15;
        if (StringUtils.hasText(job.getJobRequirement())) score += 15;
        if (StringUtils.hasText(job.getCity())) score += 10;
        return Math.min(score, 100);
    }
}
