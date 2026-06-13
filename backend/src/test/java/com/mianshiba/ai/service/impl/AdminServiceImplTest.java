package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.admin.AdminOverviewVO;
import com.mianshiba.ai.model.vo.admin.AdminUserDetailVO;
import com.mianshiba.ai.model.vo.admin.AdminUserListItemVO;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserMapper userMapper;
    @Mock
    private ResumeMapper resumeMapper;
    @Mock
    private InterviewSessionMapper interviewSessionMapper;
    @Mock
    private JobApplicationMapper jobApplicationMapper;
    @Mock
    private TrainingPlanMapper trainingPlanMapper;
    @Mock
    private TrainingAnswerMapper trainingAnswerMapper;
    @Mock
    private TrainingAnswerReviewMapper trainingAnswerReviewMapper;

    @InjectMocks
    private AdminServiceImpl adminService;

    @Test
    void getOverview_rejectsNormalUser() {
        mockClaims(1L, "user");
        User user = adminUser(1L);
        user.setUserRole("user");
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThatThrownBy(() -> adminService.getOverview("Bearer token"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NO_AUTH_ERROR.getCode());
    }

    @Test
    void getOverview_rejectsDisabledAdmin() {
        mockClaims(1L, "admin");
        User user = adminUser(1L);
        user.setUserStatus(1);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThatThrownBy(() -> adminService.getOverview("Bearer token"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN_ERROR.getCode());
    }

    @Test
    void getOverview_countsPlatformMetrics() {
        mockAdmin();
        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 8L, 2L, 1L);
        when(resumeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(11L);
        when(interviewSessionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(12L, 7L);
        when(jobApplicationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(13L);
        when(trainingPlanMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(14L);
        when(trainingAnswerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(15L);
        when(trainingAnswerReviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(16L);

        AdminOverviewVO overview = adminService.getOverview("Bearer token");

        assertThat(overview.getTotalUsers()).isEqualTo(10L);
        assertThat(overview.getEnabledUsers()).isEqualTo(8L);
        assertThat(overview.getDisabledUsers()).isEqualTo(2L);
        assertThat(overview.getAdminUsers()).isEqualTo(1L);
        assertThat(overview.getResumeCount()).isEqualTo(11L);
        assertThat(overview.getInterviewCount()).isEqualTo(12L);
        assertThat(overview.getCompletedInterviewCount()).isEqualTo(7L);
        assertThat(overview.getApplicationCount()).isEqualTo(13L);
        assertThat(overview.getTrainingPlanCount()).isEqualTo(14L);
        assertThat(overview.getTrainingAnswerCount()).isEqualTo(15L);
        assertThat(overview.getTrainingReviewCount()).isEqualTo(16L);
    }

    @Test
    void listUsers_mapsUsers() {
        mockAdmin();
        User target = adminUser(2L);
        target.setUserRole("user");
        target.setUserAccount("candidate");
        target.setUserName("候选人");
        when(userMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(target));

        List<AdminUserListItemVO> users = adminService.listUsers("Bearer token", new AdminUserQueryRequest());

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getId()).isEqualTo(2L);
        assertThat(users.get(0).getUserAccount()).isEqualTo("candidate");
        assertThat(users.get(0).getUserName()).isEqualTo("候选人");
    }

    @Test
    void getUserDetail_countsUserMetrics() {
        mockAdmin();
        User target = adminUser(2L);
        target.setUserRole("user");
        when(userMapper.selectById(2L)).thenReturn(target);
        when(resumeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(interviewSessionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L, 1L);
        when(jobApplicationMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(4L);
        when(trainingPlanMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L);
        when(trainingAnswerMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(6L);
        when(trainingAnswerReviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(7L);

        AdminUserDetailVO detail = adminService.getUserDetail("Bearer token", 2L);

        assertThat(detail.getId()).isEqualTo(2L);
        assertThat(detail.getResumeCount()).isEqualTo(2L);
        assertThat(detail.getInterviewCount()).isEqualTo(3L);
        assertThat(detail.getCompletedInterviewCount()).isEqualTo(1L);
        assertThat(detail.getApplicationCount()).isEqualTo(4L);
        assertThat(detail.getTrainingPlanCount()).isEqualTo(5L);
        assertThat(detail.getTrainingAnswerCount()).isEqualTo(6L);
        assertThat(detail.getTrainingReviewCount()).isEqualTo(7L);
    }

    @Test
    void disableUser_updatesOtherUserStatus() {
        mockAdmin();
        User target = adminUser(2L);
        target.setUserRole("user");
        when(userMapper.selectById(2L)).thenReturn(target);

        AdminUserDetailVO detail = adminService.disableUser("Bearer token", 2L);

        assertThat(target.getUserStatus()).isEqualTo(1);
        assertThat(detail.getUserStatus()).isEqualTo(1);
    }

    @Test
    void disableUser_rejectsSelfDisable() {
        mockAdmin();

        assertThatThrownBy(() -> adminService.disableUser("Bearer token", 1L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN_ERROR.getCode());
    }

    @Test
    void enableUser_updatesStatus() {
        mockAdmin();
        User target = adminUser(2L);
        target.setUserStatus(1);
        when(userMapper.selectById(2L)).thenReturn(target);

        AdminUserDetailVO detail = adminService.enableUser("Bearer token", 2L);

        assertThat(target.getUserStatus()).isEqualTo(0);
        assertThat(detail.getUserStatus()).isEqualTo(0);
    }

    @Test
    void updateUserRole_updatesOtherUserRole() {
        mockAdmin();
        User target = adminUser(2L);
        target.setUserRole("user");
        when(userMapper.selectById(2L)).thenReturn(target);
        com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest request = new com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest();
        request.setUserRole("admin");

        AdminUserDetailVO detail = adminService.updateUserRole("Bearer token", 2L, request);

        assertThat(target.getUserRole()).isEqualTo("admin");
        assertThat(detail.getUserRole()).isEqualTo("admin");
    }

    @Test
    void updateUserRole_rejectsSelfDowngrade() {
        mockAdmin();
        com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest request = new com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest();
        request.setUserRole("user");

        assertThatThrownBy(() -> adminService.updateUserRole("Bearer token", 1L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.FORBIDDEN_ERROR.getCode());
    }

    @Test
    void updateUserRole_rejectsInvalidRole() {
        mockAdmin();
        com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest request = new com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest();
        request.setUserRole("owner");

        assertThatThrownBy(() -> adminService.updateUserRole("Bearer token", 2L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
    }

    private void mockAdmin() {
        mockClaims(1L, "admin");
        when(userMapper.selectById(1L)).thenReturn(adminUser(1L));
    }

    private void mockClaims(Long userId, String userRole) {
        when(jwtUtils.resolveToken("Bearer token")).thenReturn("token");
        when(jwtUtils.parseToken("token")).thenReturn(new JwtUtils.JwtUserClaims(userId, "admin", userRole));
    }

    private User adminUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUserAccount("admin" + id);
        user.setUserName("管理员" + id);
        user.setUserRole("admin");
        user.setUserStatus(0);
        return user;
    }
}
