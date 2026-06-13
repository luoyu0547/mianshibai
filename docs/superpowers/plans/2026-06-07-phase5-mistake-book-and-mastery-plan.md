# Phase 5 八股错题本与知识点掌握度 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a bagu mistake book, topic/tag mastery analytics, and dashboard review summaries on top of Phase 4 answer reviews.

**Architecture:** Keep mistakes as a derived view from `training_question`, `training_answer`, and `training_answer_review`, and add one `training_mastery` aggregate table for topic/tag mastery. Add a `TrainingReviewService` for mistake/mastery queries and rebuilds, extend `TrainingServiceImpl` to refresh mastery after review/mastery changes, extend dashboard output, and add frontend mistake/mastery pages.

**Tech Stack:** Spring Boot 3.5.x, Java 17, MyBatis-Plus, MySQL JSON fields, Vue 3, TypeScript, Pinia, Vue Router, Element Plus, existing Neubrutalism components.

---

## File Structure

### Backend Files

- Modify: `backend/src/main/resources/sql/init.sql` — add `training_mastery` table.
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingMastery.java` — cached mastery aggregate entity.
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/TrainingMasteryMapper.java` — MyBatis-Plus mapper.
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/training/TrainingMistakeQueryRequest.java` — mistake filters.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMistakeVO.java` — mistake book item.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMasteryVO.java` — mastery item.
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMasterySummaryVO.java` — weak/basic/good/mastered counts.
- Create: `backend/src/main/java/com/mianshiba/ai/service/TrainingReviewService.java` — mistake and mastery API contract.
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImpl.java` — query, refresh, rebuild implementation.
- Create: `backend/src/main/java/com/mianshiba/ai/controller/TrainingReviewController.java` — `/api/training/review` endpoints.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/TrainingServiceImpl.java` — call mastery refresh after answer review and question mastered.
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/dashboard/DashboardVO.java` — add reviewQuestions, weakMasteries, masterySummary.
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/DashboardServiceImpl.java` — populate review fields.
- Test: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase5Test.java`.
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImplTest.java`.
- Test: `backend/src/test/java/com/mianshiba/ai/controller/TrainingReviewControllerTest.java`.
- Modify test: `backend/src/test/java/com/mianshiba/ai/service/impl/TrainingServiceImplTest.java`.
- Modify test: `backend/src/test/java/com/mianshiba/ai/service/impl/DashboardServiceImplTest.java`.

### Frontend Files

- Modify: `frontend/src/types/training.ts` — add mistake/mastery/dashboard fields.
- Modify: `frontend/src/api/training.ts` — add review endpoints.
- Modify: `frontend/src/stores/training.ts` — add mistake/mastery state and actions.
- Modify: `frontend/src/router/index.ts` — add `/training/mistakes` and `/training/mastery`.
- Modify: `frontend/src/views/training/TrainingCenterPage.vue` — add entry cards.
- Create: `frontend/src/views/training/TrainingMistakePage.vue` — mistake book page.
- Create: `frontend/src/views/training/TrainingMasteryPage.vue` — mastery page.
- Modify: `frontend/src/views/home/HomePage.vue` — show review questions/mastery summary.

---

### Task 1: Backend Mastery Schema, Entity, Mapper, And SQL Test

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingMastery.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/TrainingMasteryMapper.java`
- Test: `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase5Test.java`

- [ ] **Step 1: Write failing SQL regression test**

Create `backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase5Test.java`:

```java
package com.mianshiba.ai.sql;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InitSqlPhase5Test {

    @Test
    void initSqlContainsTrainingMasteryTable() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/sql/init.sql"), StandardCharsets.UTF_8);

        assertThat(sql).contains("CREATE TABLE IF NOT EXISTS training_mastery");
        assertThat(sql).contains("target_type VARCHAR(32) NOT NULL");
        assertThat(sql).contains("target_name VARCHAR(128) NOT NULL");
        assertThat(sql).contains("average_score DECIMAL(5,2) DEFAULT 0");
    }
}
```

- [ ] **Step 2: Run failing SQL test**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase5Test
```

Expected: FAIL because `training_mastery` table does not exist.

