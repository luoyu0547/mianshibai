# Phase 7 AI Career Coach Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the AI career coach module that saves diagnosis history, generates 7-day coach plans, and supports coach task check-ins.

**Architecture:** Add a new `coach_*` backend module instead of reusing `training_plan` or `application_todo`, so coach history and task state remain isolated. `CoachServiceImpl` aggregates existing user data, calls Spring AI for JSON generation, falls back to deterministic rules on AI failure, then persists `coach_diagnosis`, `coach_plan`, and `coach_task`. Frontend adds a dedicated `/coach` user flow with Pinia data state and three pages.

**Tech Stack:** Spring Boot 3.5.x, Java 17, MyBatis-Plus, Spring AI `ChatClient`, JUnit 5, Mockito, Vue 3, TypeScript, Pinia, Vue Router, Element Plus, Vite.

---

## File Structure

### Backend Files

- Modify: `backend/src/main/resources/sql/init.sql`
  - Add `coach_diagnosis`, `coach_plan`, `coach_task` tables.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/CoachDiagnosis.java`
  - MyBatis entity for saved diagnosis history.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/CoachPlan.java`
  - MyBatis entity for saved 7-day coach plans.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/CoachTask.java`
  - MyBatis entity for daily coach tasks.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/CoachDiagnosisMapper.java`
  - Mapper for `coach_diagnosis`.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/CoachPlanMapper.java`
  - Mapper for `coach_plan`.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/CoachTaskMapper.java`
  - Mapper for `coach_task`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/coach/CoachGenerateRequest.java`
  - Request body for generating diagnosis and plan.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachDiagnosisVO.java`
  - Diagnosis response model.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachPlanVO.java`
  - Plan response model with tasks.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachTaskVO.java`
  - Task response model.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachOverviewVO.java`
  - Coach home overview response.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachGenerateResultVO.java`
  - Combined generate response.
- Create: `backend/src/main/java/com/mianshiba/ai/service/CoachService.java`
  - Coach service interface.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/CoachServiceImpl.java`
  - Auth, data aggregation, AI generation, fallback generation, persistence, task state transitions.
- Create: `backend/src/main/java/com/mianshiba/ai/controller/CoachController.java`
  - `/api/coach` controller.
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/CoachServiceImplTest.java`
  - Unit tests for generation, fallback, ownership, task completion, and plan status.
- Create: `backend/src/test/java/com/mianshiba/ai/controller/CoachControllerTest.java`
  - Controller routing tests.
- Modify: `backend/src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java`
  - Add mocks for new mappers/service if context requires them.
- Modify: `backend/src/test/java/com/mianshiba/ai/controller/StatisticsControllerTest.java`
  - Add mocks if context loading requires the new service.

### Frontend Files

- Create: `frontend/src/types/coach.ts`
  - Coach DTO/VO TypeScript types and labels.
- Create: `frontend/src/api/coach.ts`
  - Coach API functions.
- Create: `frontend/src/stores/coach.ts`
  - Coach Pinia setup store.
- Create: `frontend/src/views/coach/CoachHomePage.vue`
  - Coach overview, generation dialog, today tasks, history lists.
- Create: `frontend/src/views/coach/CoachDiagnosisDetailPage.vue`
  - Saved diagnosis details.
- Create: `frontend/src/views/coach/CoachPlanDetailPage.vue`
  - Saved plan details and task check-ins.
- Modify: `frontend/src/router/index.ts`
  - Add `/coach`, `/coach/diagnosis/:id`, `/coach/plan/:id` routes.
- Modify: `frontend/src/layouts/MainLayout.vue`
  - Add “求职教练” navigation entry.

---

