# Phase 3 Application And Report Charts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Phase 3 minimum loop: job application tracking with todos, plus ECharts visualizations for enhanced interview reports.

**Architecture:** Add a new Spring Boot application module with two tables (`job_application`, `application_todo`) and standard Controller → Service → Mapper → Entity/DTO/VO layers. Add a frontend application module with TypeScript types, API functions, Pinia store, three pages, and route entries. Add a small reusable `BaseChart.vue` wrapper and use existing Phase 2 review APIs for radar, trend, and skill-gap charts.

**Tech Stack:** Java 17, Spring Boot 3.5.x, MyBatis-Plus, MySQL, JUnit 5, Mockito, Vue 3, TypeScript, Pinia, Vue Router, Element Plus, ECharts.

---

## File Structure

### Backend Files

- Modify: `backend/src/main/resources/sql/init.sql`
  - Adds `job_application` and `application_todo` tables with indexes.
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`
  - Adds application-specific not-found errors.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobApplication.java`
  - MyBatis-Plus entity for one application record.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/ApplicationTodo.java`
  - MyBatis-Plus entity for application/global todos.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/JobApplicationMapper.java`
  - Mapper for `job_application`.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/ApplicationTodoMapper.java`
  - Mapper for `application_todo`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationCreateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationUpdateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationStatusUpdateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationListQueryRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationTodoCreateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationTodoUpdateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationTodoQueryRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/application/JobApplicationVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/application/ApplicationTodoVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/application/ApplicationStatsVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/ApplicationService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/controller/ApplicationController.java`
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/ApplicationServiceImplTest.java`
- Create: `backend/src/test/java/com/mianshiba/ai/controller/ApplicationControllerTest.java`
- Create: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase3Test.java`

### Frontend Files

- Modify: `frontend/package.json`
  - Adds `echarts` dependency.
- Modify: `frontend/package-lock.json`
  - Updated by `npm install echarts`.
- Create: `frontend/src/types/application.ts`
  - Types for application records, todos, stats, and query DTOs.
- Create: `frontend/src/api/application.ts`
  - API functions for `/api/application`.
- Create: `frontend/src/stores/application.ts`
  - Pinia store for application list/detail/todos.
- Create: `frontend/src/components/charts/BaseChart.vue`
  - Reusable ECharts wrapper.
- Create: `frontend/src/utils/charts/reviewCharts.ts`
  - Pure helpers that build ECharts options from Phase 2 review data.
- Modify: `frontend/src/router/index.ts`
  - Adds `/applications`, `/applications/new`, `/applications/:id`, `/applications/todos`.
- Create: `frontend/src/views/application/ApplicationListPage.vue`
- Create: `frontend/src/views/application/ApplicationEditPage.vue`
- Create: `frontend/src/views/application/ApplicationDetailPage.vue`
- Create: `frontend/src/views/application/ApplicationTodoPage.vue`
- Modify: `frontend/src/views/interview/InterviewReportPage.vue`
  - Adds single-report radar chart.
- Modify: `frontend/src/views/analytics/AnalyticsOverviewPage.vue`
  - Replaces progress-only review analytics with ECharts radar/line/bar charts.
- Modify: `frontend/src/layouts/MainLayout.vue`
  - Adds navigation entry for applications.

---

## Task 1: Backend Schema, Error Codes, Entities, And Mappers

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/JobApplication.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/ApplicationTodo.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/JobApplicationMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/ApplicationTodoMapper.java`
- Create: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase3Test.java`

- [ ] **Step 1: Add SQL regression test**

Create `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase3Test.java`:

```java
package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlPhase3Test {

    @Test
    void initSql_containsApplicationTables() throws IOException {
        String sql = new String(
                getClass().getClassLoader().getResourceAsStream("sql/init.sql").readAllBytes(),
                StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS job_application");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS application_todo");
        assertThat(sql).contains("idx_application_user_status");
        assertThat(sql).contains("idx_todo_user_completed_due");
    }
}
```

