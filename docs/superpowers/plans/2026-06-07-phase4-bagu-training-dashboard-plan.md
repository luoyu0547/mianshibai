# Phase 4 AI 八股训练闭环与求职作战台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a job-search command center plus an AI-driven bagu interview training loop, while keeping algorithm training limited to external OJ recommendations.

**Architecture:** Add a focused backend training module with five persistence tables, one training service for plans/questions/answers/reviews/recommendations, and one dashboard service that aggregates existing application/interview analytics with training state. Add a frontend training data layer, upgrade the home page into a dashboard, and add training center/detail/question pages.

**Tech Stack:** Spring Boot 3.5.x, Java 17, MyBatis-Plus, Spring AI `ChatClient`, Jackson JSON parsing, Vue 3, TypeScript, Pinia, Vue Router, Element Plus, existing Neubrutalism components.

---

## File Structure

### Backend Files

- Modify: `backend/src/main/resources/sql/init.sql` — add 5 Phase 4 tables.
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java` — add training error codes.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingPlan.java` — training plan entity.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingQuestion.java` — bagu question entity.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingAnswer.java` — user answer entity.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingAnswerReview.java` — AI review entity.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/AlgorithmRecommendation.java` — external OJ recommendation entity.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/TrainingPlanMapper.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/TrainingQuestionMapper.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/TrainingAnswerMapper.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/TrainingAnswerReviewMapper.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/AlgorithmRecommendationMapper.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/training/TrainingPlanGenerateRequest.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/training/TrainingAnswerSubmitRequest.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingPlanVO.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingQuestionVO.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingAnswerVO.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingAnswerReviewVO.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/AlgorithmRecommendationVO.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/dashboard/DashboardVO.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/service/TrainingService.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/service/DashboardService.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/TrainingServiceImpl.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DashboardServiceImpl.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/controller/TrainingController.java`.
- Create: `backend/src/main/java/com/mianshiba/ai/controller/DashboardController.java`.
- Test: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase4Test.java`.
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/TrainingServiceImplTest.java`.
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/DashboardServiceImplTest.java`.
- Test: `backend/src/test/java/com/mianshiba/ai/controller/TrainingControllerTest.java`.

### Frontend Files

- Create: `frontend/src/types/training.ts` — training and dashboard TypeScript types.
- Create: `frontend/src/api/training.ts` — training API functions.
- Create: `frontend/src/api/dashboard.ts` — dashboard API function.
- Create: `frontend/src/stores/training.ts` — Pinia training store.
- Create: `frontend/src/stores/dashboard.ts` — Pinia dashboard store.
- Modify: `frontend/src/router/index.ts` — add training routes.
- Modify: `frontend/src/layouts/MainLayout.vue` — add training navigation item.
- Modify: `frontend/src/views/home/HomePage.vue` — replace welcome page with dashboard.
- Create: `frontend/src/views/training/TrainingCenterPage.vue`.
- Create: `frontend/src/views/training/TrainingPlanDetailPage.vue`.
- Create: `frontend/src/views/training/TrainingQuestionPage.vue`.

---

### Task 1: Backend Training Schema, Error Codes, Entities, And Mappers

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingPlan.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingQuestion.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingAnswer.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingAnswerReview.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/AlgorithmRecommendation.java`
- Create: five mapper interfaces under `backend/src/main/java/com/mianshiba/ai/mapper/`
- Test: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase4Test.java`

- [ ] **Step 1: Write failing SQL regression test**

Create `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase4Test.java`:

```java
package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlPhase4Test {

    @Test
    void initSqlContainsPhase4TrainingTables() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_plan");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_question");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_answer");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_answer_review");
        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS algorithm_recommendation");
    }
}
```

- [ ] **Step 2: Run failing test**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase4Test
```

Expected: FAIL because Phase 4 tables do not exist.

- [ ] **Step 3: Add SQL tables**

Append to `backend/src/main/resources/sql/init.sql` after Phase 3 tables:

```sql
CREATE TABLE IF NOT EXISTS training_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) DEFAULT 'manual',
    source_id BIGINT NULL,
    target_days INT DEFAULT 7,
    status VARCHAR(32) DEFAULT 'active',
    summary TEXT NULL,
    focus_topics JSON NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    INDEX idx_user_status (user_id, status),
    INDEX idx_user_create_time (user_id, create_time)
);

