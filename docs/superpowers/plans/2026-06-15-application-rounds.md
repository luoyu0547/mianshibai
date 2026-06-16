# 投递面试轮次功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为投递管理增加自定义面试轮次，支持每轮独立设置时间、标记通过/淘汰

**Architecture:** 新增 `application_round` 子表，将硬编码面试状态改为轮次驱动；`interviewing` 阶段不再固定第一/二/终面，交由轮次表定义

**Tech Stack:** Spring Boot 3.5, MyBatis-Plus, MySQL, Vue 3, TypeScript

---

### Task 1: 新增 `application_round` 建表语句

**Files:**
- Modify: `backend/src/main/resources/sql/init.sql`

- [ ] **Step 1: 在 init.sql 末尾追加建表语句**

```sql
CREATE TABLE IF NOT EXISTS application_round (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '轮次 id',
  application_id BIGINT NOT NULL COMMENT '关联投递记录 id',
  round_name VARCHAR(64) NOT NULL COMMENT '轮次名称，如 技术一面、HR 面',
  round_order INT NOT NULL DEFAULT 0 COMMENT '排序号',
  scheduled_at DATETIME DEFAULT NULL COMMENT '面试时间',
  result VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '结果：pending/pass/fail',
  notes TEXT DEFAULT NULL COMMENT '备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_round_application_id (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='投递面试轮次表';
```

---

### Task 2: 后端实体 `ApplicationRound`

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/entity/ApplicationRound.java`
- Create: `backend/src/main/java/com/mianshiba/ai/mapper/ApplicationRoundMapper.java`

- [ ] **Step 1: 创建实体**

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
@TableName("application_round")
public class ApplicationRound implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private String roundName;
    private Integer roundOrder;
    private LocalDateTime scheduledAt;
    private String result;
    private String notes;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @TableLogic(value = "0", delval = "1")
    private Integer isDelete;
}
```

- [ ] **Step 2: 创建 Mapper**

```java
package com.mianshiba.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mianshiba.ai.model.entity.ApplicationRound;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ApplicationRoundMapper extends BaseMapper<ApplicationRound> {
}
```

---

### Task 3: DTO 和 VO

**Files:**
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationRoundCreateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationRoundUpdateRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/dto/application/ApplicationRoundResultRequest.java`
- Create: `backend/src/main/java/com/mianshiba/ai/model/vo/application/ApplicationRoundVO.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/application/JobApplicationVO.java`

- [ ] **Step 1: CreateRequest**

```java
package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationRoundCreateRequest {
    @NotBlank(message = "轮次名称不能为空")
    @Size(max = 64, message = "轮次名称不能超过64个字符")
    private String roundName;

    private LocalDateTime scheduledAt;
    private String notes;
}
```

- [ ] **Step 2: UpdateRequest**

```java
package com.mianshiba.ai.model.dto.application;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationRoundUpdateRequest {
    private String roundName;
    private LocalDateTime scheduledAt;
    private String notes;
}
```

- [ ] **Step 3: ResultRequest**

```java
package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ApplicationRoundResultRequest {
    @NotBlank(message = "结果不能为空")
    private String result;
}
```

- [ ] **Step 4: VO**

```java
package com.mianshiba.ai.model.vo.application;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ApplicationRoundVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long applicationId;
    private String roundName;
    private Integer roundOrder;
    private LocalDateTime scheduledAt;
    private String result;
    private String notes;
}
```

- [ ] **Step 5: JobApplicationVO 增加 rounds 字段**

在 `JobApplicationVO.java` 增加字段：
```java
    private List<ApplicationRoundVO> rounds;
```
并在 import 加 `import java.util.List;`（如已有则跳过）。

---

### Task 4: Service 接口新增轮次方法

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/ApplicationService.java`

- [ ] **Step 1: 接口增加轮次 CRUD**

```java
    // 轮次
    List<ApplicationRoundVO> listRounds(String authorizationHeader, Long applicationId);
    ApplicationRoundVO createRound(String authorizationHeader, Long applicationId, ApplicationRoundCreateRequest request);
    ApplicationRoundVO updateRound(String authorizationHeader, Long applicationId, Long roundId, ApplicationRoundUpdateRequest request);
    ApplicationRoundVO setRoundResult(String authorizationHeader, Long applicationId, Long roundId, ApplicationRoundResultRequest request);
    void deleteRound(String authorizationHeader, Long applicationId, Long roundId);
```

---