- [ ] **Step 2: Run failing SQL test**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase3Test
```

Expected: FAIL because `job_application` and `application_todo` do not exist in `init.sql` yet.

- [ ] **Step 3: Add Phase 3 tables**

Append after `job_favorite` table in `backend/src/main/resources/sql/init.sql`:

```sql
CREATE TABLE IF NOT EXISTS job_application (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '投递记录 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  job_id BIGINT DEFAULT NULL COMMENT '关联职位 id',
  resume_id BIGINT DEFAULT NULL COMMENT '关联简历 id',
  company_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '公司名',
  job_title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '岗位名',
  source VARCHAR(64) NOT NULL DEFAULT '' COMMENT '投递渠道',
  status VARCHAR(32) NOT NULL DEFAULT 'pending_submit' COMMENT 'pending_submit/submitted/hr_contact/written_test/first_interview/second_interview/final_interview/offer/rejected/withdrawn',
  applied_at DATETIME DEFAULT NULL COMMENT '投递时间',
  next_event_at DATETIME DEFAULT NULL COMMENT '下一事件时间',
  salary_range VARCHAR(64) NOT NULL DEFAULT '' COMMENT '薪资范围',
  location VARCHAR(64) NOT NULL DEFAULT '' COMMENT '工作城市',
  contact_name VARCHAR(64) NOT NULL DEFAULT '' COMMENT '联系人',
  contact_info VARCHAR(128) NOT NULL DEFAULT '' COMMENT '联系方式',
  notes TEXT DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_application_user_status (user_id, status),
  KEY idx_application_user_next_event (user_id, next_event_at),
  KEY idx_application_job_id (job_id),
  KEY idx_application_resume_id (resume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求职投递记录表';

CREATE TABLE IF NOT EXISTS application_todo (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '投递待办 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  application_id BIGINT DEFAULT NULL COMMENT '关联投递记录 id，空表示全局待办',
  title VARCHAR(128) NOT NULL COMMENT '待办标题',
  description TEXT DEFAULT NULL COMMENT '待办说明',
  priority VARCHAR(16) NOT NULL DEFAULT 'medium' COMMENT 'low/medium/high',
  due_at DATETIME DEFAULT NULL COMMENT '截止时间',
  completed TINYINT NOT NULL DEFAULT 0 COMMENT '是否完成：0-未完成，1-已完成',
  completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_todo_user_completed_due (user_id, completed, due_at),
  KEY idx_todo_application_id (application_id),
  KEY idx_todo_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求职投递待办表';
```

- [ ] **Step 4: Add error codes**

Modify `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java` before `ANALYTICS_ERROR`:

```java
    /**
     * 投递记录不存在
     */
    APPLICATION_NOT_FOUND_ERROR(40430, "投递记录不存在"),

    /**
     * 投递待办不存在
     */
    APPLICATION_TODO_NOT_FOUND_ERROR(40431, "投递待办不存在"),
```

- [ ] **Step 5: Add entities**

Create `backend/src/main/java/com/mianshiba/ai/model/entity/JobApplication.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("job_application")
public class JobApplication implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long jobId;
    private Long resumeId;
    private String companyName;
    private String jobTitle;
    private String source;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime nextEventAt;
    private String salaryRange;
    private String location;
    private String contactName;
    private String contactInfo;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/entity/ApplicationTodo.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("application_todo")
public class ApplicationTodo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long applicationId;
    private String title;
    private String description;
    private String priority;
    private LocalDateTime dueAt;
    private Integer completed;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

- [ ] **Step 6: Add mappers**

Create `backend/src/main/java/com/mianshiba/ai/mapper/JobApplicationMapper.java`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.JobApplication;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface JobApplicationMapper extends BaseMapper<JobApplication> {
}
```

Create `backend/src/main/java/com/mianshiba/ai/mapper/ApplicationTodoMapper.java`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationTodoMapper extends BaseMapper<ApplicationTodo> {
}
```

- [ ] **Step 7: Run SQL test and compile**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase3Test
.\mvnw.cmd clean package -DskipTests
```

Expected: both commands pass.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java backend/src/main/java/com/mianshiba/ai/model/entity/JobApplication.java backend/src/main/java/com/mianshiba/ai/model/entity/ApplicationTodo.java backend/src/main/java/com/mianshiba/ai/mapper/JobApplicationMapper.java backend/src/main/java/com/mianshiba/ai/mapper/ApplicationTodoMapper.java backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase3Test.java
git commit -m "feat: add application tracking schema"
```

---

## Task 2: Backend DTOs, VOs, Service, And Controller

**Files:**
- Create: all `backend/src/main/java/com/mianshiba/ai/model/dto/application/*.java`
- Create: all `backend/src/main/java/com/mianshiba/ai/model/vo/application/*.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/ApplicationService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/controller/ApplicationController.java`

- [ ] **Step 1: Add DTO classes**

Create `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationCreateRequest.java`:

```java
package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationCreateRequest {
    private Long jobId;
    private Long resumeId;
    @NotBlank(message = "公司名不能为空")
    @Size(max = 128, message = "公司名最长 128 个字符")
    private String companyName;
    @NotBlank(message = "岗位名不能为空")
    @Size(max = 128, message = "岗位名最长 128 个字符")
    private String jobTitle;
    private String source;
    private String status;
    private LocalDateTime appliedAt;
    private LocalDateTime nextEventAt;
    private String salaryRange;
    private String location;
    private String contactName;
    private String contactInfo;
    private String notes;
}
```

Create `ApplicationUpdateRequest.java` with the same fields except validation annotations are omitted so partial updates are allowed.

Create `ApplicationStatusUpdateRequest.java`:

```java
package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationStatusUpdateRequest {
    @NotBlank(message = "状态不能为空")
    private String status;
}
```

Create `ApplicationListQueryRequest.java`:

```java
package com.mianshiba.ai.model.dto.application;

import lombok.Data;

@Data
public class ApplicationListQueryRequest {
    private String keyword;
    private String status;
    private String location;
    private String source;
    private Long jobId;
    private Long resumeId;
}
```

Create `ApplicationTodoCreateRequest.java`:

```java
package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationTodoCreateRequest {
    @NotBlank(message = "待办标题不能为空")
    @Size(max = 128, message = "待办标题最长 128 个字符")
    private String title;
    private String description;
    private String priority;
    private LocalDateTime dueAt;
}
```

Create `ApplicationTodoUpdateRequest.java` with `title`, `description`, `priority`, `dueAt` fields.

Create `ApplicationTodoQueryRequest.java`:

```java
package com.mianshiba.ai.model.dto.application;

import lombok.Data;

@Data
public class ApplicationTodoQueryRequest {
    private Long applicationId;
    private Boolean completed;
    private String priority;
    private Boolean overdue;
}
```

- [ ] **Step 2: Add VO classes**

Create `backend/src/main/java/com/mianshiba/ai/model/vo/application/JobApplicationVO.java`:

```java
package com.mianshiba.ai.model.vo.application;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class JobApplicationVO {
    private Long id;
    private Long jobId;
    private Long resumeId;
    private String companyName;
    private String jobTitle;
    private String source;
    private String status;
    private String statusLabel;
    private LocalDateTime appliedAt;
    private LocalDateTime nextEventAt;
    private String salaryRange;
    private String location;
    private String contactName;
    private String contactInfo;
    private String notes;
    private Integer unfinishedTodoCount;
    private List<ApplicationTodoVO> todos;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

Create `ApplicationTodoVO.java`:

```java
package com.mianshiba.ai.model.vo.application;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ApplicationTodoVO {
    private Long id;
    private Long applicationId;
    private String applicationCompanyName;
    private String applicationJobTitle;
    private String title;
    private String description;
    private String priority;
    private String priorityLabel;
    private LocalDateTime dueAt;
    private Boolean completed;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

Create `ApplicationStatsVO.java`:

```java
package com.mianshiba.ai.model.vo.application;

import lombok.Data;

@Data
public class ApplicationStatsVO {
    private Long total;
    private Long pendingSubmit;
    private Long submitted;
    private Long interviewing;
    private Long offer;
    private Long closed;
}
```

- [ ] **Step 3: Add service interface**

Create `backend/src/main/java/com/mianshiba/ai/service/ApplicationService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.application.ApplicationCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationListQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationStatusUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationUpdateRequest;
import com.mianshiba.ai.model.vo.application.ApplicationStatsVO;
import com.mianshiba.ai.model.vo.application.ApplicationTodoVO;
import com.mianshiba.ai.model.vo.application.JobApplicationVO;

import java.util.List;

public interface ApplicationService {
    JobApplicationVO createApplication(String authorizationHeader, ApplicationCreateRequest request);
    List<JobApplicationVO> listApplications(String authorizationHeader, ApplicationListQueryRequest request);
    JobApplicationVO getApplication(String authorizationHeader, Long id);
    JobApplicationVO updateApplication(String authorizationHeader, Long id, ApplicationUpdateRequest request);
    JobApplicationVO updateStatus(String authorizationHeader, Long id, ApplicationStatusUpdateRequest request);
    void deleteApplication(String authorizationHeader, Long id);
    ApplicationStatsVO getStats(String authorizationHeader);
    ApplicationTodoVO createApplicationTodo(String authorizationHeader, Long applicationId, ApplicationTodoCreateRequest request);
    ApplicationTodoVO createGlobalTodo(String authorizationHeader, ApplicationTodoCreateRequest request);
    List<ApplicationTodoVO> listTodos(String authorizationHeader, ApplicationTodoQueryRequest request);
    ApplicationTodoVO updateTodo(String authorizationHeader, Long todoId, ApplicationTodoUpdateRequest request);
    ApplicationTodoVO completeTodo(String authorizationHeader, Long todoId);
    ApplicationTodoVO reopenTodo(String authorizationHeader, Long todoId);
    void deleteTodo(String authorizationHeader, Long todoId);
}
```

- [ ] **Step 4: Implement service**

Create `backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java`. Include these constants and helpers exactly:

```java
private static final Set<String> VALID_STATUSES = Set.of(
        "pending_submit", "submitted", "hr_contact", "written_test", "first_interview",
        "second_interview", "final_interview", "offer", "rejected", "withdrawn");
private static final Set<String> INTERVIEWING_STATUSES = Set.of(
        "hr_contact", "written_test", "first_interview", "second_interview", "final_interview");
private static final Set<String> VALID_PRIORITIES = Set.of("low", "medium", "high");

private Long resolveUserId(String authorizationHeader) {
    if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }
    JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(authorizationHeader.substring(7));
    return claims.userId();
}

private void validateStatus(String status) {
    if (!StringUtils.hasText(status) || !VALID_STATUSES.contains(status)) {
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "投递状态不合法");
    }
}

private String normalizeStatus(String status) {
    return StringUtils.hasText(status) ? status : "pending_submit";
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
```

Use constructor injection with these dependencies:

```java
private final JwtUtils jwtUtils;
private final JobApplicationMapper applicationMapper;
private final ApplicationTodoMapper todoMapper;
private final JobMapper jobMapper;
private final ResumeMapper resumeMapper;
```

Implementation rules:

- `createApplication`: resolve user, validate status, validate `jobId` exists if supplied, validate `resumeId` belongs to user if supplied, insert record, return detail.
- `listApplications`: query by user; optional status, keyword matching `companyName` or `jobTitle`, location, source, jobId, resumeId; order by `nextEventAt` ascending and `createTime` descending; set unfinished todo count for each row.
- `getApplication`: fetch by id and user; include todos.
- `updateApplication`: fetch by id and user; only overwrite fields from request; validate status if non-empty.
- `updateStatus`: validate and update status only.
- `deleteApplication`: delete application by id after ownership check; delete todos with same `applicationId` and `userId`.
- `getStats`: count total, pending_submit, submitted, interviewing statuses, offer, and closed statuses (`rejected`, `withdrawn`).
- Todo methods: all check user ownership; application-specific create checks application ownership; complete sets `completed=1` and `completedAt=now`; reopen sets `completed=0` and `completedAt=null`.

- [ ] **Step 5: Add controller**

Create `backend/src/main/java/com/mianshiba/ai/controller/ApplicationController.java`:

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.application.ApplicationCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationListQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationStatusUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoCreateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoQueryRequest;
import com.mianshiba.ai.model.dto.application.ApplicationTodoUpdateRequest;
import com.mianshiba.ai.model.dto.application.ApplicationUpdateRequest;
import com.mianshiba.ai.model.vo.application.ApplicationStatsVO;
import com.mianshiba.ai.model.vo.application.ApplicationTodoVO;
import com.mianshiba.ai.model.vo.application.JobApplicationVO;
import com.mianshiba.ai.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/application")
@Tag(name = "投递管理接口")
public class ApplicationController {

    private final ApplicationService applicationService;

    @PostMapping
    @Operation(summary = "创建投递记录")
    public BaseResponse<JobApplicationVO> create(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ApplicationCreateRequest request) {
        return ResultUtils.success(applicationService.createApplication(authorizationHeader, request));
    }

    @GetMapping
    @Operation(summary = "投递记录列表")
    public BaseResponse<List<JobApplicationVO>> list(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            ApplicationListQueryRequest request) {
        return ResultUtils.success(applicationService.listApplications(authorizationHeader, request));
    }

    @GetMapping("/stats")
    @Operation(summary = "投递统计")
    public BaseResponse<ApplicationStatsVO> stats(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        return ResultUtils.success(applicationService.getStats(authorizationHeader));
    }

    @GetMapping("/{id}")
    @Operation(summary = "投递详情")
    public BaseResponse<JobApplicationVO> detail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id) {
        return ResultUtils.success(applicationService.getApplication(authorizationHeader, id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新投递记录")
    public BaseResponse<JobApplicationVO> update(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id,
            @RequestBody ApplicationUpdateRequest request) {
        return ResultUtils.success(applicationService.updateApplication(authorizationHeader, id, request));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "更新投递状态")
    public BaseResponse<JobApplicationVO> updateStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id,
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        return ResultUtils.success(applicationService.updateStatus(authorizationHeader, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除投递记录")
    public BaseResponse<Void> delete(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long id) {
        applicationService.deleteApplication(authorizationHeader, id);
        return ResultUtils.success(null);
    }

    @PostMapping("/{applicationId}/todo")
    @Operation(summary = "创建投递待办")
    public BaseResponse<ApplicationTodoVO> createApplicationTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long applicationId,
            @Valid @RequestBody ApplicationTodoCreateRequest request) {
        return ResultUtils.success(applicationService.createApplicationTodo(authorizationHeader, applicationId, request));
    }

    @PostMapping("/todo")
    @Operation(summary = "创建全局求职待办")
    public BaseResponse<ApplicationTodoVO> createGlobalTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody ApplicationTodoCreateRequest request) {
        return ResultUtils.success(applicationService.createGlobalTodo(authorizationHeader, request));
    }

    @GetMapping("/todo")
    @Operation(summary = "待办中心列表")
    public BaseResponse<List<ApplicationTodoVO>> listTodos(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            ApplicationTodoQueryRequest request) {
        return ResultUtils.success(applicationService.listTodos(authorizationHeader, request));
    }

    @PutMapping("/todo/{todoId}")
    @Operation(summary = "更新待办")
    public BaseResponse<ApplicationTodoVO> updateTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long todoId,
            @RequestBody ApplicationTodoUpdateRequest request) {
        return ResultUtils.success(applicationService.updateTodo(authorizationHeader, todoId, request));
    }

    @PutMapping("/todo/{todoId}/complete")
    @Operation(summary = "完成待办")
    public BaseResponse<ApplicationTodoVO> completeTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long todoId) {
        return ResultUtils.success(applicationService.completeTodo(authorizationHeader, todoId));
    }

    @PutMapping("/todo/{todoId}/reopen")
    @Operation(summary = "取消完成待办")
    public BaseResponse<ApplicationTodoVO> reopenTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long todoId) {
        return ResultUtils.success(applicationService.reopenTodo(authorizationHeader, todoId));
    }

    @DeleteMapping("/todo/{todoId}")
    @Operation(summary = "删除待办")
    public BaseResponse<Void> deleteTodo(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable Long todoId) {
        applicationService.deleteTodo(authorizationHeader, todoId);
        return ResultUtils.success(null);
    }
}
```

- [ ] **Step 6: Compile**

Run from `backend/`:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/dto/application backend/src/main/java/com/mianshiba/ai/model/vo/application backend/src/main/java/com/mianshiba/ai/service/ApplicationService.java backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/ApplicationController.java
git commit -m "feat: add application management API"
```

---

## Task 3: Backend Application Tests

**Files:**
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/ApplicationServiceImplTest.java`
- Create: `backend/src/test/java/com/mianshiba/ai/controller/ApplicationControllerTest.java`

- [ ] **Step 1: Add service tests**

Create `ApplicationServiceImplTest.java` with Mockito tests for:

```java
@Test
void createApplication_insertsRecordForCurrentUser() { }

@Test
void updateStatus_rejectsInvalidStatus() { }

@Test
void deleteApplication_deletesOwnedTodos() { }

@Test
void createApplicationTodo_requiresOwnedApplication() { }

@Test
void completeTodo_setsCompletedAt() { }

@Test
void reopenTodo_clearsCompletedAt() { }
```

Use `JwtUtils.JwtUserClaims(1L, "user", "user")` when mocking token parsing.

- [ ] **Step 2: Add controller tests**

Create `ApplicationControllerTest.java` with `MockMvcBuilders.standaloneSetup(new ApplicationController(applicationService))` and tests for:

```java
@Test
void create_returnsSuccess() throws Exception { }

@Test
void listTodos_returnsSuccess() throws Exception { }

@Test
void completeTodo_returnsSuccess() throws Exception { }
```

- [ ] **Step 3: Run tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=ApplicationServiceImplTest,ApplicationControllerTest,InitSqlPhase3Test
```

Expected: tests pass.

- [ ] **Step 4: Commit**

```powershell
git add backend/src/test/java/com/mianshiba/ai/service/impl/ApplicationServiceImplTest.java backend/src/test/java/com/mianshiba/ai/controller/ApplicationControllerTest.java
git commit -m "test: cover application management API"
```

---

## Task 4: Frontend Application Types, API, Store, And Routes

**Files:**
- Create: `frontend/src/types/application.ts`
- Create: `frontend/src/api/application.ts`
- Create: `frontend/src/stores/application.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: Add TypeScript types**

Create `frontend/src/types/application.ts`:

```ts
export type ApplicationStatus =
  | 'pending_submit'
  | 'submitted'
  | 'hr_contact'
  | 'written_test'
  | 'first_interview'
  | 'second_interview'
  | 'final_interview'
  | 'offer'
  | 'rejected'
  | 'withdrawn'

export type TodoPriority = 'low' | 'medium' | 'high'

export interface ApplicationTodoVO {
  id: number
  applicationId?: number | null
  applicationCompanyName?: string
  applicationJobTitle?: string
  title: string
  description?: string
  priority: TodoPriority
  priorityLabel: string
  dueAt?: string | null
  completed: boolean
  completedAt?: string | null
  createTime: string
  updateTime: string
}

export interface JobApplicationVO {
  id: number
  jobId?: number | null
  resumeId?: number | null
  companyName: string
  jobTitle: string
  source: string
  status: ApplicationStatus
  statusLabel: string
  appliedAt?: string | null
  nextEventAt?: string | null
  salaryRange: string
  location: string
  contactName: string
  contactInfo: string
  notes?: string
  unfinishedTodoCount: number
  todos?: ApplicationTodoVO[]
  createTime: string
  updateTime: string
}

export interface ApplicationStatsVO {
  total: number
  pendingSubmit: number
  submitted: number
  interviewing: number
  offer: number
  closed: number
}

export interface ApplicationCreateRequest {
  jobId?: number | null
  resumeId?: number | null
  companyName: string
  jobTitle: string
  source?: string
  status?: ApplicationStatus
  appliedAt?: string | null
  nextEventAt?: string | null
  salaryRange?: string
  location?: string
  contactName?: string
  contactInfo?: string
  notes?: string
}

export type ApplicationUpdateRequest = Partial<ApplicationCreateRequest>

export interface ApplicationListQueryRequest {
  keyword?: string
  status?: ApplicationStatus | ''
  location?: string
  source?: string
  jobId?: number
  resumeId?: number
}

export interface ApplicationTodoCreateRequest {
  title: string
  description?: string
  priority?: TodoPriority
  dueAt?: string | null
}

export type ApplicationTodoUpdateRequest = Partial<ApplicationTodoCreateRequest>

export interface ApplicationTodoQueryRequest {
  applicationId?: number
  completed?: boolean
  priority?: TodoPriority | ''
  overdue?: boolean
}

export const APPLICATION_STATUS_OPTIONS: Array<{ label: string; value: ApplicationStatus }> = [
  { label: '待投递', value: 'pending_submit' },
  { label: '已投递', value: 'submitted' },
  { label: 'HR 沟通', value: 'hr_contact' },
  { label: '笔试', value: 'written_test' },
  { label: '一面', value: 'first_interview' },
  { label: '二面', value: 'second_interview' },
  { label: '终面', value: 'final_interview' },
  { label: 'Offer', value: 'offer' },
  { label: '拒绝', value: 'rejected' },
  { label: '放弃', value: 'withdrawn' },
]

export const TODO_PRIORITY_OPTIONS: Array<{ label: string; value: TodoPriority }> = [
  { label: '低', value: 'low' },
  { label: '中', value: 'medium' },
  { label: '高', value: 'high' },
]
```

- [ ] **Step 2: Add API functions**

Create `frontend/src/api/application.ts` with functions for every backend endpoint. Use `request` from `@/utils/request` and return `Promise<{ data: { code: number; data: T } }>` shape compatible with current store patterns.

- [ ] **Step 3: Add Pinia store**

Create `frontend/src/stores/application.ts` with state:

```ts
const applications = ref<JobApplicationVO[]>([])
const currentApplication = ref<JobApplicationVO | null>(null)
const todos = ref<ApplicationTodoVO[]>([])
const stats = ref<ApplicationStatsVO | null>(null)
const loading = ref(false)
```

Actions:

```ts
fetchApplications(query?: ApplicationListQueryRequest)
fetchStats()
fetchApplication(id: number)
createApplication(data: ApplicationCreateRequest)
updateApplication(id: number, data: ApplicationUpdateRequest)
updateApplicationStatus(id: number, status: ApplicationStatus)
deleteApplication(id: number)
createApplicationTodo(applicationId: number, data: ApplicationTodoCreateRequest)
createGlobalTodo(data: ApplicationTodoCreateRequest)
fetchTodos(query?: ApplicationTodoQueryRequest)
updateTodo(id: number, data: ApplicationTodoUpdateRequest)
completeTodo(id: number)
reopenTodo(id: number)
deleteTodo(id: number)
```

- [ ] **Step 4: Add routes**

Modify `frontend/src/router/index.ts` and insert before `/analytics`:

```ts
    {
      path: '/applications',
      name: 'ApplicationList',
      component: () => import('@/views/application/ApplicationListPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/applications/new',
      name: 'ApplicationNew',
      component: () => import('@/views/application/ApplicationEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/applications/todos',
      name: 'ApplicationTodos',
      component: () => import('@/views/application/ApplicationTodoPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/applications/:id',
      name: 'ApplicationDetail',
      component: () => import('@/views/application/ApplicationDetailPage.vue'),
      meta: { requiresAuth: true },
    },
```

- [ ] **Step 5: Add layout nav entry**

Modify `frontend/src/layouts/MainLayout.vue` and add a menu link near jobs/interviews:

```vue
<router-link to="/applications" class="main-layout__nav-link">投递管理</router-link>
```

Use the existing nav class already present in `MainLayout.vue`.

- [ ] **Step 6: Type-check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: pass after placeholder pages are created in Task 5; if routes fail because pages do not exist yet, create minimal stubs in Task 5 before running this command.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/types/application.ts frontend/src/api/application.ts frontend/src/stores/application.ts frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue
git commit -m "feat: add application frontend data layer"
```

---

## Task 5: Frontend Application Pages

**Files:**
- Create: `frontend/src/views/application/ApplicationListPage.vue`
- Create: `frontend/src/views/application/ApplicationEditPage.vue`
- Create: `frontend/src/views/application/ApplicationDetailPage.vue`
- Create: `frontend/src/views/application/ApplicationTodoPage.vue`

- [ ] **Step 1: Build list page**

Create `ApplicationListPage.vue` using `MainLayout`, `NbCard`, `NbButton`, `useApplicationStore`, `APPLICATION_STATUS_OPTIONS`, and Element Plus form controls.

Required behavior:

- Load `fetchStats()` and `fetchApplications()` on mount.
- Show stat cards for total, pendingSubmit, submitted, interviewing, offer, closed.
- Filter by keyword and status.
- Render cards with company, job title, status tag, location, nextEventAt, unfinishedTodoCount.
- Provide buttons to `/applications/new`, `/applications/todos`, and detail page.
- Provide inline status select that calls `updateApplicationStatus` and reloads list/stats.

- [ ] **Step 2: Build edit page**

Create `ApplicationEditPage.vue` for new application creation only.

Required form fields:

- companyName, jobTitle, source, status, appliedAt, nextEventAt, salaryRange, location, contactName, contactInfo, notes.

On submit:

```ts
const created = await applicationStore.createApplication(form)
if (created) {
  ElMessage.success('投递记录已创建')
  router.push(`/applications/${created.id}`)
}
```

- [ ] **Step 3: Build detail page**

Create `ApplicationDetailPage.vue`.

Required behavior:

- Load detail by route `id`.
- Show application fields and notes.
- Show status select with save button.
- Show todo list from `currentApplication.todos`.
- Provide inline todo creation form with title, priority, dueAt.
- Complete/reopen todos and reload detail.
- Link to related job if `jobId` exists: `/job/${jobId}`.
- Link to related resume if `resumeId` exists: `/resume/${resumeId}/preview`.

- [ ] **Step 4: Build todo center page**

Create `ApplicationTodoPage.vue`.

Required behavior:

- Load todos on mount.
- Filter by completed, priority, overdue.
- Show applicationCompanyName/applicationJobTitle when present.
- Complete/reopen/delete todos and reload.
- Provide global todo creation form.

- [ ] **Step 5: Type-check and build**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both pass.

- [ ] **Step 6: Commit**

```powershell
git add frontend/src/views/application
git commit -m "feat: add application management pages"
```

---

## Task 6: ECharts Dependency And BaseChart

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/src/components/charts/BaseChart.vue`
- Create: `frontend/src/utils/charts/reviewCharts.ts`

- [ ] **Step 1: Install ECharts**

Run from `frontend/`:

```powershell
npm install echarts
```

Expected: `package.json` includes `echarts` and `package-lock.json` changes.

- [ ] **Step 2: Create BaseChart**

Create `frontend/src/components/charts/BaseChart.vue`:

```vue
<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'

const props = defineProps<{
  option: EChartsOption
  height?: string
}>()

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined
let resizeObserver: ResizeObserver | undefined

function resizeChart() {
  chart?.resize()
}

onMounted(() => {
  if (!chartEl.value) return
  chart = echarts.init(chartEl.value)
  chart.setOption(props.option)
  resizeObserver = new ResizeObserver(resizeChart)
  resizeObserver.observe(chartEl.value)
})

watch(
  () => props.option,
  (option) => {
    chart?.setOption(option, true)
  },
  { deep: true },
)

onUnmounted(() => {
  resizeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div ref="chartEl" class="base-chart" :style="{ height: height || '320px' }" />
</template>

<style scoped>
.base-chart {
  width: 100%;
  min-height: 240px;
}
</style>
```

- [ ] **Step 3: Create chart option helpers**

Create `frontend/src/utils/charts/reviewCharts.ts`:

```ts
import type { EChartsOption } from 'echarts'

const RADAR_LABELS: Record<string, string> = {
  accuracy: '技术准确性',
  clarity: '表达清晰度',
  depth: '项目深度',
  matching: '岗位匹配度',
  systemDesign: '系统设计',
}

export function radarLabel(key: string) {
  return RADAR_LABELS[key] ?? key
}

export function buildRadarOption(radar: Record<string, number>, title = '能力雷达'): EChartsOption {
  const keys = Object.keys(radar)
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 16 } },
    tooltip: {},
    radar: {
      indicator: keys.map((key) => ({ name: radarLabel(key), max: 100 })),
      radius: '62%',
    },
    series: [
      {
        type: 'radar',
        data: [{ value: keys.map((key) => radar[key]), name: title }],
        areaStyle: { opacity: 0.18 },
      },
    ],
  }
}

export function buildScoreTrendOption(items: Array<Record<string, string | number>>): EChartsOption {
  return {
    title: { text: '近期面试分数', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: items.map((item) => String(item.date)) },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{ type: 'line', smooth: true, data: items.map((item) => Number(item.score)), itemStyle: { color: '#6C5CE7' } }],
  }
}

export function buildSkillGapOption(items: Array<Record<string, string>>): EChartsOption {
  return {
    title: { text: 'Top 技能缺口', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: items.map((item) => item.name), axisLabel: { interval: 0, rotate: 24 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: items.map((item) => Number(item.count || 0)), itemStyle: { color: '#FDCB6E' } }],
  }
}
```

- [ ] **Step 4: Type-check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: pass.

- [ ] **Step 5: Commit**

```powershell
git add frontend/package.json frontend/package-lock.json frontend/src/components/charts/BaseChart.vue frontend/src/utils/charts/reviewCharts.ts
git commit -m "feat: add echarts chart foundation"
```

---

## Task 7: Report Page Radar Chart

**Files:**
- Modify: `frontend/src/views/interview/InterviewReportPage.vue`

- [ ] **Step 1: Add imports and computed option**

In `InterviewReportPage.vue`, import:

```ts
import BaseChart from '@/components/charts/BaseChart.vue'
import { buildRadarOption } from '@/utils/charts/reviewCharts'
```

Add computed:

```ts
const reportRadarOption = computed(() => {
  if (!enhancement.value?.radar || Object.keys(enhancement.value.radar).length === 0) {
    return null
  }
  return buildRadarOption(enhancement.value.radar, '本次能力雷达')
})
```

- [ ] **Step 2: Add chart block**

Inside the completed enhancement section, add:

```vue
<div v-if="reportRadarOption" class="interview-report-page__chart-card">
  <BaseChart :option="reportRadarOption" height="320px" />
</div>
```

Add scoped CSS:

```css
.interview-report-page__chart-card {
  margin-top: 16px;
  border: 2px solid #111;
  border-radius: 12px;
  padding: 12px;
  background: #fff;
}
```

- [ ] **Step 3: Type-check and build**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both pass.

- [ ] **Step 4: Commit**

```powershell
git add frontend/src/views/interview/InterviewReportPage.vue
git commit -m "feat: visualize interview report radar"
```

---

## Task 8: Analytics Page Charts

**Files:**
- Modify: `frontend/src/views/analytics/AnalyticsOverviewPage.vue`

- [ ] **Step 1: Add chart imports**

Import:

```ts
import BaseChart from '@/components/charts/BaseChart.vue'
import { buildRadarOption, buildScoreTrendOption, buildSkillGapOption } from '@/utils/charts/reviewCharts'
```

Add computed values:

```ts
const analyticsRadarOption = computed(() => {
  if (!reviewAnalytics.value || Object.keys(reviewAnalytics.value.radar).length === 0) return null
  return buildRadarOption(reviewAnalytics.value.radar, '能力均值雷达')
})

const scoreTrendOption = computed(() => {
  if (!reviewAnalytics.value || reviewAnalytics.value.recentScoreTrend.length === 0) return null
  return buildScoreTrendOption(reviewAnalytics.value.recentScoreTrend)
})

const skillGapOption = computed(() => {
  if (!reviewAnalytics.value || reviewAnalytics.value.topSkillGaps.length === 0) return null
  return buildSkillGapOption(reviewAnalytics.value.topSkillGaps)
})
```

Also update Vue import from `ref, onMounted` to `ref, computed, onMounted`.

- [ ] **Step 2: Replace progress-only review cards with charts**

In `AnalyticsOverviewPage.vue`, show:

```vue
<BaseChart v-if="analyticsRadarOption" :option="analyticsRadarOption" height="320px" />
<div v-else style="color: #999; padding: 20px; text-align: center;">完成带增强复盘的面试后，这里会展示能力雷达</div>
```

For trend:

```vue
<BaseChart v-if="scoreTrendOption" :option="scoreTrendOption" height="320px" />
<div v-else style="color: #999; padding: 20px; text-align: center;">暂无近期分数趋势</div>
```

For skill gaps:

```vue
<BaseChart v-if="skillGapOption" :option="skillGapOption" height="320px" />
<div v-else style="color: #999; padding: 20px; text-align: center;">暂无技能缺口数据</div>
```

Keep latest action items as a list.

- [ ] **Step 3: Type-check and build**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both pass.

- [ ] **Step 4: Commit**

```powershell
git add frontend/src/views/analytics/AnalyticsOverviewPage.vue
git commit -m "feat: visualize review analytics dashboard"
```

---

## Task 9: Full Verification And Handoff

**Files:**
- No code changes expected unless verification finds a defect.

- [ ] **Step 1: Run backend package**

Run from `backend/`:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run backend targeted tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=ApplicationServiceImplTest,ApplicationControllerTest,InitSqlPhase3Test
```

Expected: all tests pass.

- [ ] **Step 3: Run frontend type-check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: pass.

- [ ] **Step 4: Run frontend build**

Run from `frontend/`:

```powershell
npm run build-only
```

Expected: build succeeds. Existing chunk-size warnings are acceptable if there are no errors.

- [ ] **Step 5: Review git status and diff**

Run from repo root:

```powershell
git status --short
git diff --stat HEAD~8..HEAD
git log --oneline -12
```

Expected: no unintended uncommitted files. Recent commits should map to schema, backend API, backend tests, frontend data layer, frontend pages, chart foundation, report chart, analytics chart.

- [ ] **Step 6: Final commit only if verification fixes were needed**

If Step 1-5 required small fixes, commit them:

```powershell
git add <changed-files>
git commit -m "fix: complete phase three verification"
```

If no fixes were needed, do not create an empty commit.

---

## Self-Review Notes

- Spec coverage: application records, application todos, global todo center, report radar chart, analytics radar/trend/skill-gap charts, and empty states are covered by Tasks 1-8.
- Scope control: notifications, calendar sync, batch import, external job platform integration, and new AI generation are excluded.
- Type consistency: backend uses `JobApplication` and `ApplicationTodo`; frontend uses `JobApplicationVO` and `ApplicationTodoVO`; status and priority values match the spec.
- Verification: Task 9 contains backend package, targeted backend tests, frontend type-check, frontend build, and git review.