CREATE TABLE IF NOT EXISTS training_question (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    day_index INT DEFAULT 1,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    topic VARCHAR(64) NULL,
    skill_tags JSON NULL,
    difficulty VARCHAR(32) DEFAULT 'medium',
    source_type VARCHAR(32) DEFAULT 'manual',
    reference_answer TEXT NULL,
    follow_up_questions JSON NULL,
    status VARCHAR(32) DEFAULT 'pending',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    INDEX idx_user_plan (user_id, plan_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_plan_day (plan_id, day_index)
);

CREATE TABLE IF NOT EXISTS training_answer (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_text TEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    INDEX idx_user_question (user_id, question_id),
    INDEX idx_question_create_time (question_id, create_time)
);

CREATE TABLE IF NOT EXISTS training_answer_review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    answer_id BIGINT NOT NULL,
    total_score INT DEFAULT 0,
    accuracy_score INT DEFAULT 0,
    clarity_score INT DEFAULT 0,
    depth_score INT DEFAULT 0,
    project_score INT DEFAULT 0,
    strengths_json JSON NULL,
    mistakes_json JSON NULL,
    missing_points_json JSON NULL,
    suggestions_json JSON NULL,
    recommended_answer TEXT NULL,
    follow_up_questions_json JSON NULL,
    mastery_level VARCHAR(32) DEFAULT 'basic',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    INDEX idx_user_question (user_id, question_id),
    INDEX idx_answer (answer_id)
);

CREATE TABLE IF NOT EXISTS algorithm_recommendation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL,
    platform VARCHAR(64) DEFAULT 'LeetCode',
    problem_ref VARCHAR(255) NOT NULL,
    reason TEXT NULL,
    completed TINYINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    INDEX idx_user_plan (user_id, plan_id),
    INDEX idx_user_completed (user_id, completed)
);
```

- [ ] **Step 4: Add training error codes**

Modify `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java` and add these enum entries near existing module errors:

```java
TRAINING_PLAN_NOT_FOUND_ERROR(40440, "训练计划不存在"),
TRAINING_QUESTION_NOT_FOUND_ERROR(40441, "训练题不存在"),
TRAINING_ANSWER_NOT_FOUND_ERROR(40442, "训练答案不存在"),
```

- [ ] **Step 5: Create entity classes**

Create `TrainingPlan.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "training_plan", autoResultMap = true)
public class TrainingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String sourceType;
    private Long sourceId;
    private Integer targetDays;
    private String status;
    private String summary;
    @TableField(typeHandler = com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler.class)
    private List<String> focusTopics;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDelete;
}
```

Create the other entities with the same annotations and fields matching SQL. JSON list fields use `@TableField(typeHandler = JacksonTypeHandler.class)`:

```java
// TrainingQuestion fields: id, userId, planId, dayIndex, title, content, topic,
// skillTags List<String>, difficulty, sourceType, referenceAnswer,
// followUpQuestions List<String>, status, createTime, updateTime, isDelete.
// TrainingAnswer fields: id, userId, questionId, answerText, createTime, updateTime, isDelete.
// TrainingAnswerReview fields: id, userId, questionId, answerId, totalScore,
// accuracyScore, clarityScore, depthScore, projectScore, strengthsJson List<String>,
// mistakesJson List<String>, missingPointsJson List<String>, suggestionsJson List<String>,
// recommendedAnswer, followUpQuestionsJson List<String>, masteryLevel, createTime, updateTime, isDelete.
// AlgorithmRecommendation fields: id, userId, planId, category, platform, problemRef,
// reason, completed, createTime, updateTime, isDelete.
```

- [ ] **Step 6: Create mapper interfaces**

Each mapper has the same shape. Example `TrainingPlanMapper.java`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.TrainingPlan;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainingPlanMapper extends BaseMapper<TrainingPlan> {
}
```

Create corresponding interfaces for `TrainingQuestion`, `TrainingAnswer`, `TrainingAnswerReview`, and `AlgorithmRecommendation`.

- [ ] **Step 7: Run backend build and SQL test**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase4Test
.\mvnw.cmd clean package -DskipTests
```

Expected: both BUILD SUCCESS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java backend/src/main/java/com/mianshiba/ai/model/entity backend/src/main/java/com/mianshiba/ai/mapper backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase4Test.java
git commit -m "feat: add training schema"
```

---

### Task 2: Backend Training DTOs, VOs, Service, And Controller

**Files:**
- Create DTOs under `backend/src/main/java/com/mianshiba/ai/model/dto/training/`
- Create VOs under `backend/src/main/java/com/mianshiba/ai/model/vo/training/`
- Create: `backend/src/main/java/com/mianshiba/ai/service/TrainingService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/TrainingServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/controller/TrainingController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/TrainingServiceImplTest.java`

- [ ] **Step 1: Write failing service tests for non-AI operations**