- [ ] **Step 3: Add `training_mastery` SQL table**

Append after Phase 4 training tables in `backend/src/main/resources/sql/init.sql`:

```sql
CREATE TABLE IF NOT EXISTS training_mastery (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_name VARCHAR(128) NOT NULL,
    practice_count INT DEFAULT 0,
    question_count INT DEFAULT 0,
    average_score DECIMAL(5,2) DEFAULT 0,
    weak_count INT DEFAULT 0,
    mastered_count INT DEFAULT 0,
    mastery_level VARCHAR(32) DEFAULT 'basic',
    last_practiced_at DATETIME NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_delete TINYINT DEFAULT 0,
    UNIQUE KEY uk_user_target (user_id, target_type, target_name),
    INDEX idx_user_type_level (user_id, target_type, mastery_level),
    INDEX idx_user_last_practiced (user_id, last_practiced_at)
);
```

- [ ] **Step 4: Create `TrainingMastery` entity**

Create `backend/src/main/java/com/mianshiba/ai/model/entity/TrainingMastery.java`:

```java
package com.mianshiba.ai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("training_mastery")
public class TrainingMastery {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String targetType;
    private String targetName;
    private Integer practiceCount;
    private Integer questionCount;
    private BigDecimal averageScore;
    private Integer weakCount;
    private Integer masteredCount;
    private String masteryLevel;
    private LocalDateTime lastPracticedAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer isDelete;
}
```

- [ ] **Step 5: Create `TrainingMasteryMapper`**

Create `backend/src/main/java/com/mianshiba/ai/mapper/TrainingMasteryMapper.java`:

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.TrainingMastery;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrainingMasteryMapper extends BaseMapper<TrainingMastery> {
}
```

- [ ] **Step 6: Verify Task 1**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=InitSqlPhase5Test
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS for both commands.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/resources/sql/init.sql backend/src/main/java/com/mianshiba/ai/model/entity/TrainingMastery.java backend/src/main/java/com/mianshiba/ai/mapper/TrainingMasteryMapper.java backend/src/test/java/com/mianshiba/ai/sql/InitSqlPhase5Test.java
git commit -m "feat: add training mastery schema"
```

---

### Task 2: Backend Review DTOs, VOs, Service Contract, And Mastery Refresh

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/training/TrainingMistakeQueryRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMistakeVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMasteryVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMasterySummaryVO.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/TrainingReviewService.java`
- Create: `backend/src/main/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImpl.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/TrainingServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImplTest.java`

- [ ] **Step 1: Write failing mastery refresh test**

Create `backend/src/test/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImplTest.java`:

```java
package com.mianshiba.ai.service.impl;