### Task 5: 主状态调整

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ApplicationServiceImpl.java`

- [ ] **Step 1: 调整状态集**

`VALID_STATUSES` 删除 `first_interview` / `second_interview` / `final_interview`，改为：
```java
    private static final Set<String> VALID_STATUSES = Set.of(
            "pending_submit", "submitted", "interviewing", "offer", "rejected", "withdrawn");
```

`STATUS_LABELS` 对应调整：
```java
    private static final Map<String, String> STATUS_LABELS = Map.ofEntries(
            Map.entry("pending_submit", "待投递"),
            Map.entry("submitted", "已投递"),
            Map.entry("interviewing", "面试中"),
            Map.entry("offer", "Offer"),
            Map.entry("rejected", "拒绝"),
            Map.entry("withdrawn", "放弃")
    );
```

删除 `INTERVIEWING_STATUSES`（不再需要），或在 `getStats` 里将 `interviewing` 单独统计。

- [ ] **Step 2: 实现轮次方法**

```java
    private final ApplicationRoundMapper roundMapper;

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
        // 如果投递还在 submitted 状态，自动推进到 interviewing
        if ("submitted".equals(app.getStatus())) {
            app.setStatus("interviewing");
            applicationMapper.updateById(app);
        }

        // 计算下一个排序号
        Long maxOrder = roundMapper.selectCount(
                Wrappers.lambdaQuery(ApplicationRound.class)
                        .eq(ApplicationRound::getApplicationId, applicationId));
        int nextOrder = maxOrder != null ? maxOrder.intValue() : 0;

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

        // 如果淘汰，自动将投递状态设为 rejected
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
```

- [ ] **Step 3: 修改 `toApplicationVO` 填充 rounds**

在 `getApplication` / `listApplications` 方法中，查询并填充 rounds。`toApplicationVO` 增加参数或内部查询：

修改 `getApplication`：
```java
    JobApplication app = applicationMapper.selectById(id);
    ...
    List<ApplicationRound> rounds = roundMapper.selectList(
            Wrappers.lambdaQuery(ApplicationRound.class)
                    .eq(ApplicationRound::getApplicationId, id)
                    .orderByAsc(ApplicationRound::getRoundOrder));
    return toApplicationVO(app, todos, rounds);
```

修改 `toApplicationVO` 签名增加 `List<ApplicationRound> rounds` 参数，填充 `vo.setRounds(rounds.stream().map(this::toRoundVO).collect(Collectors.toList()))`。

修改所有调用 `toApplicationVO` 的地方传入 rounds（`createApplication` 传空列表，`updateApplication` 查询传入）。

- [ ] **Step 4: 调整 `getStats`**

`interviewing` 从 `INTERVIEWING_STATUSES` 改为直接统计 `interviewing`：
```java
    stats.setInterviewing(statusCount.getOrDefault("interviewing", 0L));
```

---

### Task 6: Controller 端点和错误码

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/controller/ApplicationController.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`

- [ ] **Step 1: ErrorCode 新增轮次相关错误码**

```java
    APPLICATION_ROUND_NOT_FOUND_ERROR(40430, "面试轮次不存在"),
```

- [ ] **Step 2: Controller 新增轮次端点**

```java
    @GetMapping("/{id}/round")
    @Operation(summary = "获取面试轮次列表")
    public BaseResponse<List<ApplicationRoundVO>> listRounds(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id) {
        return ResultUtils.success(applicationService.listRounds(authorizationHeader, id));
    }

    @PostMapping("/{id}/round")
    @Operation(summary = "添加面试轮次")
    public BaseResponse<ApplicationRoundVO> createRound(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id,
            @Valid @RequestBody ApplicationRoundCreateRequest request) {
        return ResultUtils.success(applicationService.createRound(authorizationHeader, id, request));
    }

    @PutMapping("/{id}/round/{roundId}")
    @Operation(summary = "更新面试轮次")
    public BaseResponse<ApplicationRoundVO> updateRound(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id,
            @PathVariable("roundId") Long roundId,
            @RequestBody ApplicationRoundUpdateRequest request) {
        return ResultUtils.success(applicationService.updateRound(authorizationHeader, id, roundId, request));
    }

    @PutMapping("/{id}/round/{roundId}/result")
    @Operation(summary = "标记面试轮次结果")
    public BaseResponse<ApplicationRoundVO> setRoundResult(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id,
            @PathVariable("roundId") Long roundId,
            @Valid @RequestBody ApplicationRoundResultRequest request) {
        return ResultUtils.success(applicationService.setRoundResult(authorizationHeader, id, roundId, request));
    }

    @DeleteMapping("/{id}/round/{roundId}")
    @Operation(summary = "删除面试轮次")
    public BaseResponse<Void> deleteRound(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader,
            @PathVariable("id") Long id,
            @PathVariable("roundId") Long roundId) {
        applicationService.deleteRound(authorizationHeader, id, roundId);
        return ResultUtils.success(null);
    }
```