Create `TrainingServiceImplTest.java` with tests for active plan lookup, ownership checks, status updates, and algorithm recommendation completion:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.*;
import com.mianshiba.ai.model.entity.*;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingServiceImplTest {
    @Mock JwtUtils jwtUtils;
    @Mock TrainingPlanMapper planMapper;
    @Mock TrainingQuestionMapper questionMapper;
    @Mock TrainingAnswerMapper answerMapper;
    @Mock TrainingAnswerReviewMapper reviewMapper;
    @Mock AlgorithmRecommendationMapper algorithmMapper;

    @InjectMocks TrainingServiceImpl trainingService;

    private void mockUser() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "testuser", "user"));
    }

    @Test
    void markQuestionMastered_updatesOwnedQuestion() {
        mockUser();
        TrainingQuestion question = new TrainingQuestion();
        question.setId(10L);
        question.setUserId(1L);
        question.setStatus("reviewed");
        when(questionMapper.selectById(10L)).thenReturn(question);

        trainingService.markQuestionMastered("Bearer test-token", 10L);

        assertThat(question.getStatus()).isEqualTo("mastered");
        verify(questionMapper).updateById(question);
    }

    @Test
    void getQuestion_rejectsOtherUsersQuestion() {
        mockUser();
        TrainingQuestion question = new TrainingQuestion();
        question.setId(10L);
        question.setUserId(2L);
        when(questionMapper.selectById(10L)).thenReturn(question);

        assertThatThrownBy(() -> trainingService.getQuestion("Bearer test-token", 10L))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo(ErrorCode.TRAINING_QUESTION_NOT_FOUND_ERROR.getCode());
    }

    @Test
    void completeAlgorithmRecommendation_updatesOwnedRecommendation() {
        mockUser();
        AlgorithmRecommendation recommendation = new AlgorithmRecommendation();
        recommendation.setId(3L);
        recommendation.setUserId(1L);
        recommendation.setCompleted(0);
        when(algorithmMapper.selectById(3L)).thenReturn(recommendation);

        trainingService.completeAlgorithmRecommendation("Bearer test-token", 3L);

        assertThat(recommendation.getCompleted()).isEqualTo(1);
        verify(algorithmMapper).updateById(recommendation);
    }
}
```

- [ ] **Step 2: Run failing tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=TrainingServiceImplTest
```

Expected: FAIL because service, DTO, VO, and methods do not exist.

- [ ] **Step 3: Create DTO classes**

Create `TrainingPlanGenerateRequest.java`:

```java
package com.mianshiba.ai.model.dto.training;

import lombok.Data;

@Data
public class TrainingPlanGenerateRequest {
    private String sourceType;
    private Long sourceId;
    private Integer targetDays;
    private String targetPosition;
}
```

Create `TrainingAnswerSubmitRequest.java`:

```java
package com.mianshiba.ai.model.dto.training;

import lombok.Data;

@Data
public class TrainingAnswerSubmitRequest {
    private String answerText;
}
```

- [ ] **Step 4: Create VO classes**

Create `TrainingAnswerReviewVO.java`:

```java
package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrainingAnswerReviewVO {
    private Long id;
    private Long answerId;
    private Integer totalScore;
    private Integer accuracyScore;
    private Integer clarityScore;
    private Integer depthScore;
    private Integer projectScore;
    private List<String> strengths;
    private List<String> mistakes;
    private List<String> missingPoints;
    private List<String> suggestions;
    private String recommendedAnswer;
    private List<String> followUpQuestions;
    private String masteryLevel;
    private LocalDateTime createTime;
}
```

Create `TrainingAnswerVO`, `TrainingQuestionVO`, `AlgorithmRecommendationVO`, and `TrainingPlanVO` with fields matching entities plus nested lists:

```java
// TrainingAnswerVO: id, questionId, answerText, TrainingAnswerReviewVO review, createTime.
// TrainingQuestionVO: id, planId, dayIndex, title, content, topic, skillTags,
// difficulty, sourceType, referenceAnswer, followUpQuestions, status,
// Integer latestScore, String latestMasteryLevel, createTime, updateTime.
// AlgorithmRecommendationVO: id, planId, category, platform, problemRef, reason, completed.
// TrainingPlanVO: id, title, sourceType, sourceId, targetDays, status, summary,
// focusTopics, List<TrainingQuestionVO> questions,
// List<AlgorithmRecommendationVO> algorithmRecommendations, createTime, updateTime.
```

- [ ] **Step 5: Create service interface**

Create `TrainingService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest;
import com.mianshiba.ai.model.dto.training.TrainingPlanGenerateRequest;
import com.mianshiba.ai.model.vo.training.TrainingAnswerVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;

import java.util.List;

public interface TrainingService {
    TrainingPlanVO generatePlan(String authorizationHeader, TrainingPlanGenerateRequest request);
    TrainingPlanVO getActivePlan(String authorizationHeader);
    List<TrainingPlanVO> listPlans(String authorizationHeader);
    TrainingPlanVO getPlan(String authorizationHeader, Long id);
    Boolean archivePlan(String authorizationHeader, Long id);
    Boolean completePlan(String authorizationHeader, Long id);
    TrainingQuestionVO getQuestion(String authorizationHeader, Long id);
    Boolean markQuestionMastered(String authorizationHeader, Long id);
    Boolean skipQuestion(String authorizationHeader, Long id);
    TrainingAnswerVO submitAnswer(String authorizationHeader, Long questionId, TrainingAnswerSubmitRequest request);
    List<TrainingAnswerVO> listQuestionAnswers(String authorizationHeader, Long questionId);
    Boolean completeAlgorithmRecommendation(String authorizationHeader, Long id);
    Boolean reopenAlgorithmRecommendation(String authorizationHeader, Long id);
}
```