## Task 1: Backend Schema, Entities, Mappers, DTO, VO, Service Contract

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/CoachDiagnosis.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/CoachPlan.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/CoachTask.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/CoachDiagnosisMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/CoachPlanMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/CoachTaskMapper.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/coach/CoachGenerateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachDiagnosisVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachPlanVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachTaskVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachOverviewVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachGenerateResultVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/CoachService.java`

- [ ] **Step 1: Add coach tables**

Append this SQL to `backend/src/main/resources/sql/init.sql` after the current Phase 6 tables:

```sql
CREATE TABLE IF NOT EXISTS coach_diagnosis (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '求职教练诊断 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '诊断标题',
  overall_score INT NOT NULL DEFAULT 0 COMMENT '综合评分 0-100',
  summary TEXT NOT NULL COMMENT '综合摘要',
  strengths_json JSON NOT NULL COMMENT '优势列表',
  weaknesses_json JSON NOT NULL COMMENT '短板列表',
  suggestions_json JSON NOT NULL COMMENT '建议列表',
  data_snapshot_json JSON NOT NULL COMMENT '生成时数据快照',
  data_completeness INT NOT NULL DEFAULT 0 COMMENT '数据完整度 0-100',
  source VARCHAR(32) NOT NULL DEFAULT 'fallback' COMMENT '生成来源：ai/fallback',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 求职教练诊断表';

CREATE TABLE IF NOT EXISTS coach_plan (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '求职教练计划 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  diagnosis_id BIGINT NOT NULL COMMENT '关联诊断 id',
  title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '计划标题',
  summary TEXT NOT NULL COMMENT '计划摘要',
  target_position VARCHAR(128) NOT NULL DEFAULT '' COMMENT '目标岗位',
  target_days INT NOT NULL DEFAULT 7 COMMENT '目标天数，固定 7',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active/completed/archived',
  source VARCHAR(32) NOT NULL DEFAULT 'fallback' COMMENT '生成来源：ai/fallback',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_diagnosis_id (diagnosis_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 求职教练计划表';

CREATE TABLE IF NOT EXISTS coach_task (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '求职教练任务 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  plan_id BIGINT NOT NULL COMMENT '教练计划 id',
  day_index INT NOT NULL DEFAULT 1 COMMENT '第几天，1-7',
  title VARCHAR(128) NOT NULL DEFAULT '' COMMENT '任务标题',
  description TEXT NOT NULL COMMENT '任务描述',
  task_type VARCHAR(32) NOT NULL DEFAULT 'habit' COMMENT 'resume/interview/training/application/job/habit',
  priority VARCHAR(32) NOT NULL DEFAULT 'medium' COMMENT 'high/medium/low',
  status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT 'pending/completed',
  reference_type VARCHAR(64) DEFAULT NULL COMMENT '引用类型',
  reference_id BIGINT DEFAULT NULL COMMENT '引用 id',
  completed_at DATETIME DEFAULT NULL COMMENT '完成时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_plan_id (plan_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 求职教练任务表';
```

- [ ] **Step 2: Create entities**

Create `backend/src/main/java/com/mianshiba/ai/model/entity/CoachDiagnosis.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "coach_diagnosis", autoResultMap = true)
public class CoachDiagnosis implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private Integer overallScore;
    private String summary;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> strengthsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> weaknessesJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> suggestionsJson;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> dataSnapshotJson;

    private Integer dataCompleteness;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/entity/CoachPlan.java`:

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
@TableName("coach_plan")
public class CoachPlan implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long diagnosisId;
    private String title;
    private String summary;
    private String targetPosition;
    private Integer targetDays;
    private String status;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/entity/CoachTask.java`:

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
@TableName("coach_task")
public class CoachTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long planId;
    private Integer dayIndex;
    private String title;
    private String description;
    private String taskType;
    private String priority;
    private String status;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

- [ ] **Step 3: Create mappers**

Create `backend/src/main/java/com/mianshiba/ai/mapper/CoachDiagnosisMapper.java`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.CoachDiagnosis;

public interface CoachDiagnosisMapper extends BaseMapper<CoachDiagnosis> {
}
```

Create `backend/src/main/java/com/mianshiba/ai/mapper/CoachPlanMapper.java`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.CoachPlan;

public interface CoachPlanMapper extends BaseMapper<CoachPlan> {
}
```

Create `backend/src/main/java/com/mianshiba/ai/mapper/CoachTaskMapper.java`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.CoachTask;

public interface CoachTaskMapper extends BaseMapper<CoachTask> {
}
```

- [ ] **Step 4: Create request and response models**

Create `backend/src/main/java/com/mianshiba/ai/model/dto/coach/CoachGenerateRequest.java`:

```java
package com.mianshiba.ai.model.dto.coach;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "AI 求职教练生成请求")
public class CoachGenerateRequest {

    @Size(max = 128, message = "目标岗位长度不能超过 128")
    @Schema(description = "目标岗位，空时使用用户资料目标岗位")
    private String targetPosition;

    @Size(max = 500, message = "关注点长度不能超过 500")
    @Schema(description = "本次希望教练重点关注的问题")
    private String focus;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachTaskVO.java`:

```java
package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CoachTaskVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long planId;
    private Integer dayIndex;
    private String title;
    private String description;
    private String taskType;
    private String priority;
    private String status;
    private String referenceType;
    private Long referenceId;
    private LocalDateTime completedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachDiagnosisVO.java`:

```java
package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class CoachDiagnosisVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private Integer overallScore;
    private String summary;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestions;
    private Map<String, Object> dataSnapshot;
    private Integer dataCompleteness;
    private String source;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachPlanVO.java`:

```java
package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CoachPlanVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long diagnosisId;
    private String title;
    private String summary;
    private String targetPosition;
    private Integer targetDays;
    private String status;
    private String source;
    private Integer totalTaskCount;
    private Integer completedTaskCount;
    private List<CoachTaskVO> tasks;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachOverviewVO.java`:

```java
package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class CoachOverviewVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private CoachDiagnosisVO latestDiagnosis;
    private CoachPlanVO activePlan;
    private List<CoachTaskVO> todayTasks;
    private Long diagnosisCount;
    private Long planCount;
}
```

Create `backend/src/main/java/com/mianshiba/ai/model/vo/coach/CoachGenerateResultVO.java`:

```java
package com.mianshiba.ai.model.vo.coach;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class CoachGenerateResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private CoachDiagnosisVO diagnosis;
    private CoachPlanVO plan;
}
```

- [ ] **Step 5: Create service contract**

Create `backend/src/main/java/com/mianshiba/ai/service/CoachService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.vo.coach.CoachDiagnosisVO;
import com.mianshiba.ai.model.vo.coach.CoachGenerateResultVO;
import com.mianshiba.ai.model.vo.coach.CoachOverviewVO;
import com.mianshiba.ai.model.vo.coach.CoachPlanVO;
import com.mianshiba.ai.model.vo.coach.CoachTaskVO;

import java.util.List;

public interface CoachService {

    CoachGenerateResultVO generate(String authorizationHeader, CoachGenerateRequest request);

    CoachOverviewVO getOverview(String authorizationHeader);

    List<CoachDiagnosisVO> listDiagnoses(String authorizationHeader);

    CoachDiagnosisVO getDiagnosis(String authorizationHeader, Long id);

    List<CoachPlanVO> listPlans(String authorizationHeader);

    CoachPlanVO getPlan(String authorizationHeader, Long id);

    CoachTaskVO completeTask(String authorizationHeader, Long id);

    CoachTaskVO reopenTask(String authorizationHeader, Long id);
}
```

- [ ] **Step 6: Run compile**

Run from `backend/`:

```powershell
.\mvnw.cmd -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit Task 1**

```powershell
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/model/entity/CoachDiagnosis.java backend/src/main/java/com/mianshiba/ai/model/entity/CoachPlan.java backend/src/main/java/com/mianshiba/ai/model/entity/CoachTask.java backend/src/main/java/com/mianshiba/ai/mapper/CoachDiagnosisMapper.java backend/src/main/java/com/mianshiba/ai/mapper/CoachPlanMapper.java backend/src/main/java/com/mianshiba/ai/mapper/CoachTaskMapper.java backend/src/main/java/com/mianshiba/ai/model/dto/coach backend/src/main/java/com/mianshiba/ai/model/vo/coach backend/src/main/java/com/mianshiba/ai/service/CoachService.java
git commit -m "feat: add coach API contracts"
```

---

## Task 2: Backend Coach Service Implementation

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/CoachServiceImpl.java`
- Create: `backend/src/test/java/com/mianshiba/ai/service/impl/CoachServiceImplTest.java`

- [ ] **Step 1: Write service tests**

Create `backend/src/test/java/com/mianshiba/ai/service/impl/CoachServiceImplTest.java` with these tests:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CoachDiagnosisMapper;
import com.mianshiba.ai.mapper.CoachPlanMapper;
import com.mianshiba.ai.mapper.CoachTaskMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.entity.CoachPlan;
import com.mianshiba.ai.model.entity.CoachTask;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoachServiceImplTest {

    @Mock private JwtUtils jwtUtils;
    @Mock private ChatClient chatClient;
    @Mock private CoachDiagnosisMapper diagnosisMapper;
    @Mock private CoachPlanMapper planMapper;
    @Mock private CoachTaskMapper taskMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private CoachServiceImpl coachService;

    private void mockUser() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "test", "user"));
        User user = new User();
        user.setId(1L);
        user.setTargetPosition("Java 后端开发");
        user.setTechDirection("Java/Spring Boot");
        user.setJobStatus("looking");
        when(userMapper.selectById(1L)).thenReturn(user);
    }

    @Test
    void generate_fallsBackAndPersistsDiagnosisPlanTasksWhenAiFails() {
        mockUser();
        CoachGenerateRequest request = new CoachGenerateRequest();
        request.setFocus("准备后端面试");

        var result = coachService.generate("Bearer test-token", request);

        assertThat(result.getDiagnosis().getSource()).isEqualTo("fallback");
        assertThat(result.getPlan().getSource()).isEqualTo("fallback");
        assertThat(result.getPlan().getTasks()).hasSize(14);
        verify(planMapper).update(any(), any());
        verify(diagnosisMapper).insert(any());
        verify(planMapper).insert(any());
        verify(taskMapper).insert(any());
    }

    @Test
    void completeTask_marksTaskCompletedAndCompletesPlanWhenAllTasksDone() {
        mockUser();
        CoachPlan plan = new CoachPlan();
        plan.setId(10L);
        plan.setUserId(1L);
        plan.setStatus("active");
        CoachTask task = new CoachTask();
        task.setId(20L);
        task.setUserId(1L);
        task.setPlanId(10L);
        task.setStatus("pending");
        when(taskMapper.selectById(20L)).thenReturn(task);
        when(planMapper.selectById(10L)).thenReturn(plan);
        when(taskMapper.selectCount(any())).thenReturn(0L);

        var result = coachService.completeTask("Bearer test-token", 20L);

        assertThat(result.getStatus()).isEqualTo("completed");
        ArgumentCaptor<CoachPlan> planCaptor = ArgumentCaptor.forClass(CoachPlan.class);
        verify(planMapper).updateById(planCaptor.capture());
        assertThat(planCaptor.getValue().getStatus()).isEqualTo("completed");
    }

    @Test
    void reopenTask_reopensCompletedPlan() {
        mockUser();
        CoachPlan plan = new CoachPlan();
        plan.setId(10L);
        plan.setUserId(1L);
        plan.setStatus("completed");
        CoachTask task = new CoachTask();
        task.setId(20L);
        task.setUserId(1L);
        task.setPlanId(10L);
        task.setStatus("completed");
        when(taskMapper.selectById(20L)).thenReturn(task);
        when(planMapper.selectById(10L)).thenReturn(plan);

        var result = coachService.reopenTask("Bearer test-token", 20L);

        assertThat(result.getStatus()).isEqualTo("pending");
        ArgumentCaptor<CoachPlan> planCaptor = ArgumentCaptor.forClass(CoachPlan.class);
        verify(planMapper).updateById(planCaptor.capture());
        assertThat(planCaptor.getValue().getStatus()).isEqualTo("active");
    }

    @Test
    void getPlan_rejectsOtherUsersPlan() {
        mockUser();
        CoachPlan plan = new CoachPlan();
        plan.setId(10L);
        plan.setUserId(2L);
        when(planMapper.selectById(10L)).thenReturn(plan);

        assertThatThrownBy(() -> coachService.getPlan("Bearer test-token", 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.NOT_FOUND_ERROR.getCode());
    }

    @Test
    void listPlans_returnsCurrentUserPlans() {
        mockUser();
        CoachPlan plan = new CoachPlan();
        plan.setId(10L);
        plan.setUserId(1L);
        plan.setStatus("active");
        when(planMapper.selectList(any())).thenReturn(List.of(plan));
        when(taskMapper.selectList(any())).thenReturn(List.of());

        var result = coachService.listPlans("Bearer test-token");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run from `backend/`:

```powershell
.\mvnw.cmd -Dtest=CoachServiceImplTest test
```

Expected: compilation fails because `CoachServiceImpl` does not exist.

- [ ] **Step 3: Implement service**

Create `backend/src/main/java/com/mianshiba/ai/service/impl/CoachServiceImpl.java` with this structure and required methods:

```java
package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.CoachDiagnosisMapper;
import com.mianshiba.ai.mapper.CoachPlanMapper;
import com.mianshiba.ai.mapper.CoachTaskMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.entity.CoachDiagnosis;
import com.mianshiba.ai.model.entity.CoachPlan;
import com.mianshiba.ai.model.entity.CoachTask;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.coach.CoachDiagnosisVO;
import com.mianshiba.ai.model.vo.coach.CoachGenerateResultVO;
import com.mianshiba.ai.model.vo.coach.CoachOverviewVO;
import com.mianshiba.ai.model.vo.coach.CoachPlanVO;
import com.mianshiba.ai.model.vo.coach.CoachTaskVO;
import com.mianshiba.ai.service.CoachService;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachServiceImpl implements CoachService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);
    private static final Set<String> TASK_TYPES = Set.of("resume", "interview", "training", "application", "job", "habit");
    private static final Set<String> PRIORITIES = Set.of("high", "medium", "low");
    private static final Set<String> REFERENCE_TYPES = Set.of("resume", "interview_session", "interview_report", "training_question", "training_plan", "job_application", "job");

    private static final String SYSTEM_PROMPT = "你是一位资深程序员求职教练。请基于用户数据生成求职诊断和 7 天计划。只返回 JSON，不要解释。";

    private final JwtUtils jwtUtils;
    private final ChatClient chatClient;
    private final CoachDiagnosisMapper diagnosisMapper;
    private final CoachPlanMapper planMapper;
    private final CoachTaskMapper taskMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachGenerateResultVO generate(String authorizationHeader, CoachGenerateRequest request) {
        Long userId = resolveUserId(authorizationHeader);
        User user = requireUser(userId);
        Map<String, Object> snapshot = buildSnapshot(user, request);
        GeneratedCoach generated = tryGenerateWithAi(snapshot);
        if (generated == null) {
            generated = fallback(snapshot);
        }

        planMapper.update(null, Wrappers.lambdaUpdate(CoachPlan.class)
                .eq(CoachPlan::getUserId, userId)
                .eq(CoachPlan::getStatus, "active")
                .set(CoachPlan::getStatus, "archived"));

        CoachDiagnosis diagnosis = new CoachDiagnosis();
        diagnosis.setUserId(userId);
        diagnosis.setTitle(generated.diagnosisTitle());
        diagnosis.setOverallScore(clamp(generated.overallScore(), 0, 100));
        diagnosis.setSummary(generated.diagnosisSummary());
        diagnosis.setStrengthsJson(generated.strengths());
        diagnosis.setWeaknessesJson(generated.weaknesses());
        diagnosis.setSuggestionsJson(generated.suggestions());
        diagnosis.setDataSnapshotJson(snapshot);
        diagnosis.setDataCompleteness(calculateCompleteness(user));
        diagnosis.setSource(generated.source());
        diagnosisMapper.insert(diagnosis);

        CoachPlan plan = new CoachPlan();
        plan.setUserId(userId);
        plan.setDiagnosisId(diagnosis.getId());
        plan.setTitle(generated.planTitle());
        plan.setSummary(generated.planSummary());
        plan.setTargetPosition(String.valueOf(snapshot.getOrDefault("targetPosition", "")));
        plan.setTargetDays(7);
        plan.setStatus("active");
        plan.setSource(generated.source());
        planMapper.insert(plan);

        List<CoachTask> tasks = normalizeTasks(userId, plan.getId(), generated.tasks());
        for (CoachTask task : tasks) {
            taskMapper.insert(task);
        }

        CoachGenerateResultVO result = new CoachGenerateResultVO();
        result.setDiagnosis(toDiagnosisVO(diagnosis));
        result.setPlan(toPlanVO(plan, tasks));
        return result;
    }

    @Override
    public CoachOverviewVO getOverview(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        CoachOverviewVO vo = new CoachOverviewVO();
        CoachDiagnosis latest = diagnosisMapper.selectOne(Wrappers.lambdaQuery(CoachDiagnosis.class)
                .eq(CoachDiagnosis::getUserId, userId)
                .orderByDesc(CoachDiagnosis::getCreateTime)
                .last("LIMIT 1"));
        CoachPlan active = planMapper.selectOne(Wrappers.lambdaQuery(CoachPlan.class)
                .eq(CoachPlan::getUserId, userId)
                .eq(CoachPlan::getStatus, "active")
                .last("LIMIT 1"));
        vo.setLatestDiagnosis(latest == null ? null : toDiagnosisVO(latest));
        vo.setActivePlan(active == null ? null : toPlanVO(active));
        vo.setTodayTasks(active == null ? List.of() : taskMapper.selectList(Wrappers.lambdaQuery(CoachTask.class)
                .eq(CoachTask::getPlanId, active.getId())
                .eq(CoachTask::getDayIndex, 1)
                .orderByAsc(CoachTask::getId)).stream().map(this::toTaskVO).toList());
        vo.setDiagnosisCount(diagnosisMapper.selectCount(Wrappers.lambdaQuery(CoachDiagnosis.class).eq(CoachDiagnosis::getUserId, userId)));
        vo.setPlanCount(planMapper.selectCount(Wrappers.lambdaQuery(CoachPlan.class).eq(CoachPlan::getUserId, userId)));
        return vo;
    }

    @Override
    public List<CoachDiagnosisVO> listDiagnoses(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        return diagnosisMapper.selectList(Wrappers.lambdaQuery(CoachDiagnosis.class)
                .eq(CoachDiagnosis::getUserId, userId)
                .orderByDesc(CoachDiagnosis::getCreateTime)).stream().map(this::toDiagnosisVO).toList();
    }

    @Override
    public CoachDiagnosisVO getDiagnosis(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        CoachDiagnosis diagnosis = diagnosisMapper.selectById(id);
        if (diagnosis == null || !userId.equals(diagnosis.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return toDiagnosisVO(diagnosis);
    }

    @Override
    public List<CoachPlanVO> listPlans(String authorizationHeader) {
        Long userId = resolveUserId(authorizationHeader);
        return planMapper.selectList(Wrappers.lambdaQuery(CoachPlan.class)
                .eq(CoachPlan::getUserId, userId)
                .orderByDesc(CoachPlan::getCreateTime)).stream().map(this::toPlanVO).toList();
    }

    @Override
    public CoachPlanVO getPlan(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        CoachPlan plan = planMapper.selectById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return toPlanVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachTaskVO completeTask(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        CoachTask task = getOwnedTask(userId, id);
        task.setStatus("completed");
        task.setCompletedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        CoachPlan plan = planMapper.selectById(task.getPlanId());
        Long pendingCount = taskMapper.selectCount(Wrappers.lambdaQuery(CoachTask.class)
                .eq(CoachTask::getPlanId, task.getPlanId())
                .eq(CoachTask::getStatus, "pending"));
        if (plan != null && pendingCount == 0) {
            plan.setStatus("completed");
            planMapper.updateById(plan);
        }
        return toTaskVO(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CoachTaskVO reopenTask(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        CoachTask task = getOwnedTask(userId, id);
        task.setStatus("pending");
        task.setCompletedAt(null);
        taskMapper.updateById(task);
        CoachPlan plan = planMapper.selectById(task.getPlanId());
        if (plan != null && "completed".equals(plan.getStatus())) {
            plan.setStatus("active");
            planMapper.updateById(plan);
        }
        return toTaskVO(task);
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        if (token == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return jwtUtils.parseToken(token).userId();
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    private Map<String, Object> buildSnapshot(User user, CoachGenerateRequest request) {
        Map<String, Object> snapshot = new HashMap<>();
        String targetPosition = request != null && request.getTargetPosition() != null && !request.getTargetPosition().isBlank()
                ? request.getTargetPosition().trim() : user.getTargetPosition();
        snapshot.put("targetPosition", targetPosition == null ? "" : targetPosition);
        snapshot.put("techDirection", user.getTechDirection() == null ? "" : user.getTechDirection());
        snapshot.put("workYears", user.getWorkYears() == null ? 0 : user.getWorkYears());
        snapshot.put("city", user.getCity() == null ? "" : user.getCity());
        snapshot.put("jobStatus", user.getJobStatus() == null ? "" : user.getJobStatus());
        snapshot.put("focus", request == null || request.getFocus() == null ? "" : request.getFocus().trim());
        return snapshot;
    }

    private GeneratedCoach tryGenerateWithAi(Map<String, Object> snapshot) {
        try {
            String response = chatClient.prompt().system(SYSTEM_PROMPT).user(OBJECT_MAPPER.writeValueAsString(snapshot)).call().content();
            String json = extractJson(response);
            Map<String, Object> parsed = OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            return parseGenerated(parsed, "ai");
        } catch (Exception e) {
            log.warn("AI 求职教练生成失败，使用兜底计划", e);
            return null;
        }
    }

    private GeneratedCoach fallback(Map<String, Object> snapshot) {
        String target = String.valueOf(snapshot.getOrDefault("targetPosition", "Java 后端开发"));
        List<CoachTaskDraft> tasks = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            tasks.add(new CoachTaskDraft(day, "完成 Day " + day + " 八股复习", "围绕 " + target + " 高频知识点完成 2 道八股题复盘。", "training", "high", null, null));
            tasks.add(new CoachTaskDraft(day, "整理 Day " + day + " 求职行动", "优化一个简历或投递相关动作，并记录结果。", "habit", "medium", null, null));
        }
        return new GeneratedCoach("求职准备诊断", 60, "当前数据不足或 AI 暂不可用，已生成基础 7 天求职启动计划。",
                List.of("已经开始系统化准备求职"), List.of("需要补齐更多训练和投递数据"), List.of("先完成 7 天基础行动计划"),
                "7 天求职启动计划", "每天完成八股复习和一个求职行动。", tasks, "fallback");
    }

    private GeneratedCoach parseGenerated(Map<String, Object> parsed, String source) {
        Map<String, Object> diagnosis = objectMap(parsed.get("diagnosis"));
        Map<String, Object> plan = objectMap(parsed.get("plan"));
        List<CoachTaskDraft> tasks = new ArrayList<>();
        for (Map<String, Object> item : listOfMaps(plan.get("tasks"))) {
            tasks.add(new CoachTaskDraft(number(item.get("dayIndex"), 1), string(item.get("title"), "求职任务"), string(item.get("description"), "完成一个求职准备动作。"),
                    string(item.get("taskType"), "habit"), string(item.get("priority"), "medium"), nullableString(item.get("referenceType")), nullableLong(item.get("referenceId"))));
        }
        return new GeneratedCoach(string(diagnosis.get("title"), "求职诊断"), number(diagnosis.get("overallScore"), 60), string(diagnosis.get("summary"), "已生成求职诊断。"),
                stringList(diagnosis.get("strengths")), stringList(diagnosis.get("weaknesses")), stringList(diagnosis.get("suggestions")),
                string(plan.get("title"), "7 天求职计划"), string(plan.get("summary"), "完成 7 天求职准备任务。"), tasks, source);
    }

    private List<CoachTask> normalizeTasks(Long userId, Long planId, List<CoachTaskDraft> drafts) {
        List<CoachTaskDraft> normalized = new ArrayList<>(drafts == null ? List.of() : drafts);
        while (normalized.size() < 14) {
            int day = normalized.size() % 7 + 1;
            normalized.add(new CoachTaskDraft(day, "补充求职准备任务", "完成一次简历、八股或投递相关复盘。", "habit", "medium", null, null));
        }
        if (normalized.size() > 21) {
            normalized = normalized.subList(0, 21);
        }
        List<CoachTask> tasks = new ArrayList<>();
        for (CoachTaskDraft draft : normalized) {
            CoachTask task = new CoachTask();
            task.setUserId(userId);
            task.setPlanId(planId);
            task.setDayIndex(clamp(draft.dayIndex(), 1, 7));
            task.setTitle(draft.title());
            task.setDescription(draft.description());
            task.setTaskType(TASK_TYPES.contains(draft.taskType()) ? draft.taskType() : "habit");
            task.setPriority(PRIORITIES.contains(draft.priority()) ? draft.priority() : "medium");
            task.setStatus("pending");
            task.setReferenceType(draft.referenceType() != null && REFERENCE_TYPES.contains(draft.referenceType()) ? draft.referenceType() : null);
            task.setReferenceId(task.getReferenceType() == null ? null : draft.referenceId());
            tasks.add(task);
        }
        return tasks;
    }

    private CoachTask getOwnedTask(Long userId, Long id) {
        CoachTask task = taskMapper.selectById(id);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return task;
    }

    private CoachPlanVO toPlanVO(CoachPlan plan) {
        List<CoachTask> tasks = taskMapper.selectList(Wrappers.lambdaQuery(CoachTask.class).eq(CoachTask::getPlanId, plan.getId()).orderByAsc(CoachTask::getDayIndex).orderByAsc(CoachTask::getId));
        return toPlanVO(plan, tasks);
    }

    private CoachPlanVO toPlanVO(CoachPlan plan, List<CoachTask> tasks) {
        CoachPlanVO vo = new CoachPlanVO();
        vo.setId(plan.getId());
        vo.setDiagnosisId(plan.getDiagnosisId());
        vo.setTitle(plan.getTitle());
        vo.setSummary(plan.getSummary());
        vo.setTargetPosition(plan.getTargetPosition());
        vo.setTargetDays(plan.getTargetDays());
        vo.setStatus(plan.getStatus());
        vo.setSource(plan.getSource());
        vo.setTasks(tasks.stream().map(this::toTaskVO).toList());
        vo.setTotalTaskCount(tasks.size());
        vo.setCompletedTaskCount((int) tasks.stream().filter(t -> "completed".equals(t.getStatus())).count());
        vo.setCreateTime(plan.getCreateTime());
        vo.setUpdateTime(plan.getUpdateTime());
        return vo;
    }

    private CoachDiagnosisVO toDiagnosisVO(CoachDiagnosis diagnosis) {
        CoachDiagnosisVO vo = new CoachDiagnosisVO();
        vo.setId(diagnosis.getId());
        vo.setTitle(diagnosis.getTitle());
        vo.setOverallScore(diagnosis.getOverallScore());
        vo.setSummary(diagnosis.getSummary());
        vo.setStrengths(defaultList(diagnosis.getStrengthsJson()));
        vo.setWeaknesses(defaultList(diagnosis.getWeaknessesJson()));
        vo.setSuggestions(defaultList(diagnosis.getSuggestionsJson()));
        vo.setDataSnapshot(diagnosis.getDataSnapshotJson() == null ? Map.of() : diagnosis.getDataSnapshotJson());
        vo.setDataCompleteness(diagnosis.getDataCompleteness());
        vo.setSource(diagnosis.getSource());
        vo.setCreateTime(diagnosis.getCreateTime());
        vo.setUpdateTime(diagnosis.getUpdateTime());
        return vo;
    }

    private CoachTaskVO toTaskVO(CoachTask task) {
        CoachTaskVO vo = new CoachTaskVO();
        vo.setId(task.getId());
        vo.setPlanId(task.getPlanId());
        vo.setDayIndex(task.getDayIndex());
        vo.setTitle(task.getTitle());
        vo.setDescription(task.getDescription());
        vo.setTaskType(task.getTaskType());
        vo.setPriority(task.getPriority());
        vo.setStatus(task.getStatus());
        vo.setReferenceType(task.getReferenceType());
        vo.setReferenceId(task.getReferenceId());
        vo.setCompletedAt(task.getCompletedAt());
        vo.setCreateTime(task.getCreateTime());
        vo.setUpdateTime(task.getUpdateTime());
        return vo;
    }

    private int calculateCompleteness(User user) {
        int score = 20;
        if (user.getTargetPosition() != null && !user.getTargetPosition().isBlank()) score += 20;
        if (user.getTechDirection() != null && !user.getTechDirection().isBlank()) score += 20;
        if (user.getCity() != null && !user.getCity().isBlank()) score += 20;
        if (user.getJobStatus() != null && !user.getJobStatus().isBlank()) score += 20;
        return clamp(score, 0, 100);
    }

    private String extractJson(String response) {
        var matcher = JSON_CODE_BLOCK_PATTERN.matcher(response == null ? "" : response);
        return matcher.find() ? matcher.group(1).trim() : response;
    }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int number(Object value, int fallback) { return value instanceof Number number ? number.intValue() : fallback; }
    private static String string(Object value, String fallback) { return value instanceof String s && !s.isBlank() ? s : fallback; }
    private static String nullableString(Object value) { return value instanceof String s && !s.isBlank() ? s : null; }
    private static Long nullableLong(Object value) { return value instanceof Number number ? number.longValue() : null; }
    private static List<String> defaultList(List<String> value) { return value == null ? List.of() : value; }
    private static List<String> stringList(Object value) { return value instanceof List<?> list ? list.stream().map(String::valueOf).toList() : Collections.emptyList(); }
    private static Map<String, Object> objectMap(Object value) { return value instanceof Map<?, ?> map ? map.entrySet().stream().collect(HashMap::new, (m, e) -> m.put(String.valueOf(e.getKey()), e.getValue()), HashMap::putAll) : new HashMap<>(); }
    private static List<Map<String, Object>> listOfMaps(Object value) { return value instanceof List<?> list ? list.stream().filter(Map.class::isInstance).map(Map.class::cast).map(CoachServiceImpl::objectMap).toList() : List.of(); }

    private record GeneratedCoach(String diagnosisTitle, int overallScore, String diagnosisSummary, List<String> strengths, List<String> weaknesses, List<String> suggestions, String planTitle, String planSummary, List<CoachTaskDraft> tasks, String source) {}
    private record CoachTaskDraft(int dayIndex, String title, String description, String taskType, String priority, String referenceType, Long referenceId) {}
}
```

- [ ] **Step 4: Run service tests**

Run from `backend/`:

```powershell
.\mvnw.cmd -Dtest=CoachServiceImplTest test
```

Expected: `BUILD SUCCESS` and 5 tests pass.

- [ ] **Step 5: Commit Task 2**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/CoachServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/CoachServiceImplTest.java
git commit -m "feat: implement coach service"
```

---

## Task 3: Backend Coach Controller

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/controller/CoachController.java`
- Create: `backend/src/test/java/com/mianshiba/ai/controller/CoachControllerTest.java`

- [ ] **Step 1: Write controller test**

Create `backend/src/test/java/com/mianshiba/ai/controller/CoachControllerTest.java`:

```java
package com.mianshiba.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.vo.coach.CoachGenerateResultVO;
import com.mianshiba.ai.model.vo.coach.CoachOverviewVO;
import com.mianshiba.ai.service.CoachService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CoachController.class)
class CoachControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private CoachService coachService;

    @Test
    void generate_routesToService() throws Exception {
        when(coachService.generate(eq("Bearer test-token"), any(CoachGenerateRequest.class))).thenReturn(new CoachGenerateResultVO());

        mockMvc.perform(post("/api/coach/generate")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CoachGenerateRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void overview_routesToService() throws Exception {
        when(coachService.getOverview("Bearer test-token")).thenReturn(new CoachOverviewVO());

        mockMvc.perform(get("/api/coach/overview").header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void listEndpoints_routeToService() throws Exception {
        when(coachService.listDiagnoses("Bearer test-token")).thenReturn(List.of());
        when(coachService.listPlans("Bearer test-token")).thenReturn(List.of());

        mockMvc.perform(get("/api/coach/diagnoses").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/coach/plans").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
    }

    @Test
    void detailAndTaskEndpoints_routeToService() throws Exception {
        mockMvc.perform(get("/api/coach/diagnoses/1").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mockMvc.perform(get("/api/coach/plans/1").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mockMvc.perform(put("/api/coach/tasks/1/complete").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
        mockMvc.perform(put("/api/coach/tasks/1/reopen").header("Authorization", "Bearer test-token")).andExpect(status().isOk());
    }
}
```

- [ ] **Step 2: Run controller test to verify it fails**

Run from `backend/`:

```powershell
.\mvnw.cmd -Dtest=CoachControllerTest test
```

Expected: compilation fails because `CoachController` does not exist.

- [ ] **Step 3: Implement controller**

Create `backend/src/main/java/com/mianshiba/ai/controller/CoachController.java`:

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.coach.CoachGenerateRequest;
import com.mianshiba.ai.model.vo.coach.CoachDiagnosisVO;
import com.mianshiba.ai.model.vo.coach.CoachGenerateResultVO;
import com.mianshiba.ai.model.vo.coach.CoachOverviewVO;
import com.mianshiba.ai.model.vo.coach.CoachPlanVO;
import com.mianshiba.ai.model.vo.coach.CoachTaskVO;
import com.mianshiba.ai.service.CoachService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
@RequestMapping("/api/coach")
@Tag(name = "AI 求职教练接口")
public class CoachController {

    private final CoachService coachService;

    @PostMapping("/generate")
    @Operation(summary = "生成求职诊断和 7 天计划")
    public BaseResponse<CoachGenerateResultVO> generate(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                        @Valid @RequestBody CoachGenerateRequest request) {
        return ResultUtils.success(coachService.generate(auth, request));
    }

    @GetMapping("/overview")
    @Operation(summary = "求职教练总览")
    public BaseResponse<CoachOverviewVO> getOverview(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(coachService.getOverview(auth));
    }

    @GetMapping("/diagnoses")
    @Operation(summary = "诊断历史")
    public BaseResponse<List<CoachDiagnosisVO>> listDiagnoses(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(coachService.listDiagnoses(auth));
    }

    @GetMapping("/diagnoses/{id}")
    @Operation(summary = "诊断详情")
    public BaseResponse<CoachDiagnosisVO> getDiagnosis(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                       @PathVariable("id") Long id) {
        return ResultUtils.success(coachService.getDiagnosis(auth, id));
    }

    @GetMapping("/plans")
    @Operation(summary = "教练计划历史")
    public BaseResponse<List<CoachPlanVO>> listPlans(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(coachService.listPlans(auth));
    }

    @GetMapping("/plans/{id}")
    @Operation(summary = "教练计划详情")
    public BaseResponse<CoachPlanVO> getPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                             @PathVariable("id") Long id) {
        return ResultUtils.success(coachService.getPlan(auth, id));
    }

    @PutMapping("/tasks/{id}/complete")
    @Operation(summary = "完成教练任务")
    public BaseResponse<CoachTaskVO> completeTask(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                  @PathVariable("id") Long id) {
        return ResultUtils.success(coachService.completeTask(auth, id));
    }

    @PutMapping("/tasks/{id}/reopen")
    @Operation(summary = "重开教练任务")
    public BaseResponse<CoachTaskVO> reopenTask(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                @PathVariable("id") Long id) {
        return ResultUtils.success(coachService.reopenTask(auth, id));
    }
}
```

- [ ] **Step 4: Run controller and service tests**

Run from `backend/`:

```powershell
.\mvnw.cmd -Dtest=CoachControllerTest,CoachServiceImplTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit Task 3**

```powershell
git add backend/src/main/java/com/mianshiba/ai/controller/CoachController.java backend/src/test/java/com/mianshiba/ai/controller/CoachControllerTest.java
git commit -m "feat: expose coach API"
```

---

## Task 4: Backend Context Test Integration

**Files:**
- Modify: `backend/src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java`
- Modify: `backend/src/test/java/com/mianshiba/ai/controller/StatisticsControllerTest.java`

- [ ] **Step 1: Run context tests first**

Run from `backend/`:

```powershell
.\mvnw.cmd -Dtest=MianshibaAiBackendApplicationTests,StatisticsControllerTest test
```

Expected: if new `CoachServiceImpl` dependencies are not mocked in broad context tests, Spring reports missing mapper/service beans.

- [ ] **Step 2: Add mocks to context tests when needed**

In `MianshibaAiBackendApplicationTests`, add imports for new mappers when not already present:

```java
import com.mianshiba.ai.mapper.CoachDiagnosisMapper;
import com.mianshiba.ai.mapper.CoachPlanMapper;
import com.mianshiba.ai.mapper.CoachTaskMapper;
```

Add fields inside the test class:

```java
@MockBean
private CoachDiagnosisMapper coachDiagnosisMapper;

@MockBean
private CoachPlanMapper coachPlanMapper;

@MockBean
private CoachTaskMapper coachTaskMapper;
```

In `StatisticsControllerTest`, add import if controller slice loads service dependencies:

```java
import com.mianshiba.ai.service.CoachService;
```

Add field inside the test class:

```java
@MockBean
private CoachService coachService;
```

- [ ] **Step 3: Run context tests again**

Run from `backend/`:

```powershell
.\mvnw.cmd -Dtest=MianshibaAiBackendApplicationTests,StatisticsControllerTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 4: Commit Task 4**

```powershell
git add backend/src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java backend/src/test/java/com/mianshiba/ai/controller/StatisticsControllerTest.java
git commit -m "test: mock coach dependencies in context tests"
```

---

## Task 5: Frontend Coach Data Layer and Routes

**Files:**
- Create: `frontend/src/types/coach.ts`
- Create: `frontend/src/api/coach.ts`
- Create: `frontend/src/stores/coach.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`

- [ ] **Step 1: Create frontend types**

Create `frontend/src/types/coach.ts`:

```ts
export type CoachSource = 'ai' | 'fallback'
export type CoachPlanStatus = 'active' | 'completed' | 'archived'
export type CoachTaskStatus = 'pending' | 'completed'
export type CoachTaskType = 'resume' | 'interview' | 'training' | 'application' | 'job' | 'habit'
export type CoachTaskPriority = 'high' | 'medium' | 'low'

export interface CoachGenerateRequest {
  targetPosition?: string
  focus?: string
}

export interface CoachTaskVO {
  id: number
  planId: number
  dayIndex: number
  title: string
  description: string
  taskType: CoachTaskType
  priority: CoachTaskPriority
  status: CoachTaskStatus
  referenceType?: string | null
  referenceId?: number | null
  completedAt?: string | null
  createTime: string
  updateTime: string
}

export interface CoachDiagnosisVO {
  id: number
  title: string
  overallScore: number
  summary: string
  strengths: string[]
  weaknesses: string[]
  suggestions: string[]
  dataSnapshot: Record<string, unknown>
  dataCompleteness: number
  source: CoachSource
  createTime: string
  updateTime: string
}

export interface CoachPlanVO {
  id: number
  diagnosisId: number
  title: string
  summary: string
  targetPosition: string
  targetDays: number
  status: CoachPlanStatus
  source: CoachSource
  totalTaskCount: number
  completedTaskCount: number
  tasks: CoachTaskVO[]
  createTime: string
  updateTime: string
}

export interface CoachOverviewVO {
  latestDiagnosis: CoachDiagnosisVO | null
  activePlan: CoachPlanVO | null
  todayTasks: CoachTaskVO[]
  diagnosisCount: number
  planCount: number
}

export interface CoachGenerateResultVO {
  diagnosis: CoachDiagnosisVO
  plan: CoachPlanVO
}

export const COACH_PLAN_STATUS_LABELS: Record<CoachPlanStatus, string> = {
  active: '进行中',
  completed: '已完成',
  archived: '已归档',
}

export const COACH_TASK_TYPE_LABELS: Record<CoachTaskType, string> = {
  resume: '简历优化',
  interview: '面试训练',
  training: '八股训练',
  application: '投递跟进',
  job: '职位分析',
  habit: '求职习惯',
}

export const COACH_PRIORITY_LABELS: Record<CoachTaskPriority, string> = {
  high: '高',
  medium: '中',
  low: '低',
}
```

- [ ] **Step 2: Create API functions**

Create `frontend/src/api/coach.ts`:

```ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type {
  CoachDiagnosisVO,
  CoachGenerateRequest,
  CoachGenerateResultVO,
  CoachOverviewVO,
  CoachPlanVO,
  CoachTaskVO,
} from '@/types/coach'

export function generateCoachPlan(data: CoachGenerateRequest) {
  return request.post<BaseResponse<CoachGenerateResultVO>>('/api/coach/generate', data)
}

export function getCoachOverview() {
  return request.get<BaseResponse<CoachOverviewVO>>('/api/coach/overview')
}

export function listCoachDiagnoses() {
  return request.get<BaseResponse<CoachDiagnosisVO[]>>('/api/coach/diagnoses')
}

export function getCoachDiagnosis(id: number) {
  return request.get<BaseResponse<CoachDiagnosisVO>>(`/api/coach/diagnoses/${id}`)
}

export function listCoachPlans() {
  return request.get<BaseResponse<CoachPlanVO[]>>('/api/coach/plans')
}

export function getCoachPlan(id: number) {
  return request.get<BaseResponse<CoachPlanVO>>(`/api/coach/plans/${id}`)
}

export function completeCoachTask(id: number) {
  return request.put<BaseResponse<CoachTaskVO>>(`/api/coach/tasks/${id}/complete`)
}

export function reopenCoachTask(id: number) {
  return request.put<BaseResponse<CoachTaskVO>>(`/api/coach/tasks/${id}/reopen`)
}
```

- [ ] **Step 3: Create Pinia store**

Create `frontend/src/stores/coach.ts`:

```ts
import { ref } from 'vue'
import { defineStore } from 'pinia'
import {
  completeCoachTask as completeCoachTaskApi,
  generateCoachPlan as generateCoachPlanApi,
  getCoachDiagnosis as getCoachDiagnosisApi,
  getCoachOverview as getCoachOverviewApi,
  getCoachPlan as getCoachPlanApi,
  listCoachDiagnoses as listCoachDiagnosesApi,
  listCoachPlans as listCoachPlansApi,
  reopenCoachTask as reopenCoachTaskApi,
} from '@/api/coach'
import type {
  CoachDiagnosisVO,
  CoachGenerateRequest,
  CoachOverviewVO,
  CoachPlanVO,
} from '@/types/coach'

export const useCoachStore = defineStore('coach', () => {
  const overview = ref<CoachOverviewVO | null>(null)
  const diagnoses = ref<CoachDiagnosisVO[]>([])
  const plans = ref<CoachPlanVO[]>([])
  const currentDiagnosis = ref<CoachDiagnosisVO | null>(null)
  const currentPlan = ref<CoachPlanVO | null>(null)
  const loading = ref(false)
  const generating = ref(false)

  async function generate(data: CoachGenerateRequest) {
    generating.value = true
    try {
      const res = await generateCoachPlanApi(data)
      if (res.data.code === 0) {
        overview.value = {
          latestDiagnosis: res.data.data.diagnosis,
          activePlan: res.data.data.plan,
          todayTasks: res.data.data.plan.tasks.filter((task) => task.dayIndex === 1),
          diagnosisCount: (overview.value?.diagnosisCount || 0) + 1,
          planCount: (overview.value?.planCount || 0) + 1,
        }
        return res.data.data
      }
      return null
    } finally {
      generating.value = false
    }
  }

  async function fetchOverview() {
    loading.value = true
    try {
      const res = await getCoachOverviewApi()
      if (res.data.code === 0) overview.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchDiagnoses() {
    const res = await listCoachDiagnosesApi()
    if (res.data.code === 0) diagnoses.value = res.data.data
  }

  async function fetchDiagnosis(id: number) {
    loading.value = true
    try {
      const res = await getCoachDiagnosisApi(id)
      if (res.data.code === 0) currentDiagnosis.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function fetchPlans() {
    const res = await listCoachPlansApi()
    if (res.data.code === 0) plans.value = res.data.data
  }

  async function fetchPlan(id: number) {
    loading.value = true
    try {
      const res = await getCoachPlanApi(id)
      if (res.data.code === 0) currentPlan.value = res.data.data
    } finally {
      loading.value = false
    }
  }

  async function completeTask(id: number) {
    const res = await completeCoachTaskApi(id)
    if (res.data.code !== 0) return false
    if (currentPlan.value) {
      const task = currentPlan.value.tasks.find((item) => item.id === id)
      if (task) task.status = 'completed'
      currentPlan.value.completedTaskCount = currentPlan.value.tasks.filter((item) => item.status === 'completed').length
    }
    return true
  }

  async function reopenTask(id: number) {
    const res = await reopenCoachTaskApi(id)
    if (res.data.code !== 0) return false
    if (currentPlan.value) {
      const task = currentPlan.value.tasks.find((item) => item.id === id)
      if (task) task.status = 'pending'
      currentPlan.value.completedTaskCount = currentPlan.value.tasks.filter((item) => item.status === 'completed').length
    }
    return true
  }

  return {
    overview,
    diagnoses,
    plans,
    currentDiagnosis,
    currentPlan,
    loading,
    generating,
    generate,
    fetchOverview,
    fetchDiagnoses,
    fetchDiagnosis,
    fetchPlans,
    fetchPlan,
    completeTask,
    reopenTask,
  }
})
```

- [ ] **Step 4: Add routes**

Modify `frontend/src/router/index.ts` by adding routes before `/analytics`:

```ts
    {
      path: '/coach',
      name: 'CoachHome',
      component: () => import('@/views/coach/CoachHomePage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/coach/diagnosis/:id',
      name: 'CoachDiagnosisDetail',
      component: () => import('@/views/coach/CoachDiagnosisDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/coach/plan/:id',
      name: 'CoachPlanDetail',
      component: () => import('@/views/coach/CoachPlanDetailPage.vue'),
      meta: { requiresAuth: true },
    },
```

- [ ] **Step 5: Add navigation entry**

Modify `frontend/src/layouts/MainLayout.vue` navigation after the training link:

```vue
<router-link to="/coach" class="main-layout__nav-link">求职教练</router-link>
```

- [ ] **Step 6: Run type check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: fails because the three route components do not exist yet. This is acceptable before Task 6.

- [ ] **Step 7: Commit Task 5**

```powershell
git add frontend/src/types/coach.ts frontend/src/api/coach.ts frontend/src/stores/coach.ts frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue
git commit -m "feat: add coach frontend data layer"
```

---

## Task 6: Frontend Coach Pages

**Files:**
- Create: `frontend/src/views/coach/CoachHomePage.vue`
- Create: `frontend/src/views/coach/CoachDiagnosisDetailPage.vue`
- Create: `frontend/src/views/coach/CoachPlanDetailPage.vue`

- [ ] **Step 1: Create CoachHomePage**

Create `frontend/src/views/coach/CoachHomePage.vue`:

```vue
<template>
  <MainLayout>
    <div class="coach-page">
      <div class="coach-page__header">
        <div>
          <h1 class="coach-page__title">AI 求职教练</h1>
          <p class="coach-page__subtitle">把简历、面试、八股和投递数据整理成今天该做的行动。</p>
        </div>
        <NbButton type="primary" @click="showGenerateDialog = true">生成诊断与计划</NbButton>
      </div>

      <div v-if="coachStore.loading" class="coach-page__loading">加载中...</div>

      <template v-else>
        <NbCard v-if="!coachStore.overview?.latestDiagnosis" class="coach-card coach-card--empty">
          <h2>还没有求职诊断</h2>
          <p>生成第一份诊断后，我会给你一份 7 天行动计划。</p>
          <NbButton type="primary" @click="showGenerateDialog = true">开始生成</NbButton>
        </NbCard>

        <div v-else class="coach-grid">
          <NbCard class="coach-card">
            <h2>最近诊断</h2>
            <div class="score">{{ coachStore.overview.latestDiagnosis.overallScore }}</div>
            <h3>{{ coachStore.overview.latestDiagnosis.title }}</h3>
            <p>{{ coachStore.overview.latestDiagnosis.summary }}</p>
            <router-link :to="`/coach/diagnosis/${coachStore.overview.latestDiagnosis.id}`">
              <NbButton>查看诊断详情</NbButton>
            </router-link>
          </NbCard>

          <NbCard v-if="coachStore.overview.activePlan" class="coach-card">
            <h2>当前 7 天计划</h2>
            <h3>{{ coachStore.overview.activePlan.title }}</h3>
            <el-progress :percentage="planProgress" :stroke-width="12" />
            <p>{{ coachStore.overview.activePlan.completedTaskCount }}/{{ coachStore.overview.activePlan.totalTaskCount }} 个任务已完成</p>
            <router-link :to="`/coach/plan/${coachStore.overview.activePlan.id}`">
              <NbButton>查看计划</NbButton>
            </router-link>
          </NbCard>
        </div>

        <NbCard v-if="coachStore.overview?.todayTasks?.length" class="coach-card">
          <h2>今日任务</h2>
          <div v-for="task in coachStore.overview.todayTasks" :key="task.id" class="task-row">
            <div>
              <strong>{{ task.title }}</strong>
              <p>{{ task.description }}</p>
            </div>
            <el-tag :type="task.status === 'completed' ? 'success' : 'warning'">{{ task.status === 'completed' ? '已完成' : '待完成' }}</el-tag>
          </div>
        </NbCard>

        <div class="history-grid">
          <NbCard class="coach-card">
            <h2>历史诊断</h2>
            <div v-for="diagnosis in coachStore.diagnoses.slice(0, 5)" :key="diagnosis.id" class="history-row" @click="router.push(`/coach/diagnosis/${diagnosis.id}`)">
              <span>{{ diagnosis.title }}</span>
              <span>{{ formatDate(diagnosis.createTime) }}</span>
            </div>
          </NbCard>
          <NbCard class="coach-card">
            <h2>历史计划</h2>
            <div v-for="plan in coachStore.plans.slice(0, 5)" :key="plan.id" class="history-row" @click="router.push(`/coach/plan/${plan.id}`)">
              <span>{{ plan.title }}</span>
              <el-tag size="small">{{ COACH_PLAN_STATUS_LABELS[plan.status] }}</el-tag>
            </div>
          </NbCard>
        </div>
      </template>

      <el-dialog v-model="showGenerateDialog" title="生成求职诊断与计划" width="520px">
        <el-form label-width="100px">
          <el-form-item label="目标岗位">
            <el-input v-model="generateForm.targetPosition" placeholder="如：Java 后端开发" />
          </el-form-item>
          <el-form-item label="本次重点">
            <el-input v-model="generateForm.focus" type="textarea" :rows="4" placeholder="如：准备春招、强化 Redis、提升项目表达" />
          </el-form-item>
        </el-form>
        <template #footer>
          <NbButton @click="showGenerateDialog = false">取消</NbButton>
          <NbButton type="primary" :loading="coachStore.generating" @click="handleGenerate">生成</NbButton>
        </template>
      </el-dialog>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import { useCoachStore } from '@/stores/coach'
import { COACH_PLAN_STATUS_LABELS } from '@/types/coach'

const router = useRouter()
const coachStore = useCoachStore()
const showGenerateDialog = ref(false)
const generateForm = reactive({ targetPosition: '', focus: '' })

const planProgress = computed(() => {
  const plan = coachStore.overview?.activePlan
  if (!plan || plan.totalTaskCount === 0) return 0
  return Math.round((plan.completedTaskCount / plan.totalTaskCount) * 100)
})

function formatDate(value: string) {
  return new Date(value).toLocaleDateString()
}

async function handleGenerate() {
  const result = await coachStore.generate({ ...generateForm })
  if (result) {
    ElMessage.success('诊断与计划已生成')
    showGenerateDialog.value = false
    await Promise.all([coachStore.fetchDiagnoses(), coachStore.fetchPlans()])
  }
}

onMounted(async () => {
  await Promise.all([coachStore.fetchOverview(), coachStore.fetchDiagnoses(), coachStore.fetchPlans()])
})
</script>

<style scoped>
.coach-page { display: flex; flex-direction: column; gap: 24px; }
.coach-page__header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.coach-page__title { margin: 0; font-family: var(--font-heading); font-size: 36px; }
.coach-page__subtitle { margin: 8px 0 0; color: var(--nb-muted); }
.coach-page__loading { padding: 48px; text-align: center; }
.coach-grid, .history-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 20px; }
.coach-card { display: flex; flex-direction: column; gap: 12px; }
.coach-card--empty { align-items: flex-start; }
.score { width: 84px; height: 84px; display: grid; place-items: center; border: var(--nb-border); box-shadow: var(--nb-shadow); background: var(--nb-primary); color: white; font-size: 32px; font-weight: 800; }
.task-row, .history-row { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 12px; border: var(--nb-border); background: #fff; cursor: pointer; }
.task-row p { margin: 4px 0 0; color: var(--nb-muted); }
@media (max-width: 768px) { .coach-grid, .history-grid { grid-template-columns: 1fr; } .coach-page__header { align-items: flex-start; flex-direction: column; } }
</style>
```

- [ ] **Step 2: Create CoachDiagnosisDetailPage**

Create `frontend/src/views/coach/CoachDiagnosisDetailPage.vue`:

```vue
<template>
  <MainLayout>
    <div class="diagnosis-page">
      <NbButton @click="router.push('/coach')">返回教练首页</NbButton>
      <NbCard v-if="coachStore.currentDiagnosis" class="diagnosis-card">
        <div class="diagnosis-header">
          <div>
            <h1>{{ coachStore.currentDiagnosis.title }}</h1>
            <p>{{ formatDate(coachStore.currentDiagnosis.createTime) }} · {{ coachStore.currentDiagnosis.source === 'ai' ? 'AI 生成' : '规则兜底' }}</p>
          </div>
          <div class="score">{{ coachStore.currentDiagnosis.overallScore }}</div>
        </div>
        <p class="summary">{{ coachStore.currentDiagnosis.summary }}</p>
        <el-progress :percentage="coachStore.currentDiagnosis.dataCompleteness" :stroke-width="12" />
        <section><h2>优势</h2><ul><li v-for="item in coachStore.currentDiagnosis.strengths" :key="item">{{ item }}</li></ul></section>
        <section><h2>短板</h2><ul><li v-for="item in coachStore.currentDiagnosis.weaknesses" :key="item">{{ item }}</li></ul></section>
        <section><h2>建议</h2><ul><li v-for="item in coachStore.currentDiagnosis.suggestions" :key="item">{{ item }}</li></ul></section>
      </NbCard>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import { useCoachStore } from '@/stores/coach'

const route = useRoute()
const router = useRouter()
const coachStore = useCoachStore()

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

onMounted(() => coachStore.fetchDiagnosis(Number(route.params.id)))
</script>

<style scoped>
.diagnosis-page { display: flex; flex-direction: column; gap: 20px; }
.diagnosis-card { display: flex; flex-direction: column; gap: 20px; }
.diagnosis-header { display: flex; align-items: center; justify-content: space-between; gap: 20px; }
.diagnosis-header h1 { margin: 0; font-family: var(--font-heading); }
.diagnosis-header p, .summary { color: var(--nb-muted); }
.score { width: 88px; height: 88px; display: grid; place-items: center; border: var(--nb-border); box-shadow: var(--nb-shadow); background: var(--nb-primary); color: white; font-size: 34px; font-weight: 800; }
section { border-top: var(--nb-border); padding-top: 16px; }
li { margin-bottom: 8px; }
</style>
```

- [ ] **Step 3: Create CoachPlanDetailPage**

Create `frontend/src/views/coach/CoachPlanDetailPage.vue`:

```vue
<template>
  <MainLayout>
    <div class="plan-page">
      <NbButton @click="router.push('/coach')">返回教练首页</NbButton>
      <NbCard v-if="coachStore.currentPlan" class="plan-header">
        <h1>{{ coachStore.currentPlan.title }}</h1>
        <p>{{ coachStore.currentPlan.summary }}</p>
        <el-progress :percentage="planProgress" :stroke-width="12" />
        <p>{{ coachStore.currentPlan.completedTaskCount }}/{{ coachStore.currentPlan.totalTaskCount }} 个任务已完成</p>
      </NbCard>

      <div v-for="group in groupedTasks" :key="group.dayIndex" class="day-section">
        <h2>Day {{ group.dayIndex }}</h2>
        <NbCard v-for="task in group.tasks" :key="task.id" class="task-card">
          <div class="task-card__main">
            <div>
              <h3>{{ task.title }}</h3>
              <p>{{ task.description }}</p>
              <div class="tags">
                <el-tag>{{ COACH_TASK_TYPE_LABELS[task.taskType] }}</el-tag>
                <el-tag :type="task.priority === 'high' ? 'danger' : task.priority === 'medium' ? 'warning' : 'info'">{{ COACH_PRIORITY_LABELS[task.priority] }}优先级</el-tag>
              </div>
            </div>
            <div class="task-card__actions">
              <NbButton v-if="task.status === 'pending'" type="success" @click="handleComplete(task.id)">完成</NbButton>
              <NbButton v-else @click="handleReopen(task.id)">重开</NbButton>
              <NbButton v-if="referencePath(task)" @click="router.push(referencePath(task)!)">去处理</NbButton>
            </div>
          </div>
        </NbCard>
      </div>
    </div>
  </MainLayout>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import MainLayout from '@/layouts/MainLayout.vue'
import NbButton from '@/components/NbButton.vue'
import NbCard from '@/components/NbCard.vue'
import { useCoachStore } from '@/stores/coach'
import { COACH_PRIORITY_LABELS, COACH_TASK_TYPE_LABELS } from '@/types/coach'
import type { CoachTaskVO } from '@/types/coach'

const route = useRoute()
const router = useRouter()
const coachStore = useCoachStore()

const planProgress = computed(() => {
  const plan = coachStore.currentPlan
  if (!plan || plan.totalTaskCount === 0) return 0
  return Math.round((plan.completedTaskCount / plan.totalTaskCount) * 100)
})

const groupedTasks = computed(() => {
  const map = new Map<number, CoachTaskVO[]>()
  for (const task of coachStore.currentPlan?.tasks || []) {
    const list = map.get(task.dayIndex) || []
    list.push(task)
    map.set(task.dayIndex, list)
  }
  return Array.from(map.entries()).sort(([a], [b]) => a - b).map(([dayIndex, tasks]) => ({ dayIndex, tasks }))
})

function referencePath(task: CoachTaskVO) {
  if (!task.referenceType || !task.referenceId) return null
  if (task.referenceType === 'resume') return `/resume/${task.referenceId}/edit`
  if (task.referenceType === 'interview_session') return `/interview/${task.referenceId}/report`
  if (task.referenceType === 'training_question') return `/training/question/${task.referenceId}`
  if (task.referenceType === 'training_plan') return `/training/plan/${task.referenceId}`
  if (task.referenceType === 'job_application') return `/applications/${task.referenceId}`
  if (task.referenceType === 'job') return `/job/${task.referenceId}`
  return null
}

async function handleComplete(id: number) {
  if (await coachStore.completeTask(id)) ElMessage.success('任务已完成')
}

async function handleReopen(id: number) {
  if (await coachStore.reopenTask(id)) ElMessage.success('任务已重开')
}

onMounted(() => coachStore.fetchPlan(Number(route.params.id)))
</script>

<style scoped>
.plan-page { display: flex; flex-direction: column; gap: 20px; }
.plan-header h1 { margin: 0; font-family: var(--font-heading); }
.plan-header p, .task-card p { color: var(--nb-muted); }
.day-section { display: flex; flex-direction: column; gap: 12px; }
.task-card__main { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.task-card__main h3 { margin: 0; }
.task-card__actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.tags { display: flex; gap: 8px; margin-top: 12px; }
@media (max-width: 768px) { .task-card__main { flex-direction: column; } .task-card__actions { justify-content: flex-start; } }
</style>
```

- [ ] **Step 4: Run frontend checks**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both commands pass. `build-only` may show the existing chunk size warning.

- [ ] **Step 5: Commit Task 6**

```powershell
git add frontend/src/views/coach
git commit -m "feat: add coach pages"
```

---

## Task 7: Full Verification and Final Fixes

**Files:**
- Modify only files needed to fix compile/test/type errors found during verification.

- [ ] **Step 1: Run backend coach tests**

Run from `backend/`:

```powershell
.\mvnw.cmd -Dtest=CoachServiceImplTest,CoachControllerTest test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run backend full test suite**

Run from `backend/`:

```powershell
.\mvnw.cmd test
```

Expected: all Phase 7 tests pass. If the pre-existing `InterviewReportEnhancementServiceImplTest.runTask_shouldThrowIfNotFound` flaky failure appears, document it separately and do not change unrelated code.

- [ ] **Step 3: Run frontend checks**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both pass. Existing Vite chunk-size warning is acceptable.

- [ ] **Step 4: Review diff**

Run from repo root:

```powershell
git status
git diff --stat
git diff -- docs/superpowers/specs/2026-06-13-phase7-ai-career-coach-design.md docs/superpowers/plans/2026-06-13-phase7-ai-career-coach-plan.md
```

Expected: no accidental spec changes except this plan file; implementation diffs match Tasks 1-6.

- [ ] **Step 5: Commit verification fixes if any**

If Step 1-4 required fixes, inspect changed files first:

```powershell
git status --short
```

Then stage only the files changed for verification fixes. Example when context-test mocks were adjusted:

```powershell
git add backend/src/test/java/com/mianshiba/ai/MianshibaAiBackendApplicationTests.java backend/src/test/java/com/mianshiba/ai/controller/StatisticsControllerTest.java
git commit -m "fix: stabilize coach verification"
```

If no fixes were required, do not create an empty commit.

---

## Self-Review Notes

- Spec coverage: schema, diagnosis history, plan history, task check-ins, AI fallback, frontend pages, routes, and verification are mapped to Tasks 1-7.
- Scope control: notifications, admin coach management, complex trend charts, and synchronization to `application_todo` are excluded.
- Type consistency: backend uses `CoachDiagnosis`, `CoachPlan`, `CoachTask`; frontend uses matching `Coach*VO` types and route names.
- Known implementation simplification: Task 2 starts with user-profile-only snapshot, then leaves room to add richer mapper aggregation inside `buildSnapshot` without changing public API. This keeps MVP implementable while preserving the spec contract.