import com.mianshiba.ai.mapper.TrainingAnswerReviewMapper;
import com.mianshiba.ai.mapper.TrainingMasteryMapper;
import com.mianshiba.ai.mapper.TrainingQuestionMapper;
import com.mianshiba.ai.model.entity.TrainingAnswerReview;
import com.mianshiba.ai.model.entity.TrainingMastery;
import com.mianshiba.ai.model.entity.TrainingQuestion;
import com.mianshiba.ai.utils.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrainingReviewServiceImplTest {

    @Mock JwtUtils jwtUtils;
    @Mock TrainingQuestionMapper questionMapper;
    @Mock TrainingAnswerReviewMapper reviewMapper;
    @Mock TrainingMasteryMapper masteryMapper;

    @InjectMocks TrainingReviewServiceImpl reviewService;

    @Test
    void refreshMasteryForQuestion_upsertsTopicAndSkillTags() {
        TrainingQuestion question = new TrainingQuestion();
        question.setId(10L);
        question.setUserId(1L);
        question.setTopic("MySQL");
        question.setSkillTags(List.of("索引", "事务"));
        when(questionMapper.selectById(10L)).thenReturn(question);

        TrainingAnswerReview review = new TrainingAnswerReview();
        review.setQuestionId(10L);
        review.setTotalScore(55);
        review.setMasteryLevel("weak");
        when(reviewMapper.selectList(any())).thenReturn(List.of(review));
        when(masteryMapper.selectOne(any())).thenReturn(null);

        reviewService.refreshMasteryForQuestion(1L, 10L);

        ArgumentCaptor<TrainingMastery> captor = ArgumentCaptor.forClass(TrainingMastery.class);
        verify(masteryMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertThat(captor.getAllValues()).extracting(TrainingMastery::getTargetName)
                .containsExactlyInAnyOrder("MySQL", "索引", "事务");
    }
}
```

- [ ] **Step 2: Run failing test**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=TrainingReviewServiceImplTest
```

Expected: FAIL because `TrainingReviewServiceImpl` does not exist.

- [ ] **Step 3: Create DTO and VO classes**

Create `TrainingMistakeQueryRequest.java`:

```java
package com.mianshiba.ai.model.dto.training;

import lombok.Data;

@Data
public class TrainingMistakeQueryRequest {
    private String topic;
    private String masteryLevel;
    private Boolean includeMastered;
    private Integer scoreMax;
}
```

Create `TrainingMistakeVO.java`:

```java
package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TrainingMistakeVO {
    private Long questionId;
    private Long planId;
    private String title;
    private String content;
    private String topic;
    private List<String> skillTags;
    private String difficulty;
    private String status;
    private Integer latestScore;
    private String masteryLevel;
    private List<String> mistakes;
    private List<String> missingPoints;
    private List<String> suggestions;
    private String recommendedAnswer;
    private LocalDateTime lastAnsweredAt;
}
```

Create `TrainingMasteryVO.java`:

```java
package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TrainingMasteryVO {
    private Long id;
    private String targetType;
    private String targetName;
    private Integer practiceCount;
    private Integer questionCount;
    private BigDecimal averageScore;
    private Integer weakCount;
    private Integer masteredCount;
    private String masteryLevel;
    private LocalDateTime lastPracticedAt;
}
```

Create `TrainingMasterySummaryVO.java`:

```java
package com.mianshiba.ai.model.vo.training;

import lombok.Data;

@Data
public class TrainingMasterySummaryVO {
    private Long weak;
    private Long basic;
    private Long good;
    private Long mastered;
}
```

- [ ] **Step 4: Create service interface**

Create `TrainingReviewService.java`:

```java
package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.training.TrainingMistakeQueryRequest;
import com.mianshiba.ai.model.vo.training.TrainingMasterySummaryVO;
import com.mianshiba.ai.model.vo.training.TrainingMasteryVO;
import com.mianshiba.ai.model.vo.training.TrainingMistakeVO;

import java.util.List;

public interface TrainingReviewService {
    List<TrainingMistakeVO> listMistakes(String authorizationHeader, TrainingMistakeQueryRequest request);
    List<TrainingMasteryVO> listTopicMastery(String authorizationHeader);
    List<TrainingMasteryVO> listSkillTagMastery(String authorizationHeader);
    TrainingMasterySummaryVO getMasterySummary(String authorizationHeader);
    Boolean rebuildMastery(String authorizationHeader);
    void refreshMasteryForQuestion(Long userId, Long questionId);
}
```

- [ ] **Step 5: Implement `TrainingReviewServiceImpl` refresh and mastery query methods**

Create `TrainingReviewServiceImpl.java` with `@Service`, `@RequiredArgsConstructor`, `@Slf4j`. Implement:

- `resolveUserId` using `JwtUtils`.
- `refreshMasteryForQuestion(Long userId, Long questionId)`:
  - Load owned question.
  - Load all reviews for this user/question.
  - Build one aggregate for topic and one per skillTag.
  - Upsert each aggregate into `training_mastery` by `(userId, targetType, targetName)`.
- `calculateMasteryLevel(averageScore, weakCount, practiceCount)`:
  - average < 60 or weakCount/practiceCount >= 0.5 => `weak`.
  - average < 75 => `basic`.
  - average < 90 => `good`.
  - else `mastered`.
- `listTopicMastery`: query `targetType = topic`, order weak first then average asc.
- `listSkillTagMastery`: query `targetType = skill_tag`, order weak first then average asc.
- `getMasterySummary`: count topic masteries by level.
- `rebuildMastery`: delete current user's mastery rows, iterate reviewed questions for user, call `refreshMasteryForQuestion`.

Use exact target type constants:

```java
private static final String TARGET_TYPE_TOPIC = "topic";
private static final String TARGET_TYPE_SKILL_TAG = "skill_tag";
private static final Set<String> WEAK_LEVELS = Set.of("weak", "basic");
```

- [ ] **Step 6: Wire refresh into `TrainingServiceImpl`**

Modify `TrainingServiceImpl`:

- Add dependency: `private final TrainingReviewService trainingReviewService;`
- After `reviewMapper.insert(review);` and question status update in `submitAnswer`, call:

```java
trainingReviewService.refreshMasteryForQuestion(userId, questionId);
```

- After `questionMapper.updateById(question);` in `markQuestionMastered`, call:

```java
Long userId = resolveUserId(authorizationHeader);
trainingReviewService.refreshMasteryForQuestion(userId, id);
```

Avoid parsing the token twice by storing `userId` at the start of the method if you refactor it.

- [ ] **Step 7: Run tests and build**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=TrainingReviewServiceImplTest,TrainingServiceImplTest
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS. If `TrainingServiceImplTest` requires a new mock for `TrainingReviewService`, add it.

- [ ] **Step 8: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/dto/training/TrainingMistakeQueryRequest.java backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMistakeVO.java backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMasteryVO.java backend/src/main/java/com/mianshiba/ai/model/vo/training/TrainingMasterySummaryVO.java backend/src/main/java/com/mianshiba/ai/service/TrainingReviewService.java backend/src/main/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImpl.java backend/src/main/java/com/mianshiba/ai/service/impl/TrainingServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImplTest.java backend/src/test/java/com/mianshiba/ai/service/impl/TrainingServiceImplTest.java
git commit -m "feat: add training mastery aggregation"
```

---

### Task 3: Backend Mistake Book And Mastery API

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImpl.java`
- Create: `backend/src/main/java/com/mianshiba/ai/controller/TrainingReviewController.java`
- Test: `backend/src/test/java/com/mianshiba/ai/controller/TrainingReviewControllerTest.java`

- [ ] **Step 1: Add failing mistake query service test**

Extend `TrainingReviewServiceImplTest` with:

```java
@Test
void listMistakes_excludesMasteredQuestionsByDefault() {
    when(jwtUtils.resolveToken("Bearer test-token")).thenReturn("test-token");
    when(jwtUtils.parseToken("test-token")).thenReturn(new JwtUtils.JwtUserClaims(1L, "testuser", "user"));

    TrainingQuestion mastered = new TrainingQuestion();
    mastered.setId(1L);
    mastered.setUserId(1L);
    mastered.setStatus("mastered");
    mastered.setTopic("Redis");

    TrainingQuestion weak = new TrainingQuestion();
    weak.setId(2L);
    weak.setUserId(1L);
    weak.setStatus("reviewed");
    weak.setTopic("MySQL");

    when(questionMapper.selectList(any())).thenReturn(List.of(mastered, weak));

    TrainingAnswerReview weakReview = new TrainingAnswerReview();
    weakReview.setQuestionId(2L);
    weakReview.setTotalScore(55);
    weakReview.setMasteryLevel("weak");
    when(reviewMapper.selectOne(any())).thenReturn(weakReview);

    TrainingMistakeQueryRequest request = new TrainingMistakeQueryRequest();
    List<TrainingMistakeVO> mistakes = reviewService.listMistakes("Bearer test-token", request);

    assertThat(mistakes).hasSize(1);
    assertThat(mistakes.get(0).getQuestionId()).isEqualTo(2L);
}
```

- [ ] **Step 2: Run failing test**

```powershell
.\mvnw.cmd test -Dtest=TrainingReviewServiceImplTest
```

Expected: FAIL until `listMistakes` is implemented.

- [ ] **Step 3: Implement `listMistakes`**

Rules:

- Resolve user ID.
- Query all user's reviewed/answered/mastered questions, and apply a topic filter when `request.getTopic()` has text.
- For each question, load latest review by `questionId` ordered by `createTime desc limit 1`.
- Skip if no review.
- If `includeMastered` is not true and question status is `mastered`, skip.
- Mistake condition: latest mastery `weak/basic` or latest score < 70.
- Apply `masteryLevel` and `scoreMax` filters.
- Convert to `TrainingMistakeVO`.
- Sort weak before basic before score ascending, then latest answer time desc if available.
- Limit to a practical maximum of 100 results.

- [ ] **Step 4: Create controller**

Create `TrainingReviewController.java`:

```java
package com.mianshiba.ai.controller;

import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.model.dto.training.TrainingMistakeQueryRequest;
import com.mianshiba.ai.model.vo.training.TrainingMasterySummaryVO;
import com.mianshiba.ai.model.vo.training.TrainingMasteryVO;
import com.mianshiba.ai.model.vo.training.TrainingMistakeVO;
import com.mianshiba.ai.service.TrainingReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/training/review")
@Tag(name = "八股复习接口")
public class TrainingReviewController {
    private final TrainingReviewService trainingReviewService;

    @GetMapping("/mistakes")
    @Operation(summary = "查询错题本")
    public BaseResponse<List<TrainingMistakeVO>> listMistakes(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            TrainingMistakeQueryRequest request) {
        return ResultUtils.success(trainingReviewService.listMistakes(authorizationHeader, request));
    }

    @GetMapping("/mastery")
    @Operation(summary = "查询 topic 掌握度")
    public BaseResponse<List<TrainingMasteryVO>> listTopicMastery(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResultUtils.success(trainingReviewService.listTopicMastery(authorizationHeader));
    }

    @GetMapping("/mastery/tags")
    @Operation(summary = "查询 skillTag 掌握度")
    public BaseResponse<List<TrainingMasteryVO>> listSkillTagMastery(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResultUtils.success(trainingReviewService.listSkillTagMastery(authorizationHeader));
    }

    @GetMapping("/mastery/summary")
    @Operation(summary = "查询掌握度摘要")
    public BaseResponse<TrainingMasterySummaryVO> getMasterySummary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResultUtils.success(trainingReviewService.getMasterySummary(authorizationHeader));
    }

    @PostMapping("/mastery/rebuild")
    @Operation(summary = "重建掌握度统计")
    public BaseResponse<Boolean> rebuildMastery(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        return ResultUtils.success(trainingReviewService.rebuildMastery(authorizationHeader));
    }
}
```

- [ ] **Step 5: Create controller test**

Create `TrainingReviewControllerTest.java` with `@WebMvcTest(TrainingReviewController.class)`, mocked `TrainingReviewService`, and tests for `/mistakes`, `/mastery`, `/mastery/tags`, `/mastery/summary`, `/mastery/rebuild`.

Minimum test:

```java
@Test
void listMistakes_returnsSuccessResponse() throws Exception {
    TrainingMistakeVO vo = new TrainingMistakeVO();
    vo.setQuestionId(1L);
    vo.setTitle("讲讲 MySQL 索引");
    when(trainingReviewService.listMistakes(eq("Bearer test-token"), any())).thenReturn(List.of(vo));

    mockMvc.perform(get("/api/training/review/mistakes")
            .header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data[0].title").value("讲讲 MySQL 索引"));
}
```

- [ ] **Step 6: Verify Task 3**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=TrainingReviewServiceImplTest,TrainingReviewControllerTest
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 7: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImpl.java backend/src/main/java/com/mianshiba/ai/controller/TrainingReviewController.java backend/src/test/java/com/mianshiba/ai/service/impl/TrainingReviewServiceImplTest.java backend/src/test/java/com/mianshiba/ai/controller/TrainingReviewControllerTest.java
git commit -m "feat: add training mistake review API"
```

---

### Task 4: Dashboard Review Summary Enhancement

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/dashboard/DashboardVO.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/DashboardServiceImpl.java`
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/DashboardServiceImplTest.java`

- [ ] **Step 1: Add dashboard test for review fields**

Extend `DashboardServiceImplTest`:

```java
@Test
void getDashboard_includesReviewQuestionsAndMasterySummary() {
    mockUser();
    when(todoMapper.selectList(any())).thenReturn(List.of());
    when(applicationMapper.selectList(any())).thenReturn(List.of());
    when(questionMapper.selectList(any())).thenReturn(List.of());
    when(reviewMapper.selectList(any())).thenReturn(List.of());
    when(algorithmMapper.selectList(any())).thenReturn(List.of());

    TrainingMistakeVO mistake = new TrainingMistakeVO();
    mistake.setQuestionId(10L);
    mistake.setTitle("讲讲 Redis 缓存穿透");
    when(trainingReviewService.listMistakes(eq("Bearer test-token"), any())).thenReturn(List.of(mistake));

    TrainingMasteryVO mastery = new TrainingMasteryVO();
    mastery.setTargetName("Redis");
    mastery.setMasteryLevel("weak");
    when(trainingReviewService.listTopicMastery("Bearer test-token")).thenReturn(List.of(mastery));

    TrainingMasterySummaryVO summary = new TrainingMasterySummaryVO();
    summary.setWeak(1L);
    summary.setBasic(0L);
    summary.setGood(0L);
    summary.setMastered(0L);
    when(trainingReviewService.getMasterySummary("Bearer test-token")).thenReturn(summary);

    DashboardVO dashboard = dashboardService.getDashboard("Bearer test-token");

    assertThat(dashboard.getReviewQuestions()).hasSize(1);
    assertThat(dashboard.getWeakMasteries()).hasSize(1);
    assertThat(dashboard.getMasterySummary().getWeak()).isEqualTo(1L);
}
```

Add `@Mock TrainingReviewService trainingReviewService` to the test and constructor dependencies.

- [ ] **Step 2: Run failing dashboard test**

```powershell
.\mvnw.cmd test -Dtest=DashboardServiceImplTest
```

Expected: FAIL because dashboard fields do not exist yet.

- [ ] **Step 3: Extend `DashboardVO`**

Add fields:

```java
private List<TrainingMistakeVO> reviewQuestions;
private List<TrainingMasteryVO> weakMasteries;
private TrainingMasterySummaryVO masterySummary;
```

Add imports for the three VOs.

- [ ] **Step 4: Extend `DashboardServiceImpl`**

Inject `TrainingReviewService trainingReviewService`.

In `getDashboard`, set:

```java
dashboard.setReviewQuestions(buildReviewQuestions(authorizationHeader));
dashboard.setWeakMasteries(buildWeakMasteries(authorizationHeader));
dashboard.setMasterySummary(buildMasterySummary(authorizationHeader));
```

Implementation rules:

- `buildReviewQuestions`: call `trainingReviewService.listMistakes(auth, new TrainingMistakeQueryRequest())`, limit 5, catch exceptions and return empty list.
- `buildWeakMasteries`: call `trainingReviewService.listTopicMastery(auth)`, filter masteryLevel `weak/basic`, limit 5, catch exceptions and return empty list.
- `buildMasterySummary`: call `trainingReviewService.getMasterySummary(auth)`, catch exceptions and return empty summary with zeros.

- [ ] **Step 5: Verify Task 4**

Run from `backend/`:

```powershell
.\mvnw.cmd test -Dtest=DashboardServiceImplTest
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```powershell
git add backend/src/main/java/com/mianshiba/ai/model/vo/dashboard/DashboardVO.java backend/src/main/java/com/mianshiba/ai/service/impl/DashboardServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/DashboardServiceImplTest.java
git commit -m "feat: enhance dashboard with review mastery"
```

---

### Task 5: Backend Full Phase 5 Verification

**Files:**
- Modify tests only if verification reveals constructor or mock drift.

- [ ] **Step 1: Run all Phase 5 targeted tests**

Run from `backend/`:

```powershell
.\mvnw.cmd test "-Dtest=InitSqlPhase5Test,TrainingReviewServiceImplTest,TrainingReviewControllerTest,TrainingServiceImplTest,DashboardServiceImplTest"
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run backend package**

Run from `backend/`:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 3: Confirm verification left no pending changes**

Run from repo root:

```powershell
git status --short
```

Expected: no output. If there is output, inspect it before proceeding and only continue after the working tree is clean or the intended fix has been committed with a focused message.

---

### Task 6: Frontend Types, API, Store, Routes, And Training Center Entry

**Files:**
- Modify: `frontend/src/types/training.ts`
- Modify: `frontend/src/api/training.ts`
- Modify: `frontend/src/stores/training.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/views/training/TrainingCenterPage.vue`
- Create stubs: `frontend/src/views/training/TrainingMistakePage.vue`, `frontend/src/views/training/TrainingMasteryPage.vue`

- [ ] **Step 1: Extend training types**

Add to `frontend/src/types/training.ts`:

```ts
export interface TrainingMistakeQueryRequest {
  topic?: string
  masteryLevel?: MasteryLevel
  includeMastered?: boolean
  scoreMax?: number
}

export interface TrainingMistakeVO {
  questionId: number
  planId: number
  title: string
  content: string
  topic: string
  skillTags: string[]
  difficulty: TrainingDifficulty
  status: TrainingQuestionStatus
  latestScore: number
  masteryLevel: MasteryLevel
  mistakes: string[]
  missingPoints: string[]
  suggestions: string[]
  recommendedAnswer: string
  lastAnsweredAt: string
}

export interface TrainingMasteryVO {
  id: number
  targetType: 'topic' | 'skill_tag'
  targetName: string
  practiceCount: number
  questionCount: number
  averageScore: number
  weakCount: number
  masteredCount: number
  masteryLevel: MasteryLevel
  lastPracticedAt: string | null
}

export interface TrainingMasterySummaryVO {
  weak: number
  basic: number
  good: number
  mastered: number
}
```

Extend `DashboardVO`:

```ts
reviewQuestions: TrainingMistakeVO[]
weakMasteries: TrainingMasteryVO[]
masterySummary: TrainingMasterySummaryVO | null
```

- [ ] **Step 2: Add API functions**

Add to `frontend/src/api/training.ts`:

```ts
export function listTrainingMistakes(params?: TrainingMistakeQueryRequest) {
  return request.get<BaseResponse<TrainingMistakeVO[]>>('/api/training/review/mistakes', { params })
}

export function listTopicMastery() {
  return request.get<BaseResponse<TrainingMasteryVO[]>>('/api/training/review/mastery')
}

export function listSkillTagMastery() {
  return request.get<BaseResponse<TrainingMasteryVO[]>>('/api/training/review/mastery/tags')
}

export function getTrainingMasterySummary() {
  return request.get<BaseResponse<TrainingMasterySummaryVO>>('/api/training/review/mastery/summary')
}

export function rebuildTrainingMastery() {
  return request.post<BaseResponse<boolean>>('/api/training/review/mastery/rebuild')
}
```

Update imports for the new types.

- [ ] **Step 3: Extend training store**

Add refs:

```ts
const mistakes = ref<TrainingMistakeVO[]>([])
const topicMasteries = ref<TrainingMasteryVO[]>([])
const skillTagMasteries = ref<TrainingMasteryVO[]>([])
const masterySummary = ref<TrainingMasterySummaryVO | null>(null)
```

Add actions: `fetchMistakes`, `fetchTopicMastery`, `fetchSkillTagMastery`, `fetchMasterySummary`, `rebuildMastery`.

- [ ] **Step 4: Add routes**

Modify `frontend/src/router/index.ts`:

```ts
{
  path: '/training/mistakes',
  name: 'training-mistakes',
  component: () => import('@/views/training/TrainingMistakePage.vue'),
  meta: { requiresAuth: true },
},
{
  path: '/training/mastery',
  name: 'training-mastery',
  component: () => import('@/views/training/TrainingMasteryPage.vue'),
  meta: { requiresAuth: true },
},
```

- [ ] **Step 5: Create page stubs and entry cards**

Create `TrainingMistakePage.vue` and `TrainingMasteryPage.vue` with `MainLayout` and `NbCard` compile-safe stubs.

Modify `TrainingCenterPage.vue` near the top to add two entry cards linking to `/training/mistakes` and `/training/mastery`.

- [ ] **Step 6: Verify frontend data layer**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: PASS.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/types/training.ts frontend/src/api/training.ts frontend/src/stores/training.ts frontend/src/router/index.ts frontend/src/views/training/TrainingCenterPage.vue frontend/src/views/training/TrainingMistakePage.vue frontend/src/views/training/TrainingMasteryPage.vue
git commit -m "feat: add training review frontend data layer"
```

---

### Task 7: Frontend Mistake Book, Mastery Page, Dashboard Enhancement, And Full Verification

**Files:**
- Modify: `frontend/src/views/training/TrainingMistakePage.vue`
- Modify: `frontend/src/views/training/TrainingMasteryPage.vue`
- Modify: `frontend/src/views/home/HomePage.vue`

- [ ] **Step 1: Implement `TrainingMistakePage.vue`**

Features:

- Load `trainingStore.fetchMistakes(filters)` on mount.
- Filters: topic input, masteryLevel select, includeMastered switch, scoreMax select.
- Summary cards: total mistakes, weak count, basic count, mastered status count.
- Mistake cards: title, topic, skill tags, difficulty, latest score, mastery level, mistakes, missing points, suggestions, recommended answer summary.
- Actions: link to `/training/question/:questionId`, button `标记已掌握` calls `trainingStore.masterQuestion(questionId)` then reloads mistakes.
- Empty state: `暂无错题，继续完成八股训练后这里会自动出现需要复习的题目。`

- [ ] **Step 2: Implement `TrainingMasteryPage.vue`**

Features:

- Load topic mastery, skillTag mastery, and summary on mount.
- Summary cards for weak/basic/good/mastered.
- Topic table/cards: targetName, masteryLevel tag, averageScore, practiceCount, weakCount, masteredCount, lastPracticedAt.
- Skill tag section: top weak tags as cards.
- Rebuild button calls `trainingStore.rebuildMastery()` then reloads.
- Clicking a topic navigates to `/training/mistakes?topic=<targetName>`.

- [ ] **Step 3: Enhance `HomePage.vue` dashboard**

Add three sections if data exists:

- `dashboard.reviewQuestions`: card titled `今日复习` with up to 5 questions linking to question page.
- `dashboard.weakMasteries`: card titled `薄弱知识点` showing mastery tags.
- `dashboard.masterySummary`: small summary row with weak/basic/good/mastered counts.

If the new fields are null or empty, keep existing dashboard modules working.

- [ ] **Step 4: Run frontend verification**

Run from `frontend/`:

```powershell
npm run type-check
npm run build-only
```

Expected: both PASS.

- [ ] **Step 5: Run backend final verification**

Run from `backend/`:

```powershell
.\mvnw.cmd test "-Dtest=InitSqlPhase5Test,TrainingReviewServiceImplTest,TrainingReviewControllerTest,TrainingServiceImplTest,DashboardServiceImplTest"
.\mvnw.cmd clean package -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 6: Check repo state**

Run from repo root:

```powershell
git status --short
git log --oneline -12
```

Expected: only intended frontend files changed before commit.

- [ ] **Step 7: Commit**

```powershell
git add frontend/src/views/training/TrainingMistakePage.vue frontend/src/views/training/TrainingMasteryPage.vue frontend/src/views/home/HomePage.vue
git commit -m "feat: add mistake book and mastery pages"
```

- [ ] **Step 8: Final handoff summary**

Report:

- Backend targeted tests result.
- Backend package result.
- Frontend type-check result.
- Frontend build-only result.
- Phase 5 commits.
- Confirm algorithm recommendations are still excluded from mistake/mastery statistics.

---

## Plan Self-Review

- Spec coverage: `training_mastery`, mistake book, mastery pages, dashboard review fields, mark mastered refresh, and algorithm exclusion are each mapped to tasks.
- Scope check: This plan does not implement AI async work, complex spaced repetition, notifications, public question banks, or algorithm correctness tracking.
- Type consistency: Backend uses `TrainingMistakeVO`, `TrainingMasteryVO`, `TrainingMasterySummaryVO`; frontend mirrors those names and Dashboard adds `reviewQuestions`, `weakMasteries`, and `masterySummary`.
- Verification: SQL, service, controller, dashboard, frontend type-check, frontend build, backend package, and final repo state are explicitly covered.