---

### Task 7: 前端类型和 API

**Files:**
- Modify: `frontend/src/types/application.ts`
- Modify: `frontend/src/api/application.ts`

- [ ] **Step 1: 类型定义**

在 `types/application.ts` 追加：
```typescript
export type RoundResult = 'pending' | 'pass' | 'fail'

export interface ApplicationRoundVO {
  id: number
  applicationId: number
  roundName: string
  roundOrder: number
  scheduledAt: string | null
  result: RoundResult
  notes: string | null
}

export interface ApplicationRoundCreateRequest {
  roundName: string
  scheduledAt?: string | null
  notes?: string
}

export interface ApplicationRoundUpdateRequest {
  roundName?: string
  scheduledAt?: string | null
  notes?: string
}

export interface ApplicationRoundResultRequest {
  result: RoundResult
}
```

`JobApplicationVO` 增加 `rounds: ApplicationRoundVO[]`

- [ ] **Step 2: API 层**

在 `api/application.ts` 追加：
```typescript
import type { ApplicationRoundVO, ApplicationRoundCreateRequest, ApplicationRoundUpdateRequest, ApplicationRoundResultRequest } from '@/types/application'

export function createApplicationRound(id: number, data: ApplicationRoundCreateRequest) {
  return request.post<BaseResponse<ApplicationRoundVO>>(`/api/application/${id}/round`, data)
}

export function listApplicationRounds(id: number) {
  return request.get<BaseResponse<ApplicationRoundVO[]>>(`/api/application/${id}/round`)
}

export function updateApplicationRound(id: number, roundId: number, data: ApplicationRoundUpdateRequest) {
  return request.put<BaseResponse<ApplicationRoundVO>>(`/api/application/${id}/round/${roundId}`, data)
}

export function setApplicationRoundResult(id: number, roundId: number, data: ApplicationRoundResultRequest) {
  return request.put<BaseResponse<ApplicationRoundVO>>(`/api/application/${id}/round/${roundId}/result`, data)
}

export function deleteApplicationRound(id: number, roundId: number) {
  return request.delete<BaseResponse<void>>(`/api/application/${id}/round/${roundId}`)
}
```

---

### Task 8: 前端详情页轮次时间线

**Files:**
- Modify: `frontend/src/views/application/ApplicationDetailPage.vue`
- Modify: `frontend/src/utils/statusMaps.ts`（如有需要）

- [ ] **Step 1: 在详情页增加轮次区块**

在 `ApplicationDetailPage.vue` 的 `<template>` 中，在投递基本信息下方增加"面试轮次"区块：

```html
<NbSectionTitle title="面试轮次" />

<div v-if="rounds.length === 0" class="app-detail__empty-rounds">
  <NbEmptyState title="暂无面试轮次" description="添加第一条面试轮次来追踪面试进度">
    <template #action>
      <NbButton variant="primary" @click="showRoundDialog = true">添加轮次</NbButton>
    </template>
  </NbEmptyState>
</div>

<div v-else class="app-detail__rounds">
  <div v-for="(round, index) in rounds" :key="round.id" class="app-detail__round-card">
    <div class="app-detail__round-order">{{ index + 1 }}</div>
    <div class="app-detail__round-body">
      <div class="app-detail__round-header">
        <span class="app-detail__round-name">{{ round.roundName }}</span>
        <NbStatusBadge :variant="roundResultVariant(round.result)" :label="roundResultLabel(round.result)" />
      </div>
      <div v-if="round.scheduledAt" class="app-detail__round-time">
        {{ formatDate(round.scheduledAt) }}
      </div>
      <div v-if="round.notes" class="app-detail__round-notes">{{ round.notes }}</div>
      <div class="app-detail__round-actions">
        <NbButton v-if="round.result === 'pending'" size="small" variant="success" @click="handleRoundResult(round.id, 'pass')">标记通过</NbButton>
        <NbButton v-if="round.result === 'pending'" size="small" variant="danger" @click="handleRoundResult(round.id, 'fail')">标记淘汰</NbButton>
        <NbButton size="small" variant="ghost" @click="editRound(round)">编辑</NbButton>
        <NbButton size="small" variant="ghost" @click="deleteRound(round.id)">删除</NbButton>
      </div>
    </div>
  </div>

  <NbButton variant="primary" block class="app-detail__add-round" @click="showRoundDialog = true">+ 添加轮次</NbButton>
</div>
```

