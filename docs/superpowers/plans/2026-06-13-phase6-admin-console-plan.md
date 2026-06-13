# Phase 6 Admin Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the basic admin console for administrator authorization, platform overview, user management, and admin-only frontend routes.

**Architecture:** Reuse the existing `user.user_role` and `user.user_status` fields instead of adding a new account system. Backend adds a focused `AdminService` and `AdminController` under `/api/admin`; frontend adds an `AdminLayout`, admin routes, admin data layer, and three admin pages. No schema changes are required.

**Tech Stack:** Spring Boot 3.5.x, Java 17, MyBatis-Plus, JUnit 5, Mockito, Vue 3, TypeScript, Pinia, Vue Router, Element Plus.

---

## File Structure

### Backend Files

- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/admin/AdminUserQueryRequest.java`
  - Query object for admin user list filters.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/admin/AdminUserRoleUpdateRequest.java`
  - Request body for role updates.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminOverviewVO.java`
  - Platform overview metrics.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminUserListItemVO.java`
  - User list row data.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminUserDetailVO.java`
  - User detail plus per-user usage metrics.
- Create: `backend/src/main/java/com/mianshiba/ai/service/AdminService.java`
  - Admin service interface.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/AdminServiceImpl.java`
  - Authorization, overview, user list/detail, status and role operations.
- Create: `backend/src/main/java/com/mianshiba/ai/controller/AdminController.java`
  - `/api/admin` controller.
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/AdminServiceImplTest.java`
  - Unit tests for authorization, overview, user management, and self-protection rules.
- Create: `backend/src/test/java/com/mianshiba/ai/controller/AdminControllerTest.java`
  - Controller routing tests.

### Frontend Files

- Create: `frontend/src/types/admin.ts`
  - Admin DTO/VO TypeScript interfaces.
- Create: `frontend/src/api/admin.ts`
  - Admin API functions.
- Create: `frontend/src/stores/admin.ts`
  - Admin Pinia setup store.
- Create: `frontend/src/layouts/AdminLayout.vue`
  - Admin shell with left navigation.
- Create: `frontend/src/views/admin/AdminDashboardPage.vue`
  - Platform overview cards.
- Create: `frontend/src/views/admin/AdminUserListPage.vue`
  - Searchable/filterable user list and status actions.
- Create: `frontend/src/views/admin/AdminUserDetailPage.vue`
  - User detail, usage metrics, role update, status actions.
- Modify: `frontend/src/router/index.ts`
  - Add admin routes and `requiresAdmin` guard.
- Modify: `frontend/src/layouts/MainLayout.vue`
  - Show admin entry only for `userRole === 'admin'`.

---

## Task 1: Backend Admin DTO/VO and Service Skeleton

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/admin/AdminUserQueryRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/admin/AdminUserRoleUpdateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminOverviewVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminUserListItemVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminUserDetailVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/AdminService.java`

- [ ] **Step 1: Create admin request DTOs**

Create `backend/src/main/java/com/mianshiba/ai/model/dto/admin/AdminUserQueryRequest.java`:

```java
package com.mianshiba.ai.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员用户查询请求")
public class AdminUserQueryRequest {

    @Schema(description = "关键词，匹配账号、昵称、邮箱")
    private String keyword;

    @Schema(description = "用户状态：0 正常，1 禁用")
    private Integer userStatus;

    @Schema(description = "用户角色：user/admin")
    private String userRole;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/dto/admin/AdminUserRoleUpdateRequest.java`:

```java
package com.mianshiba.ai.model.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "管理员更新用户角色请求")
public class AdminUserRoleUpdateRequest {

    @NotBlank(message = "用户角色不能为空")
    @Schema(description = "用户角色：user/admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String userRole;
}
```

- [ ] **Step 2: Create admin overview VO**

Create `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminOverviewVO.java`:

```java
package com.mianshiba.ai.model.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "管理员平台总览")
public class AdminOverviewVO {

    private Long totalUsers;
    private Long enabledUsers;
    private Long disabledUsers;
    private Long adminUsers;
    private Long resumeCount;
    private Long interviewCount;
    private Long completedInterviewCount;
    private Long applicationCount;
    private Long trainingPlanCount;
    private Long trainingAnswerCount;
    private Long trainingReviewCount;
}
```

- [ ] **Step 3: Create admin user list and detail VOs**

Create `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminUserListItemVO.java`:

```java
package com.mianshiba.ai.model.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理员用户列表项")
public class AdminUserListItemVO {

    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userRole;
    private Integer userStatus;
    private String email;
    private String targetPosition;
    private String techDirection;
    private String city;
    private LocalDateTime createTime;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/admin/AdminUserDetailVO.java`:

