package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.InterviewSessionMapper;
import com.mianshiba.ai.mapper.JobApplicationMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.TrainingAnswerMapper;
import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingPlanMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.admin.AdminUserQueryRequest;
import com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest;
import com.mianshiba.ai.model.entity.InterviewSession;
import com.mianshiba.ai.model.entity.JobApplication;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.TrainingAnswer;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingPlan;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.admin.AdminOverviewVO;
import com.mianshiba.ai.model.vo.admin.AdminUserDetailVO;
import com.mianshiba.ai.model.vo.admin.AdminUserListItemVO;
import com.mianshiba.ai.service.AdminService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_USER = "user";
    private static final int STATUS_ENABLED = 0;
    private static final int STATUS_DISABLED = 1;
    private static final Set<String> VALID_ROLES = Set.of(ROLE_USER, ROLE_ADMIN);

    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final ResumeMapper resumeMapper;
    private final InterviewSessionMapper interviewSessionMapper;
    private final JobApplicationMapper jobApplicationMapper;
    private final TrainingPlanMapper trainingPlanMapper;
    private final TrainingAnswerMapper trainingAnswerMapper;
    private final TrainingAnswerReviewMapper trainingAnswerReviewMapper;

    @Override
    public AdminOverviewVO getOverview(String authorizationHeader) {
        requireAdmin(authorizationHeader);

        AdminOverviewVO overview = new AdminOverviewVO();
        overview.setTotalUsers(userMapper.selectCount(Wrappers.lambdaQuery(User.class)));
        overview.setEnabledUsers(userMapper.selectCount(Wrappers.lambdaQuery(User.class).eq(User::getUserStatus, STATUS_ENABLED)));
        overview.setDisabledUsers(userMapper.selectCount(Wrappers.lambdaQuery(User.class).eq(User::getUserStatus, STATUS_DISABLED)));
        overview.setAdminUsers(userMapper.selectCount(Wrappers.lambdaQuery(User.class).eq(User::getUserRole, ROLE_ADMIN)));
        overview.setResumeCount(resumeMapper.selectCount(Wrappers.lambdaQuery(Resume.class)));
        overview.setInterviewCount(interviewSessionMapper.selectCount(Wrappers.lambdaQuery(InterviewSession.class)));
        overview.setCompletedInterviewCount(interviewSessionMapper.selectCount(
                Wrappers.lambdaQuery(InterviewSession.class).eq(InterviewSession::getStatus, "completed")));
        overview.setApplicationCount(jobApplicationMapper.selectCount(Wrappers.lambdaQuery(JobApplication.class)));
        overview.setTrainingPlanCount(trainingPlanMapper.selectCount(Wrappers.lambdaQuery(TrainingPlan.class)));
        overview.setTrainingAnswerCount(trainingAnswerMapper.selectCount(Wrappers.lambdaQuery(TrainingAnswer.class)));
        overview.setTrainingReviewCount(trainingAnswerReviewMapper.selectCount(Wrappers.lambdaQuery(TrainingAnswerReview.class)));
        return overview;
    }

    @Override
    public List<AdminUserListItemVO> listUsers(String authorizationHeader, AdminUserQueryRequest request) {
        requireAdmin(authorizationHeader);

        AdminUserQueryRequest query = request == null ? new AdminUserQueryRequest() : request;
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery(User.class);
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(User::getUserAccount, query.getKeyword())
                    .or().like(User::getUserName, query.getKeyword())
                    .or().like(User::getEmail, query.getKeyword()));
        }
        if (query.getUserStatus() != null) {
            wrapper.eq(User::getUserStatus, query.getUserStatus());
        }
        if (StringUtils.hasText(query.getUserRole())) {
            wrapper.eq(User::getUserRole, query.getUserRole());
        }
        wrapper.orderByDesc(User::getCreateTime);

        return userMapper.selectList(wrapper).stream().map(this::toListItemVO).toList();
    }

    @Override
    public AdminUserDetailVO getUserDetail(String authorizationHeader, Long userId) {
        requireAdmin(authorizationHeader);
        User user = getExistingUser(userId);
        return toDetailVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserDetailVO disableUser(String authorizationHeader, Long userId) {
        User admin = requireAdmin(authorizationHeader);
        if (admin.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "不能禁用自己");
        }
        User user = getExistingUser(userId);
        user.setUserStatus(STATUS_DISABLED);
        userMapper.updateById(user);
        return toDetailVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserDetailVO enableUser(String authorizationHeader, Long userId) {
        requireAdmin(authorizationHeader);
        User user = getExistingUser(userId);
        user.setUserStatus(STATUS_ENABLED);
        userMapper.updateById(user);
        return toDetailVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminUserDetailVO updateUserRole(String authorizationHeader, Long userId, AdminUserRoleUpdateRequest request) {
        User admin = requireAdmin(authorizationHeader);
        if (request == null || !StringUtils.hasText(request.getUserRole())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色不能为空");
        }
        String role = request.getUserRole();
        if (!VALID_ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户角色只能是 user 或 admin");
        }
        if (admin.getId().equals(userId) && ROLE_USER.equals(role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "不能将自己降级为普通用户");
        }
        User user = getExistingUser(userId);
        user.setUserRole(role);
        userMapper.updateById(user);
        return toDetailVO(user);
    }

    private User requireAdmin(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(STATUS_DISABLED).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已禁用");
        }
        if (!ROLE_ADMIN.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }
        return user;
    }

    private User getExistingUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return user;
    }

    private AdminUserListItemVO toListItemVO(User user) {
        AdminUserListItemVO vo = new AdminUserListItemVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserAvatar(user.getUserAvatar());
        vo.setUserRole(user.getUserRole());
        vo.setUserStatus(user.getUserStatus());
        vo.setEmail(user.getEmail());
        vo.setTargetPosition(user.getTargetPosition());
        vo.setTechDirection(user.getTechDirection());
        vo.setCity(user.getCity());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    private AdminUserDetailVO toDetailVO(User user) {
        AdminUserDetailVO vo = new AdminUserDetailVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserAvatar(user.getUserAvatar());
        vo.setUserRole(user.getUserRole());
        vo.setUserStatus(user.getUserStatus());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setTargetPosition(user.getTargetPosition());
        vo.setTechDirection(user.getTechDirection());
        vo.setWorkYears(user.getWorkYears());
        vo.setCity(user.getCity());
        vo.setJobStatus(user.getJobStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setResumeCount(resumeMapper.selectCount(userScoped(Resume.class, Resume::getUserId, user.getId())));
        vo.setInterviewCount(interviewSessionMapper.selectCount(userScoped(InterviewSession.class, InterviewSession::getUserId, user.getId())));
        vo.setCompletedInterviewCount(interviewSessionMapper.selectCount(
                userScoped(InterviewSession.class, InterviewSession::getUserId, user.getId())
                        .eq(InterviewSession::getStatus, "completed")));
        vo.setApplicationCount(jobApplicationMapper.selectCount(userScoped(JobApplication.class, JobApplication::getUserId, user.getId())));
        vo.setTrainingPlanCount(trainingPlanMapper.selectCount(userScoped(TrainingPlan.class, TrainingPlan::getUserId, user.getId())));
        vo.setTrainingAnswerCount(trainingAnswerMapper.selectCount(userScoped(TrainingAnswer.class, TrainingAnswer::getUserId, user.getId())));
        vo.setTrainingReviewCount(trainingAnswerReviewMapper.selectCount(userScoped(TrainingAnswerReview.class, TrainingAnswerReview::getUserId, user.getId())));
        return vo;
    }

    private <T> LambdaQueryWrapper<T> userScoped(Class<T> entityClass,
                                                com.baomidou.mybatisplus.core.toolkit.support.SFunction<T, Long> column,
                                                Long userId) {
        return Wrappers.lambdaQuery(entityClass).eq(column, userId);
    }
}