- [ ] **Step 6: Implement non-AI service operations**

Create `TrainingServiceImpl.java` with constants, mapper injection, `resolveUserId`, ownership helpers, and VO conversion. `generatePlan` and `submitAnswer` can call private methods added in Task 3, but must compile now with deterministic fallback stubs:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TrainingServiceImpl implements TrainingService {
    private static final Set<String> PLAN_STATUSES = Set.of("active", "completed", "archived");
    private static final Set<String> QUESTION_STATUSES = Set.of("pending", "answered", "reviewed", "mastered", "skipped");

    private final JwtUtils jwtUtils;
    private final TrainingPlanMapper planMapper;
    private final TrainingQuestionMapper questionMapper;
    private final TrainingAnswerMapper answerMapper;
    private final TrainingAnswerReviewMapper reviewMapper;
    private final AlgorithmRecommendationMapper algorithmMapper;

    @Override
    public TrainingQuestionVO getQuestion(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        TrainingQuestion question = getOwnedQuestion(id, userId);
        return toQuestionVO(question);
    }

    @Override
    public Boolean markQuestionMastered(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        TrainingQuestion question = getOwnedQuestion(id, userId);
        question.setStatus("mastered");
        questionMapper.updateById(question);
        return true;
    }

    @Override
    public Boolean completeAlgorithmRecommendation(String authorizationHeader, Long id) {
        Long userId = resolveUserId(authorizationHeader);
        AlgorithmRecommendation recommendation = getOwnedAlgorithmRecommendation(id, userId);
        recommendation.setCompleted(1);
        algorithmMapper.updateById(recommendation);
        return true;
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        return jwtUtils.parseToken(token).userId();
    }

    private TrainingQuestion getOwnedQuestion(Long id, Long userId) {
        TrainingQuestion question = questionMapper.selectById(id);
        if (question == null || !userId.equals(question.getUserId())) {
            throw new BusinessException(ErrorCode.TRAINING_QUESTION_NOT_FOUND_ERROR);
        }
        return question;
    }
}
```

Complete all interface methods in the same file using MyBatis-Plus queries scoped by `userId`.

- [ ] **Step 7: Create controller**

Create `TrainingController.java`:

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.training.TrainingAnswerSubmitRequest;
import com.mianshiba.ai.model.dto.training.TrainingPlanGenerateRequest;
import com.mianshiba.ai.model.vo.training.TrainingAnswerVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;
import com.mianshiba.ai.service.TrainingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/training")
@Tag(name = "八股训练接口")
public class TrainingController {
    private final TrainingService trainingService;

    @PostMapping("/plan/generate")
    public BaseResponse<TrainingPlanVO> generatePlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                     @RequestBody TrainingPlanGenerateRequest request) {
        return ResultUtils.success(trainingService.generatePlan(auth, request));
    }

    @GetMapping("/plan/active")
    public BaseResponse<TrainingPlanVO> getActivePlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(trainingService.getActivePlan(auth));
    }

    @GetMapping("/plan")
    public BaseResponse<List<TrainingPlanVO>> listPlans(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(trainingService.listPlans(auth));
    }

    @GetMapping("/plan/{id}")
    public BaseResponse<TrainingPlanVO> getPlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.getPlan(auth, id));
    }

    @PutMapping("/plan/{id}/archive")
    public BaseResponse<Boolean> archivePlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.archivePlan(auth, id));
    }

    @PutMapping("/plan/{id}/complete")
    public BaseResponse<Boolean> completePlan(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.completePlan(auth, id));
    }

    @GetMapping("/question/{id}")
    public BaseResponse<TrainingQuestionVO> getQuestion(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.getQuestion(auth, id));
    }

    @PutMapping("/question/{id}/master")
    public BaseResponse<Boolean> masterQuestion(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.markQuestionMastered(auth, id));
    }

    @PutMapping("/question/{id}/skip")
    public BaseResponse<Boolean> skipQuestion(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.skipQuestion(auth, id));
    }

    @PostMapping("/question/{id}/answer")
    public BaseResponse<TrainingAnswerVO> submitAnswer(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
                                                       @PathVariable Long id,
                                                       @RequestBody TrainingAnswerSubmitRequest request) {
        return ResultUtils.success(trainingService.submitAnswer(auth, id, request));
    }

    @GetMapping("/question/{id}/answers")
    public BaseResponse<List<TrainingAnswerVO>> listQuestionAnswers(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.listQuestionAnswers(auth, id));
    }

    @PutMapping("/algorithm/{id}/complete")
    public BaseResponse<Boolean> completeAlgorithm(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.completeAlgorithmRecommendation(auth, id));
    }

    @PutMapping("/algorithm/{id}/reopen")
    public BaseResponse<Boolean> reopenAlgorithm(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth, @PathVariable Long id) {
        return ResultUtils.success(trainingService.reopenAlgorithmRecommendation(auth, id));
    }
}
```