```java
package com.mianshiba.ai.model.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "管理员用户详情")
public class AdminUserDetailVO {

    private Long id;
    private String userAccount;
    private String userName;
    private String userAvatar;
    private String userRole;
    private Integer userStatus;
    private String email;
    private String phone;
    private String targetPosition;
    private String techDirection;
    private Integer workYears;
    private String city;
    private String jobStatus;
    private LocalDateTime createTime;
    private Long resumeCount;
    private Long interviewCount;
    private Long completedInterviewCount;
    private Long applicationCount;
    private Long trainingPlanCount;
    private Long trainingAnswerCount;
    private Long trainingReviewCount;
}
```

- [ ] **Step 4: Create admin service interface**

Create `backend/src/main/java/com/mianshiba/ai/service/AdminService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.admin.AdminUserQueryRequest;
import com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest;
import com.mianshiba.ai.model.vo.admin.AdminOverviewVO;
import com.mianshiba.ai.model.vo.admin.AdminUserDetailVO;
import com.mianshiba.ai.model.vo.admin.AdminUserListItemVO;

import java.util.List;

public interface AdminService {

    AdminOverviewVO getOverview(String authorizationHeader);

    List<AdminUserListItemVO> listUsers(String authorizationHeader, AdminUserQueryRequest request);

    AdminUserDetailVO getUserDetail(String authorizationHeader, Long userId);

    AdminUserDetailVO disableUser(String authorizationHeader, Long userId);

    AdminUserDetailVO enableUser(String authorizationHeader, Long userId);

    AdminUserDetailVO updateUserRole(String authorizationHeader, Long userId, AdminUserRoleUpdateRequest request);
}
```

- [ ] **Step 5: Run backend compile check**

Run from `backend/`:

```powershell
.\mvnw.cmd -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 6: Commit Task 1**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/dto/admin backend/src/main/java/com/mianshiba/ai/model/vo/admin backend/src/main/java/com/mianshiba/ai/service/AdminService.java
git commit -m "feat: add admin API contracts"
```

---

## Task 2: Backend Admin Authorization, Overview, List, and Detail

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/AdminServiceImpl.java`
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/AdminServiceImplTest.java`
- Uses existing mappers: `UserMapper`, `ResumeMapper`, `InterviewSessionMapper`, `JobApplicationMapper`, `TrainingPlanMapper`, `TrainingAnswerMapper`, `TrainingAnswerReviewMapper`

- [ ] **Step 1: Write failing service tests**

Create `backend/src/test/java/com/mianshiba/ai/service/impl/AdminServiceImplTest.java` with these tests first:

```java
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
```

- [ ] **Step 2: Run tests and verify failure**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=AdminServiceImplTest
```

Expected: compilation fails because `AdminServiceImpl` does not exist.

- [ ] **Step 3: Implement `AdminServiceImpl`**

Create `backend/src/main/java/com/mianshiba/ai/service/impl/AdminServiceImpl.java`:

```java
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
```

- [ ] **Step 4: Run Task 2 tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=AdminServiceImplTest
```

Expected: tests in Task 2 pass. Tests for status/role operations are not present yet.

- [ ] **Step 5: Commit Task 2**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/AdminServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/AdminServiceImplTest.java
git commit -m "feat: add admin overview and user queries"
```

---

## Task 3: Backend Admin Status and Role Mutation Tests

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/AdminServiceImpl.java`
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/AdminServiceImplTest.java`

- [ ] **Step 1: Add mutation regression tests**

Append these tests to `AdminServiceImplTest` before helper methods:

```java
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
```

- [ ] **Step 2: Run mutation regression tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=AdminServiceImplTest
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Confirm mutation method implementation**

Confirm the three mutation methods in `AdminServiceImpl` match this implementation:

```java
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
```

- [ ] **Step 4: Run Task 3 tests after confirmation**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=AdminServiceImplTest
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit Task 3**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/AdminServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/AdminServiceImplTest.java
git commit -m "feat: add admin user mutations"
```

---