- [ ] **Step 2: 新增轮次弹窗**

内联弹窗添加轮次：
```html
<el-dialog v-model="showRoundDialog" :title="editingRound ? '编辑轮次' : '添加轮次'" width="480px" destroy-on-close>
  <el-form :model="roundForm" label-position="top">
    <el-form-item label="轮次名称" required>
      <el-input v-model="roundForm.roundName" placeholder="如：技术一面、HR 面" />
    </el-form-item>
    <el-form-item label="面试时间">
      <el-date-picker v-model="roundForm.scheduledAt" type="datetime" placeholder="选择面试时间" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%;" />
    </el-form-item>
    <el-form-item label="备注">
      <el-input v-model="roundForm.notes" type="textarea" :rows="3" placeholder="备注信息" />
    </el-form-item>
  </el-form>
  <template #footer>
    <NbButton variant="ghost" @click="showRoundDialog = false">取消</NbButton>
    <NbButton variant="primary" :loading="roundLoading" @click="saveRound">保存</NbButton>
  </template>
</el-dialog>
```

- [ ] **Step 3: Script 部分**

```typescript
import { listApplicationRounds, createApplicationRound, updateApplicationRound, setApplicationRoundResult, deleteApplicationRound } from '@/api/application'
import type { ApplicationRoundVO, ApplicationRoundCreateRequest, ApplicationRoundUpdateRequest, RoundResult } from '@/types/application'

const rounds = ref<ApplicationRoundVO[]>([])
const showRoundDialog = ref(false)
const roundLoading = ref(false)
const editingRound = ref<ApplicationRoundVO | null>(null)

const roundForm = reactive({
  roundName: '',
  scheduledAt: null as string | null,
  notes: '',
})

watch(showRoundDialog, (val) => {
  if (!val) {
    editingRound.value = null
    roundForm.roundName = ''
    roundForm.scheduledAt = null
    roundForm.notes = ''
  } else if (editingRound.value) {
    roundForm.roundName = editingRound.value.roundName
    roundForm.scheduledAt = editingRound.value.scheduledAt
    roundForm.notes = editingRound.value.notes || ''
  }
})

function roundResultVariant(result: RoundResult): StatusVariant {
  return result === 'pass' ? 'success' : result === 'fail' ? 'danger' : 'muted'
}

function roundResultLabel(result: RoundResult): string {
  return result === 'pass' ? '通过' : result === 'fail' ? '淘汰' : '待定'
}

async function loadRounds() {
  const res = await listApplicationRounds(props.id)
  if (res.code === 0 && res.data) {
    rounds.value = res.data
  }
}

async function saveRound() {
  if (!roundForm.roundName.trim()) return
  roundLoading.value = true
  try {
    if (editingRound.value) {
      await updateApplicationRound(props.id, editingRound.value.id, roundForm as ApplicationRoundUpdateRequest)
    } else {
      await createApplicationRound(props.id, roundForm as ApplicationRoundCreateRequest)
    }
    showRoundDialog.value = false
    await loadRounds()
    await loadDetail()
  } finally {
    roundLoading.value = false
  }
}

function editRound(round: ApplicationRoundVO) {
  editingRound.value = round
  showRoundDialog.value = true
}

async function handleRoundResult(roundId: number, result: RoundResult) {
  if (result === 'fail') {
    ElMessageBox.confirm('标记淘汰后投递状态将自动变为"已拒绝"，确认？', '确认淘汰', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(async () => {
      await setApplicationRoundResult(props.id, roundId, { result })
      await loadRounds()
      await loadDetail()
    }).catch(() => {})
  } else {
    await setApplicationRoundResult(props.id, roundId, { result })
    await loadRounds()
  }
}

async function deleteRound(roundId: number) {
  await deleteApplicationRound(props.id, roundId)
  await loadRounds()
}
```

- [ ] **Step 4: 在 `onMounted` 或适当位置调用 `loadRounds()`**

---

### Task 9: 新增错误码常量

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/exception/ErrorCode.java`

- [ ] **Step 1: 增加轮次相关错误码常量**

```java
    APPLICATION_ROUND_NOT_FOUND_ERROR(40430, "面试轮次不存在"),
```

---

### Task 10: 编译验证

- [ ] **Step 1: 后端编译 + 测试**

```bash
cd backend
.\mvnw.cmd compile -DskipTests
.\mvnw.cmd test -pl . -Dtest=ApplicationServiceImplTest
```

- [ ] **Step 2: 前端类型检查**

```bash
cd frontend
npm run type-check
```