- [ ] **Step 8: Run tests and build**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=TrainingServiceImplTest
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 9: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/dto/training backend/src/main/java/com/mianshiba/ai/model/vo/training backend/src/main/java/com/mianshiba/ai/service/TrainingService.java backend/src/main/java/com/mianshiba/ai/service/impl/TrainingServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/TrainingController.java backend/src/test/java/com/mianshiba/ai/service/impl/TrainingServiceImplTest.java
git commit -m "feat: add training management API"
```

---

### Task 3: AI Plan Generation And Bagu Answer Review

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/TrainingServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/TrainingServiceImplTest.java`

- [ ] **Step 1: Add failing tests for AI JSON parsing and persistence**

Extend `TrainingServiceImplTest` with mocked `ChatClient` if feasible, or use a package-private parsing method test. The minimum required tests:

```java
@Test
void submitAnswer_rejectsBlankAnswer() {
    TrainingAnswerSubmitRequest request = new TrainingAnswerSubmitRequest();
    request.setAnswerText("   ");

    assertThatThrownBy(() -> trainingService.submitAnswer("Bearer test-token", 1L, request))
            .isInstanceOf(BusinessException.class)
            .extracting("code").isEqualTo(ErrorCode.PARAMS_ERROR.getCode());
}

@Test
void generatePlan_defaultsTargetDaysToSeven() {
    mockUser();
    TrainingPlanGenerateRequest request = new TrainingPlanGenerateRequest();
    request.setSourceType("manual");

    TrainingPlanVO vo = trainingService.generatePlan("Bearer test-token", request);

    assertThat(vo.getTargetDays()).isEqualTo(7);
}
```

- [ ] **Step 2: Run failing tests**

```powershell
.\mvnw.cmd test -Dtest=TrainingServiceImplTest
```

Expected: FAIL until AI/fallback generation and blank validation are implemented.

- [ ] **Step 3: Inject AI and context mappers**

Modify `TrainingServiceImpl` constructor dependencies:

```java
private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);

private final ChatClient chatClient;
private final InterviewReportEnhancementMapper enhancementMapper;
private final InterviewReportMapper reportMapper;
private final InterviewSessionMapper sessionMapper;
private final JobApplicationMapper applicationMapper;
private final ResumeMapper resumeMapper;
```

- [ ] **Step 4: Implement `generatePlan` with AI fallback**

`generatePlan` must:

- Resolve user.
- Archive existing active plans for the user.
- Build context from latest enhancements, applications, the user's latest resume, and recently active job applications.
- Call AI for structured JSON.
- If AI is unavailable in tests, use deterministic fallback plan content.
- Insert one `TrainingPlan`, multiple `TrainingQuestion`, and multiple `AlgorithmRecommendation` rows.
- Return `TrainingPlanVO`.

Use this prompt shape:

```java
private static final String PLAN_PROMPT = "你是一位 Java 后端面试教练。请基于用户短板生成八股训练计划和算法外部 OJ 刷题建议。" +
        "只返回 JSON，不要解释。JSON 字段：title, summary, targetDays, focusTopics, questions, algorithmRecommendations。" +
        "questions 每项包含 dayIndex,title,content,topic,skillTags,difficulty,referenceAnswer,followUpQuestions。" +
        "algorithmRecommendations 每项包含 category,platform,problemRef,reason。" +
        "算法只推荐去 LeetCode、力扣、CodeTop 等平台刷题，不要求在本系统提交代码。";
```

- [ ] **Step 5: Implement `submitAnswer` with AI review**

`submitAnswer` must:

- Reject blank or longer than 8000 characters answer text with `PARAMS_ERROR`.
- Verify question ownership.
- Insert `TrainingAnswer`.
- Call AI review prompt.
- Insert `TrainingAnswerReview`.
- Update question status to `reviewed`.
- Return `TrainingAnswerVO` including review.

Use this review prompt shape:

```java
private static final String REVIEW_PROMPT = "你是一位严格的程序员八股面试官。请批改用户答案。" +
        "只返回 JSON，字段：totalScore,accuracyScore,clarityScore,depthScore,projectScore," +
        "strengths,mistakes,missingPoints,suggestions,recommendedAnswer,followUpQuestions,masteryLevel。" +
        "masteryLevel 只能是 weak,basic,good,mastered。推荐回答要适合面试口述。";
```

- [ ] **Step 6: Add JSON extraction helpers**

Use the same approach as `ResumeAiServiceImpl`:

```java
private String extractJsonFromResponse(String response) {
    Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(response);
    if (matcher.find()) {
        return matcher.group(1).trim();
    }
    return response.trim();
}
```