## Task 4: Backend Admin Controller

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/controller/AdminController.java`
- Create: `backend/src/test/java/com/mianshiba/ai/controller/AdminControllerTest.java`

- [ ] **Step 1: Write failing controller test**

Create `backend/src/test/java/com/mianshiba/ai/controller/AdminControllerTest.java`:

```java
package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.model.dto.admin.AdminUserQueryRequest;
import com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest;
import com.mianshiba.ai.model.vo.admin.AdminOverviewVO;
import com.mianshiba.ai.model.vo.admin.AdminUserDetailVO;
import com.mianshiba.ai.model.vo.admin.AdminUserListItemVO;
import com.mianshiba.ai.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @Test
    void getOverview_returnsOverview() throws Exception {
        AdminOverviewVO overview = new AdminOverviewVO();
        overview.setTotalUsers(10L);
        when(adminService.getOverview("Bearer token")).thenReturn(overview);

        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalUsers").value(10));
    }

    @Test
    void listUsers_passesQuery() throws Exception {
        AdminUserListItemVO user = new AdminUserListItemVO();
        user.setId(2L);
        user.setUserAccount("candidate");
        when(adminService.listUsers(eq("Bearer token"), any(AdminUserQueryRequest.class))).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer token")
                        .param("keyword", "candidate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2));
    }

    @Test
    void getUserDetail_returnsDetail() throws Exception {
        AdminUserDetailVO detail = new AdminUserDetailVO();
        detail.setId(2L);
        when(adminService.getUserDetail("Bearer token", 2L)).thenReturn(detail);

        mockMvc.perform(get("/api/admin/users/2").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }

    @Test
    void disableUser_callsService() throws Exception {
        AdminUserDetailVO detail = new AdminUserDetailVO();
        detail.setId(2L);
        detail.setUserStatus(1);
        when(adminService.disableUser("Bearer token", 2L)).thenReturn(detail);

        mockMvc.perform(put("/api/admin/users/2/disable").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value(1));
    }

    @Test
    void enableUser_callsService() throws Exception {
        AdminUserDetailVO detail = new AdminUserDetailVO();
        detail.setId(2L);
        detail.setUserStatus(0);
        when(adminService.enableUser("Bearer token", 2L)).thenReturn(detail);

        mockMvc.perform(put("/api/admin/users/2/enable").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userStatus").value(0));
    }

    @Test
    void updateUserRole_callsService() throws Exception {
        AdminUserRoleUpdateRequest request = new AdminUserRoleUpdateRequest();
        request.setUserRole("admin");
        AdminUserDetailVO detail = new AdminUserDetailVO();
        detail.setId(2L);
        detail.setUserRole("admin");
        when(adminService.updateUserRole(eq("Bearer token"), eq(2L), any(AdminUserRoleUpdateRequest.class))).thenReturn(detail);

        mockMvc.perform(put("/api/admin/users/2/role")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userRole").value("admin"));

        verify(adminService).updateUserRole(eq("Bearer token"), eq(2L), any(AdminUserRoleUpdateRequest.class));
    }
}
```

- [ ] **Step 2: Run controller test and verify failure**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=AdminControllerTest
```

Expected: compilation fails because `AdminController` does not exist.

- [ ] **Step 3: Implement `AdminController`**

Create `backend/src/main/java/com/mianshiba/ai/controller/AdminController.java`:

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.admin.AdminUserQueryRequest;
import com.mianshiba.ai.model.dto.admin.AdminUserRoleUpdateRequest;
import com.mianshiba.ai.model.vo.admin.AdminOverviewVO;
import com.mianshiba.ai.model.vo.admin.AdminUserDetailVO;
import com.mianshiba.ai.model.vo.admin.AdminUserListItemVO;
import com.mianshiba.ai.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
@Tag(name = "管理员后台接口")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/overview")
    @Operation(summary = "平台总览")
    public BaseResponse<AdminOverviewVO> getOverview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(adminService.getOverview(authorizationHeader));
    }

    @GetMapping("/users")
    @Operation(summary = "管理员用户列表")
    public BaseResponse<List<AdminUserListItemVO>> listUsers(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            AdminUserQueryRequest request) {
        return ResultUtils.success(adminService.listUsers(authorizationHeader, request));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "管理员用户详情")
    public BaseResponse<AdminUserDetailVO> getUserDetail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminService.getUserDetail(authorizationHeader, id));
    }

    @PutMapping("/users/{id}/disable")
    @Operation(summary = "禁用用户")
    public BaseResponse<AdminUserDetailVO> disableUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminService.disableUser(authorizationHeader, id));
    }

    @PutMapping("/users/{id}/enable")
    @Operation(summary = "启用用户")
    public BaseResponse<AdminUserDetailVO> enableUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(adminService.enableUser(authorizationHeader, id));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "更新用户角色")
    public BaseResponse<AdminUserDetailVO> updateUserRole(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        return ResultUtils.success(adminService.updateUserRole(authorizationHeader, id, request));
    }
}
```

- [ ] **Step 4: Run backend tests for admin module**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=AdminServiceImplTest,AdminControllerTest
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit Task 4**

```powershell
git add backend/src/main/java/com/mianshiba/ai/controller/AdminController.java backend/src/test/java/com/mianshiba/ai/controller/AdminControllerTest.java
git commit -m "feat: expose admin console API"
```

---

## Task 5: Frontend Admin Data Layer, Routes, Guard, and Layout

**Files:**
- Create: `frontend/src/types/admin.ts`
- Create: `frontend/src/api/admin.ts`
- Create: `frontend/src/stores/admin.ts`
- Create: `frontend/src/layouts/AdminLayout.vue`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: Create frontend admin types**

Create `frontend/src/types/admin.ts`:

```ts
export interface AdminOverviewVO {
  totalUsers: number
  enabledUsers: number
  disabledUsers: number
  adminUsers: number
  resumeCount: number
  interviewCount: number
  completedInterviewCount: number
  applicationCount: number
  trainingPlanCount: number
  trainingAnswerCount: number
  trainingReviewCount: number
}

export interface AdminUserListItemVO {
  id: number
  userAccount: string
  userName: string
  userAvatar: string
  userRole: string
  userStatus: number
  email: string
  targetPosition: string
  techDirection: string
  city: string
  createTime: string
}

export interface AdminUserDetailVO extends AdminUserListItemVO {
  phone: string
  workYears: number
  jobStatus: string
  resumeCount: number
  interviewCount: number
  completedInterviewCount: number
  applicationCount: number
  trainingPlanCount: number
  trainingAnswerCount: number
  trainingReviewCount: number
}

export interface AdminUserQueryRequest {
  keyword?: string
  userStatus?: number
  userRole?: string
}

export interface AdminUserRoleUpdateRequest {
  userRole: string
}
```

- [ ] **Step 2: Create frontend admin API**

Create `frontend/src/api/admin.ts`:

```ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  AdminOverviewVO,
  AdminUserDetailVO,
  AdminUserListItemVO,
  AdminUserQueryRequest,
  AdminUserRoleUpdateRequest,
} from '@/types/admin'

export function getAdminOverview() {
  return request.get<BaseResponse<AdminOverviewVO>>('/api/admin/overview')
}

export function listAdminUsers(params?: AdminUserQueryRequest) {
  return request.get<BaseResponse<AdminUserListItemVO[]>>('/api/admin/users', { params })
}

export function getAdminUser(id: number) {
  return request.get<BaseResponse<AdminUserDetailVO>>(`/api/admin/users/${id}`)
}

export function disableAdminUser(id: number) {
  return request.put<BaseResponse<AdminUserDetailVO>>(`/api/admin/users/${id}/disable`)
}

export function enableAdminUser(id: number) {
  return request.put<BaseResponse<AdminUserDetailVO>>(`/api/admin/users/${id}/enable`)
}

export function updateAdminUserRole(id: number, data: AdminUserRoleUpdateRequest) {
  return request.put<BaseResponse<AdminUserDetailVO>>(`/api/admin/users/${id}/role`, data)
}
```

- [ ] **Step 3: Create frontend admin store**

Create `frontend/src/stores/admin.ts`:

```ts
import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  disableAdminUser,
  enableAdminUser,
  getAdminOverview,
  getAdminUser,
  listAdminUsers,
  updateAdminUserRole,
} from '@/api/admin'
import type { AdminOverviewVO, AdminUserDetailVO, AdminUserListItemVO, AdminUserQueryRequest } from '@/types/admin'