- [ ] **Step 7: Run backend tests**

```powershell
.\mvnw.cmd test -Dtest=TrainingServiceImplTest
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/TrainingServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/TrainingServiceImplTest.java
git commit -m "feat: add ai bagu training generation"
```

---

### Task 4: Dashboard Aggregation API

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/dashboard/DashboardVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/DashboardService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/DashboardServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/controller/DashboardController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/DashboardServiceImplTest.java`

- [ ] **Step 1: Write failing dashboard service test**

Create `DashboardServiceImplTest.java`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.*;
import com.mianshiba.ai.model.entity.ApplicationTodo;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {
    @Mock JwtUtils jwtUtils;
    @Mock ApplicationTodoMapper todoMapper;
    @Mock JobApplicationMapper applicationMapper;
    @Mock TrainingPlanMapper planMapper;
    @Mock TrainingQuestionMapper questionMapper;
    @Mock TrainingAnswerReviewMapper reviewMapper;
    @Mock AlgorithmRecommendationMapper algorithmMapper;

    @InjectMocks DashboardServiceImpl dashboardService;

    @Test
    void getDashboard_includesTodosAndTrainingQuestions() {
        when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
        when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "testuser", "user"));

        ApplicationTodo todo = new ApplicationTodo();
        todo.setId(1L);
        todo.setTitle("跟进 HR");
        when(todoMapper.selectList(any())).thenReturn(List.of(todo));

        TrainingQuestion question = new TrainingQuestion();
        question.setId(2L);
        question.setTitle("讲讲 MySQL 索引");
        question.setStatus("pending");
        when(questionMapper.selectList(any())).thenReturn(List.of(question));

        var vo = dashboardService.getDashboard("Bearer test-token");

        assertThat(vo.getTodayPriorities()).isNotEmpty();
        assertThat(vo.getPendingQuestions()).hasSize(1);
    }
}
```

- [ ] **Step 2: Run failing test**

```powershell
.\mvnw.cmd test -Dtest=DashboardServiceImplTest
```

Expected: FAIL because dashboard classes do not exist.

- [ ] **Step 3: Create DashboardVO**

Create `DashboardVO.java`:

```java
package com.mianshiba.ai.model.vo.dashboard;

import com.mianshiba.ai.model.vo.application.ApplicationStatsVO;
import com.mianshiba.ai.model.vo.training.AlgorithmRecommendationVO;
import com.mianshiba.ai.model.vo.training.TrainingPlanVO;
import com.mianshiba.ai.model.vo.training.TrainingQuestionVO;
import lombok.Data;

import java.util.List;

@Data
public class DashboardVO {
    private List<String> todayPriorities;
    private ApplicationStatsVO applicationStats;
    private TrainingPlanVO activePlan;
    private List<TrainingQuestionVO> pendingQuestions;
    private List<String> weakTopics;
    private List<AlgorithmRecommendationVO> algorithmRecommendations;
}
```

- [ ] **Step 4: Create service and controller**

Create `DashboardService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.vo.dashboard.DashboardVO;

public interface DashboardService {
    DashboardVO getDashboard(String authorizationHeader);
}
```