export const useAdminStore = defineStore('admin', () => {
  const overview = ref<AdminOverviewVO | null>(null)
  const users = ref<AdminUserListItemVO[]>([])
  const currentUser = ref<AdminUserDetailVO | null>(null)
  const loading = ref(false)

  async function fetchOverview() {
    loading.value = true
    try {
      const res = await getAdminOverview()
      if (res.data.code === 0) {
        overview.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUsers(query?: AdminUserQueryRequest) {
    loading.value = true
    try {
      const res = await listAdminUsers(query)
      if (res.data.code === 0) {
        users.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function fetchUser(id: number) {
    loading.value = true
    try {
      const res = await getAdminUser(id)
      if (res.data.code === 0) {
        currentUser.value = res.data.data
      }
    } finally {
      loading.value = false
    }
  }

  async function disableUser(id: number) {
    const res = await disableAdminUser(id)
    if (res.data.code === 0) {
      currentUser.value = res.data.data
      return res.data.data
    }
    return null
  }

  async function enableUser(id: number) {
    const res = await enableAdminUser(id)
    if (res.data.code === 0) {
      currentUser.value = res.data.data
      return res.data.data
    }
    return null
  }

  async function updateUserRole(id: number, userRole: string) {
    const res = await updateAdminUserRole(id, { userRole })
    if (res.data.code === 0) {
      currentUser.value = res.data.data
      return res.data.data
    }
    return null
  }

  return {
    overview,
    users,
    currentUser,
    loading,
    fetchOverview,
    fetchUsers,
    fetchUser,
    disableUser,
    enableUser,
    updateUserRole,
  }
})
```

- [ ] **Step 4: Create admin layout**

Create `frontend/src/layouts/AdminLayout.vue`:

```vue
<template>
  <div class="admin-layout">
    <aside class="admin-layout__sidebar">
      <router-link to="/admin" class="admin-layout__brand">面试吧 Admin</router-link>
      <nav class="admin-layout__nav">
        <router-link to="/admin" class="admin-layout__link">平台总览</router-link>
        <router-link to="/admin/users" class="admin-layout__link">用户管理</router-link>
        <router-link to="/" class="admin-layout__link admin-layout__link--ghost">返回用户端</router-link>
      </nav>
    </aside>
    <section class="admin-layout__content">
      <header class="admin-layout__header">
        <span>管理员后台</span>
        <strong>{{ userStore.userInfo?.userName || userStore.userInfo?.userAccount || 'admin' }}</strong>
      </header>
      <main class="admin-layout__main">
        <slot />
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px 1fr;
  background: var(--nb-bg);
}

.admin-layout__sidebar {
  padding: 24px;
  background: var(--nb-card);
  border-right: var(--nb-border);
  box-shadow: 6px 0 0 rgba(0, 0, 0, 0.08);
}

.admin-layout__brand {
  display: block;
  margin-bottom: 32px;
  color: var(--nb-text);
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 800;
  text-decoration: none;
  text-shadow: 2px 2px 0 var(--nb-primary);
}

.admin-layout__nav {
  display: grid;
  gap: 12px;
}

.admin-layout__link {
  padding: 12px 14px;
  color: var(--nb-text);
  text-decoration: none;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: #fff;
  box-shadow: var(--nb-shadow-sm);
  font-weight: 700;
}

.admin-layout__link.router-link-active {
  background: var(--nb-primary);
  color: #fff;
}

.admin-layout__link--ghost {
  margin-top: 16px;
  background: transparent;
}

.admin-layout__content {
  min-width: 0;
}

.admin-layout__header {
  height: 64px;
  padding: 0 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--nb-card);
  border-bottom: var(--nb-border);
  font-weight: 700;
}

.admin-layout__main {
  padding: 32px;
}

@media (max-width: 768px) {
  .admin-layout {
    grid-template-columns: 1fr;
  }

  .admin-layout__sidebar {
    border-right: 0;
    border-bottom: var(--nb-border);
    box-shadow: 0 4px 0 rgba(0, 0, 0, 0.08);
  }

  .admin-layout__nav {
    grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  }
}
</style>
```

- [ ] **Step 5: Add admin routes and guard**

Modify `frontend/src/router/index.ts`:

Add these routes before the closing `]` of `routes`:

```ts
    {
      path: '/admin',
      name: 'AdminDashboard',
      component: () => import('@/views/admin/AdminDashboardPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/users',
      name: 'AdminUsers',
      component: () => import('@/views/admin/AdminUserListPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/users/:id',
      name: 'AdminUserDetail',
      component: () => import('@/views/admin/AdminUserDetailPage.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
```

Add this guard block after the existing `requiresAuth` block and before the `public` block:

```ts
  // 非管理员访问管理员路由，回到用户端首页
  if (to.meta.requiresAdmin && userStore.userInfo?.userRole !== 'admin') {
    next('/')
    return
  }
```

- [ ] **Step 6: Add admin entry in `MainLayout.vue`**

Add this nav link after the “训练中心” link:

```vue
          <router-link v-if="userStore.userInfo?.userRole === 'admin'" to="/admin" class="main-layout__nav-link">
            管理后台
          </router-link>
```

- [ ] **Step 7: Create temporary admin page shells for type-check**

Create `frontend/src/views/admin/AdminDashboardPage.vue`:

```vue
<template>
  <AdminLayout>
    <div>平台总览</div>
  </AdminLayout>
</template>

<script setup lang="ts">
import AdminLayout from '@/layouts/AdminLayout.vue'
</script>
```

Create `frontend/src/views/admin/AdminUserListPage.vue`:

```vue
<template>
  <AdminLayout>
    <div>用户管理</div>
  </AdminLayout>
</template>

<script setup lang="ts">
import AdminLayout from '@/layouts/AdminLayout.vue'
</script>
```

Create `frontend/src/views/admin/AdminUserDetailPage.vue`:

```vue
<template>
  <AdminLayout>
    <div>用户详情</div>
  </AdminLayout>
</template>

<script setup lang="ts">
import AdminLayout from '@/layouts/AdminLayout.vue'
</script>
```

- [ ] **Step 8: Run frontend type-check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: type-check passes.

- [ ] **Step 9: Commit Task 5**

```powershell
git add frontend/src/types/admin.ts frontend/src/api/admin.ts frontend/src/stores/admin.ts frontend/src/layouts/AdminLayout.vue frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/views/admin
git commit -m "feat: add admin frontend shell"
```

---

## Task 6: Frontend Admin Dashboard and User Management Pages

**Files:**
- Modify: `frontend/src/views/admin/AdminDashboardPage.vue`
- Modify: `frontend/src/views/admin/AdminUserListPage.vue`
- Modify: `frontend/src/views/admin/AdminUserDetailPage.vue`

- [ ] **Step 1: Implement dashboard page**

Replace `frontend/src/views/admin/AdminDashboardPage.vue` with:

```vue
<template>
  <AdminLayout>
    <section class="admin-page">
      <div class="admin-page__header">
        <p class="admin-page__eyebrow">Admin Overview</p>
        <h1>平台总览</h1>
        <p>查看用户、简历、面试、投递和训练数据的核心运营指标。</p>
      </div>

      <el-skeleton v-if="adminStore.loading && !adminStore.overview" :rows="6" animated />
      <div v-else class="metric-grid">
        <NbCard v-for="metric in metrics" :key="metric.label" class="metric-card">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </NbCard>
      </div>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useAdminStore } from '@/stores/admin'

const adminStore = useAdminStore()

const metrics = computed(() => {
  const overview = adminStore.overview
  return [
    { label: '用户总数', value: overview?.totalUsers ?? 0 },
    { label: '正常用户', value: overview?.enabledUsers ?? 0 },
    { label: '禁用用户', value: overview?.disabledUsers ?? 0 },
    { label: '管理员', value: overview?.adminUsers ?? 0 },
    { label: '简历数量', value: overview?.resumeCount ?? 0 },
    { label: '面试数量', value: overview?.interviewCount ?? 0 },
    { label: '已完成面试', value: overview?.completedInterviewCount ?? 0 },
    { label: '投递记录', value: overview?.applicationCount ?? 0 },
    { label: '训练计划', value: overview?.trainingPlanCount ?? 0 },
    { label: '八股作答', value: overview?.trainingAnswerCount ?? 0 },
    { label: 'AI 批改', value: overview?.trainingReviewCount ?? 0 },
  ]
})

onMounted(() => {
  adminStore.fetchOverview()
})
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 24px;
}

.admin-page__header h1 {
  margin: 0;
  font-size: 36px;
}

.admin-page__header p {
  margin: 8px 0 0;
  color: var(--nb-muted);
}

.admin-page__eyebrow {
  margin: 0 0 6px !important;
  color: var(--nb-primary) !important;
  font-weight: 800;
  text-transform: uppercase;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 18px;
}

.metric-card {
  display: grid;
  gap: 10px;
}

.metric-card span {
  color: var(--nb-muted);
  font-weight: 700;
}

.metric-card strong {
  font-size: 34px;
  line-height: 1;
}
</style>
```

- [ ] **Step 2: Implement user list page**

Replace `frontend/src/views/admin/AdminUserListPage.vue` with:

```vue
<template>
  <AdminLayout>
    <section class="admin-page">
      <div class="admin-page__header">
        <div>
          <p class="admin-page__eyebrow">Users</p>
          <h1>用户管理</h1>
        </div>
        <el-button type="primary" @click="loadUsers">刷新</el-button>
      </div>

      <NbCard class="filter-card">
        <el-input v-model="query.keyword" placeholder="搜索账号、昵称、邮箱" clearable @keyup.enter="loadUsers" />
        <el-select v-model="query.userStatus" placeholder="状态" clearable>
          <el-option label="正常" :value="0" />
          <el-option label="禁用" :value="1" />
        </el-select>
        <el-select v-model="query.userRole" placeholder="角色" clearable>
          <el-option label="普通用户" value="user" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-button type="primary" @click="loadUsers">查询</el-button>
      </NbCard>

      <NbCard>
        <el-table :data="adminStore.users" v-loading="adminStore.loading" empty-text="暂无用户">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="userAccount" label="账号" min-width="140" />
          <el-table-column prop="userName" label="昵称" min-width="140" />
          <el-table-column prop="email" label="邮箱" min-width="180" />
          <el-table-column prop="userRole" label="角色" width="110">
            <template #default="{ row }">
              <el-tag :type="row.userRole === 'admin' ? 'danger' : 'info'">{{ roleLabel(row.userRole) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="userStatus" label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.userStatus === 0 ? 'success' : 'warning'">{{ statusLabel(row.userStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="targetPosition" label="目标岗位" min-width="140" />
          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="router.push(`/admin/users/${row.id}`)">详情</el-button>
              <el-button v-if="row.userStatus === 0" size="small" type="warning" @click="disableUser(row.id)">禁用</el-button>
              <el-button v-else size="small" type="success" @click="enableUser(row.id)">启用</el-button>
            </template>
          </el-table-column>
        </el-table>
      </NbCard>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useAdminStore } from '@/stores/admin'
import type { AdminUserQueryRequest } from '@/types/admin'

const router = useRouter()
const adminStore = useAdminStore()
const query = reactive<AdminUserQueryRequest>({})

function roleLabel(role: string) {
  return role === 'admin' ? '管理员' : '普通用户'
}

function statusLabel(status: number) {
  return status === 0 ? '正常' : '禁用'
}

async function loadUsers() {
  await adminStore.fetchUsers({ ...query })
}

async function disableUser(id: number) {
  await adminStore.disableUser(id)
  ElMessage.success('已禁用用户')
  await loadUsers()
}

async function enableUser(id: number) {
  await adminStore.enableUser(id)
  ElMessage.success('已启用用户')
  await loadUsers()
}

onMounted(loadUsers)
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.admin-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-page__header h1 {
  margin: 0;
  font-size: 34px;
}

.admin-page__eyebrow {
  margin: 0 0 6px;
  color: var(--nb-primary);
  font-weight: 800;
  text-transform: uppercase;
}

.filter-card {
  display: grid;
  grid-template-columns: minmax(240px, 1fr) 160px 160px auto;
  gap: 12px;
  align-items: center;
}

@media (max-width: 900px) {
  .filter-card {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 3: Implement user detail page**

Replace `frontend/src/views/admin/AdminUserDetailPage.vue` with:

```vue
<template>
  <AdminLayout>
    <section class="admin-page">
      <div class="admin-page__header">
        <div>
          <p class="admin-page__eyebrow">User Detail</p>
          <h1>{{ user?.userName || user?.userAccount || '用户详情' }}</h1>
        </div>
        <el-button @click="router.push('/admin/users')">返回列表</el-button>
      </div>

      <el-skeleton v-if="adminStore.loading && !user" :rows="6" animated />
      <template v-else-if="user">
        <NbCard class="profile-card">
          <div>
            <span>账号</span>
            <strong>{{ user.userAccount }}</strong>
          </div>
          <div>
            <span>角色</span>
            <el-select v-model="selectedRole" @change="changeRole">
              <el-option label="普通用户" value="user" />
              <el-option label="管理员" value="admin" />
            </el-select>
          </div>
          <div>
            <span>状态</span>
            <el-tag :type="user.userStatus === 0 ? 'success' : 'warning'">{{ user.userStatus === 0 ? '正常' : '禁用' }}</el-tag>
          </div>
          <div class="profile-card__actions">
            <el-button v-if="user.userStatus === 0" type="warning" @click="disableUser">禁用用户</el-button>
            <el-button v-else type="success" @click="enableUser">启用用户</el-button>
          </div>
        </NbCard>

        <div class="metric-grid">
          <NbCard v-for="metric in metrics" :key="metric.label" class="metric-card">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </NbCard>
        </div>

        <NbCard class="info-card">
          <h2>基础信息</h2>
          <dl>
            <dt>邮箱</dt><dd>{{ user.email || '-' }}</dd>
            <dt>手机号</dt><dd>{{ user.phone || '-' }}</dd>
            <dt>目标岗位</dt><dd>{{ user.targetPosition || '-' }}</dd>
            <dt>技术方向</dt><dd>{{ user.techDirection || '-' }}</dd>
            <dt>工作年限</dt><dd>{{ user.workYears ?? '-' }}</dd>
            <dt>城市</dt><dd>{{ user.city || '-' }}</dd>
            <dt>求职状态</dt><dd>{{ user.jobStatus || '-' }}</dd>
            <dt>注册时间</dt><dd>{{ user.createTime || '-' }}</dd>
          </dl>
        </NbCard>
      </template>
    </section>
  </AdminLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from '@/layouts/AdminLayout.vue'
import NbCard from '@/components/NbCard.vue'
import { useAdminStore } from '@/stores/admin'

const route = useRoute()
const router = useRouter()
const adminStore = useAdminStore()
const selectedRole = ref('user')

const userId = computed(() => Number(route.params.id))
const user = computed(() => adminStore.currentUser)

const metrics = computed(() => {
  const current = user.value
  return [
    { label: '简历', value: current?.resumeCount ?? 0 },
    { label: '面试', value: current?.interviewCount ?? 0 },
    { label: '已完成面试', value: current?.completedInterviewCount ?? 0 },
    { label: '投递', value: current?.applicationCount ?? 0 },
    { label: '训练计划', value: current?.trainingPlanCount ?? 0 },
    { label: '八股作答', value: current?.trainingAnswerCount ?? 0 },
    { label: 'AI 批改', value: current?.trainingReviewCount ?? 0 },
  ]
})

watch(user, (value) => {
  if (value) {
    selectedRole.value = value.userRole
  }
})

async function loadUser() {
  await adminStore.fetchUser(userId.value)
}

async function changeRole(role: string) {
  await adminStore.updateUserRole(userId.value, role)
  ElMessage.success('角色已更新')
}

async function disableUser() {
  await adminStore.disableUser(userId.value)
  ElMessage.success('已禁用用户')
}

async function enableUser() {
  await adminStore.enableUser(userId.value)
  ElMessage.success('已启用用户')
}

onMounted(loadUser)
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.admin-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.admin-page__header h1 {
  margin: 0;
  font-size: 34px;
}

.admin-page__eyebrow {
  margin: 0 0 6px;
  color: var(--nb-primary);
  font-weight: 800;
  text-transform: uppercase;
}

.profile-card {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 16px;
  align-items: center;
}

.profile-card span,
.metric-card span {
  display: block;
  margin-bottom: 6px;
  color: var(--nb-muted);
  font-weight: 700;
}

.profile-card strong,
.metric-card strong {
  font-size: 26px;
}

.profile-card__actions {
  display: flex;
  justify-content: flex-end;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.info-card h2 {
  margin: 0 0 16px;
}

.info-card dl {
  display: grid;
  grid-template-columns: 120px 1fr;
  gap: 12px 18px;
  margin: 0;
}

.info-card dt {
  color: var(--nb-muted);
  font-weight: 700;
}

.info-card dd {
  margin: 0;
}

@media (max-width: 900px) {
  .profile-card {
    grid-template-columns: 1fr;
  }

  .profile-card__actions {
    justify-content: flex-start;
  }
}
```

- [ ] **Step 4: Run frontend checks**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both commands pass. Vite may print a chunk-size warning; that warning is acceptable if the command exits successfully.

- [ ] **Step 5: Commit Task 6**

```powershell
git add frontend/src/views/admin/AdminDashboardPage.vue frontend/src/views/admin/AdminUserListPage.vue frontend/src/views/admin/AdminUserDetailPage.vue
git commit -m "feat: add admin console pages"
```

---

## Task 7: Full Verification

**Files:**
- No new files.
- Verify all modified backend and frontend files.

- [ ] **Step 1: Run backend admin tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=AdminServiceImplTest,AdminControllerTest
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run backend full tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run frontend checks**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both commands complete successfully.

- [ ] **Step 4: Inspect final diff**

Run from repo root:

```powershell
git status --short
git diff --stat HEAD
```

Expected: only intentional Phase 6 admin console changes are present.

- [ ] **Step 5: Commit verification fixes if files changed**

If verification required small fixes, commit them:

```powershell
git add backend frontend
git commit -m "fix: stabilize admin console"
```

If no files changed, do not create an empty commit.

---

## Implementation Notes

- Keep admin authorization inside `AdminServiceImpl.requireAdmin()` so every controller method gets one consistent security path.
- Do not add a new table, migration, or admin registration flow.
- Do not introduce RBAC abstractions.
- Use `NOT_LOGIN_ERROR` for missing/invalid login, `FORBIDDEN_ERROR` for disabled account and self-protection rules, `NO_AUTH_ERROR` for normal users accessing admin features.
- Keep route guard simple: `requiresAdmin` checks `userInfo.userRole === 'admin'` after `fetchCurrentUser()` recovery.
- Preserve existing Neubrutalism variables and component style.