Create `DashboardController.java`:

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dashboard")
@Tag(name = "求职作战台接口")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public BaseResponse<DashboardVO> getDashboard(@RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResultUtils.success(dashboardService.getDashboard(auth));
    }
}
```

- [ ] **Step 5: Implement DashboardServiceImpl**

Aggregate:

- Top 5 incomplete `ApplicationTodo` ordered by due date and priority.
- Application stats using the same status groups from `ApplicationServiceImpl`.
- Active training plan.
- Top 5 pending/reviewed training questions.
- Weak topics from latest `TrainingAnswerReview.masteryLevel` in `weak/basic`.
- Top 5 incomplete algorithm recommendations.

- [ ] **Step 6: Run tests and build**

```powershell
.\mvnw.cmd test -Dtest=DashboardServiceImplTest
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/vo/dashboard backend/src/main/java/com/mianshiba/ai/service/DashboardService.java backend/src/main/java/com/mianshiba/ai/service/impl/DashboardServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/DashboardController.java backend/src/test/java/com/mianshiba/ai/service/impl/DashboardServiceImplTest.java
git commit -m "feat: add job search dashboard API"
```

---

### Task 5: Backend Controller Tests And Full Backend Verification

**Files:**
- Create: `backend/src/test/java/com/mianshiba/ai/controller/TrainingControllerTest.java`
- Create: `backend/src/test/java/com/mianshiba/ai/controller/DashboardControllerTest.java`

- [ ] **Step 1: Write controller tests**

Create `TrainingControllerTest.java` using `@WebMvcTest(TrainingController.class)` and mocked `TrainingService`. Cover:

```java
@Test
void getActivePlan_returnsSuccessResponse() throws Exception {
    TrainingPlanVO vo = new TrainingPlanVO();
    vo.setId(1L);
    vo.setTitle("Java 后端八股强化");
    when(trainingService.getActivePlan("Bearer test-token")).thenReturn(vo);

    mockMvc.perform(get("/api/training/plan/active")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.title").value("Java 后端八股强化"));
}
```

Also cover `POST /api/training/question/{id}/answer`.

- [ ] **Step 2: Run targeted backend tests**

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase4Test,TrainingServiceImplTest,DashboardServiceImplTest,TrainingControllerTest
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Run package verification**

```powershell
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Commit**

```powershell
git add backend/src/test/java/com/mianshiba/ai/controller/TrainingControllerTest.java backend/src/test/java/com/mianshiba/ai/controller/DashboardControllerTest.java
git commit -m "test: cover training and dashboard APIs"
```

---

### Task 6: Frontend Training Types, API, Store, And Routes

**Files:**
- Create: `frontend/src/types/training.ts`
- Create: `frontend/src/api/training.ts`
- Create: `frontend/src/api/dashboard.ts`
- Create: `frontend/src/stores/training.ts`
- Create: `frontend/src/stores/dashboard.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`
- Create stubs: `frontend/src/views/training/TrainingCenterPage.vue`, `TrainingPlanDetailPage.vue`, `TrainingQuestionPage.vue`

- [ ] **Step 1: Create frontend types**

Create `training.ts` with union types and interfaces:

```ts
export type TrainingPlanStatus = 'active' | 'completed' | 'archived'
export type TrainingQuestionStatus = 'pending' | 'answered' | 'reviewed' | 'mastered' | 'skipped'
export type TrainingDifficulty = 'easy' | 'medium' | 'hard'
export type MasteryLevel = 'weak' | 'basic' | 'good' | 'mastered'

export interface TrainingPlanGenerateRequest {
  sourceType?: string
  sourceId?: number
  targetDays?: number
  targetPosition?: string
}

export interface TrainingAnswerSubmitRequest {
  answerText: string
}

export interface TrainingAnswerReviewVO {
  id: number
  answerId: number
  totalScore: number
  accuracyScore: number
  clarityScore: number
  depthScore: number
  projectScore: number
  strengths: string[]
  mistakes: string[]
  missingPoints: string[]
  suggestions: string[]
  recommendedAnswer: string
  followUpQuestions: string[]
  masteryLevel: MasteryLevel
  createTime: string
}
```

Also define `TrainingAnswerVO`, `TrainingQuestionVO`, `AlgorithmRecommendationVO`, `TrainingPlanVO`, and `DashboardVO` matching backend VOs.

- [ ] **Step 2: Create API functions**

Create `frontend/src/api/training.ts`:

```ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { TrainingAnswerSubmitRequest, TrainingAnswerVO, TrainingPlanGenerateRequest, TrainingPlanVO, TrainingQuestionVO } from '@/types/training'

export function generateTrainingPlan(data: TrainingPlanGenerateRequest) {
  return request.post<BaseResponse<TrainingPlanVO>>('/api/training/plan/generate', data)
}

export function getActiveTrainingPlan() {
  return request.get<BaseResponse<TrainingPlanVO | null>>('/api/training/plan/active')
}

export function listTrainingPlans() {
  return request.get<BaseResponse<TrainingPlanVO[]>>('/api/training/plan')
}

export function getTrainingPlan(id: number) {
  return request.get<BaseResponse<TrainingPlanVO>>(`/api/training/plan/${id}`)
}

export function getTrainingQuestion(id: number) {
  return request.get<BaseResponse<TrainingQuestionVO>>(`/api/training/question/${id}`)
}

export function submitTrainingAnswer(id: number, data: TrainingAnswerSubmitRequest) {
  return request.post<BaseResponse<TrainingAnswerVO>>(`/api/training/question/${id}/answer`, data)
}
```

Add remaining archive/complete/master/skip/answers/algorithm complete/reopen functions.

Create `frontend/src/api/dashboard.ts`:

```ts
import request from '@/utils/request'
import type { BaseResponse } from '@/types/user'
import type { DashboardVO } from '@/types/training'

export function getDashboard() {
  return request.get<BaseResponse<DashboardVO>>('/api/dashboard')
}
```

- [ ] **Step 3: Create Pinia stores**

Create `training.ts` store with refs: `plans`, `activePlan`, `currentPlan`, `currentQuestion`, `answers`, `loading`. Add actions wrapping every API function.

Create `dashboard.ts` store with refs: `dashboard`, `loading`, and action `fetchDashboard()`.

- [ ] **Step 4: Add routes and nav**

Modify `frontend/src/router/index.ts`:

```ts
{
  path: '/training',
  name: 'training',
  component: () => import('@/views/training/TrainingCenterPage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/training/plan/:id',
  name: 'training-plan-detail',
  component: () => import('@/views/training/TrainingPlanDetailPage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/training/question/:id',
  name: 'training-question',
  component: () => import('@/views/training/TrainingQuestionPage.vue'),
  meta: { requiresAuth: true },
}
```

Modify `MainLayout.vue` to add a `训练中心` navigation item to `/training`.

- [ ] **Step 5: Create page stubs**

Each stub should compile:

```vue
<template>
  <MainLayout>
    <NbCard>
      <h2>训练中心</h2>
      <p>八股训练数据层已接入，后续任务将替换为完整页面。</p>
    </NbCard>
  </MainLayout>
</template>

<script setup lang="ts">
import MainLayout from '@/layouts/MainLayout.vue'
import NbCard from '@/components/NbCard.vue'
</script>
```

- [ ] **Step 6: Verify frontend types**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/types/training.ts frontend/src/api/training.ts frontend/src/api/dashboard.ts frontend/src/stores/training.ts frontend/src/stores/dashboard.ts frontend/src/router/index.ts frontend/src/layouts/MainLayout.vue frontend/src/views/training
git commit -m "feat: add training frontend data layer"
```

---

### Task 7: Frontend Dashboard And Training Center Pages

**Files:**
- Modify: `frontend/src/views/home/HomePage.vue`
- Modify: `frontend/src/views/training/TrainingCenterPage.vue`
- Modify: `frontend/src/views/training/TrainingPlanDetailPage.vue`

- [ ] **Step 1: Replace home page with dashboard**

Use `useDashboardStore()` and `onMounted(fetchDashboard)`. Render:

- Welcome card.
- Today priorities.
- Application stats.
- Active plan progress.
- Pending bagu questions.
- Weak topics.
- Algorithm recommendations.

Use `NbCard`, `NbButton`, `el-tag`, `el-progress`, and `router-link`.

- [ ] **Step 2: Build training center page**

Implement `TrainingCenterPage.vue`:

- Load active plan and plan list on mount.
- Button `生成训练计划` opens a small Element Plus form for target days and position.
- Call `trainingStore.generatePlan(form)`.
- Show current plan with progress.
- Show questions grouped by `dayIndex`.
- Show algorithm recommendations with complete/reopen button.
- Link questions to `/training/question/:id`.

- [ ] **Step 3: Build training plan detail page**

Implement `TrainingPlanDetailPage.vue`:

- Load by route param.
- Show plan summary and focus topics.
- Show questions grouped by day.
- Show latest score/mastery if present.
- Show algorithm recommendations.
- Buttons archive/complete plan.

- [ ] **Step 4: Run frontend verification**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both PASS.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/views/home/HomePage.vue frontend/src/views/training/TrainingCenterPage.vue frontend/src/views/training/TrainingPlanDetailPage.vue
git commit -m "feat: add dashboard and training center pages"
```

---

### Task 8: Frontend Question Answer Page And Full Verification

**Files:**
- Modify: `frontend/src/views/training/TrainingQuestionPage.vue`

- [ ] **Step 1: Implement question page**

Build `TrainingQuestionPage.vue` with:

- Load question and answers on mount.
- Show title, topic, tags, difficulty, source type, content.
- Textarea answer input.
- Submit button calls `submitAnswer`.
- Review card shows score dimensions, strengths, mistakes, missing points, suggestions, recommended answer, follow-up questions, mastery level.
- History section lists all answers and reviews.
- Buttons: mark mastered, skip, back to training.

- [ ] **Step 2: Run frontend verification**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both PASS.

- [ ] **Step 3: Run backend final verification**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase4Test,TrainingServiceImplTest,DashboardServiceImplTest,TrainingControllerTest
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 4: Check git state and recent commits**

Run from repo root:

```powershell
git status --short
git log --oneline -12
```

Expected: only intended files changed before commit; recent Phase 4 commits visible.

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/views/training/TrainingQuestionPage.vue
git commit -m "feat: add bagu answer review page"
```

- [ ] **Step 6: Final handoff summary**

Report:

- Backend targeted tests result.
- Backend package result.
- Frontend type-check result.
- Frontend build-only result.
- Commit list for Phase 4.
- Any known limitations: algorithm suggestions are external OJ recommendations only; no OJ integration or algorithm answer review.

---

## Plan Self-Review

- Spec coverage: Dashboard, training plan generation, bagu question answer submission, AI review persistence, mastery tracking, algorithm OJ recommendations, permissions, and verification are each mapped to tasks.
- Scope check: The plan does not include OJ judging, algorithm answer storage, external account integration, reminders, public question bank, or courses.
- Type consistency: Backend names use `TrainingPlan`, `TrainingQuestion`, `TrainingAnswer`, `TrainingAnswerReview`, and `AlgorithmRecommendation`; frontend uses the corresponding `VO` suffix types.
- Testing coverage: Includes SQL regression, service ownership/status tests, AI/validation tests, dashboard aggregation tests, controller tests, backend package, frontend type-check, and frontend build.
