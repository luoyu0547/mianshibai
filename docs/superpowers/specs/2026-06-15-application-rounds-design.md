# 投递管理 - 面试轮次功能设计

## 问题

1. 不同公司面试流程不同（轮次数量、名称各异），当前硬编码状态机无法适应
2. 推进到下一轮时无法设置该轮的时间
3. 无法记录每轮面试的结果（通过/淘汰）

## 解决方案

新增 `application_round` 子表，将面试流程从硬编码状态驱动改为轮次驱动。

---

## 后端设计

### 1. 新建表 `application_round`

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

### 2. 调整投递主状态机

删除 `first_interview` / `second_interview` / `final_interview` 三个状态，简化为：

```
pending_submit → submitted → interviewing → offer
                                        ↓
                                  rejected / withdrawn
```

- `interviewing` 阶段通过轮次表驱动（不再硬编码有几轮）
- 当某轮标记为 `fail` 时，用户可选择是否将投递自动切到 `rejected`

### 3. 新增 DTO

- `ApplicationRoundCreateRequest`：applicationId, roundName, scheduledAt, notes
- `ApplicationRoundUpdateRequest`：roundName, scheduledAt, notes（可空）
- `ApplicationRoundResultRequest`：result（pass / fail）

### 4. 新增 VO

`ApplicationRoundVO`：id, applicationId, roundName, roundOrder, scheduledAt, result, notes

`JobApplicationVO` 增加 `List<ApplicationRoundVO> rounds` 字段

### 5. 新增 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/application/{id}/round` | 添加轮次 |
| GET | `/api/application/{id}/round` | 获取轮次列表 |
| PUT | `/api/application/{id}/round/{roundId}` | 更新轮次 |
| PUT | `/api/application/{id}/round/{roundId}/result` | 标记轮次结果（pass/fail） |
| DELETE | `/api/application/{id}/round/{roundId}` | 删除轮次 |

### 6. 实体 & Mapper

- `ApplicationRound` 实体（MyBatis-Plus，逻辑删除）
- `ApplicationRoundMapper`（BaseMapper）

### 7. Service

`ApplicationService` 新增：

```java
List<ApplicationRoundVO> listRounds(String auth, Long applicationId);
ApplicationRoundVO createRound(String auth, Long applicationId, ApplicationRoundCreateRequest req);
ApplicationRoundVO updateRound(String auth, Long applicationId, Long roundId, ApplicationRoundUpdateRequest req);
ApplicationRoundVO setRoundResult(String auth, Long applicationId, Long roundId, ApplicationRoundResultRequest req);
void deleteRound(String auth, Long applicationId, Long roundId);
```

---

## 前端设计

### 1. 类型

新增 `applicationRound.ts` 或写在 `types/application.ts`：

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

export interface ApplicationRoundResultRequest {
  result: RoundResult
}
```

`JobApplicationVO` 增加 `rounds: ApplicationRoundVO[]`

### 2. API

```typescript
export function listApplicationRounds(applicationId: number) { ... }
export function createApplicationRound(applicationId: number, data: ApplicationRoundCreateRequest) { ... }
export function updateApplicationRound(applicationId: number, roundId: number, data: ApplicationRoundUpdateRequest) { ... }
export function setApplicationRoundResult(applicationId: number, roundId: number, data: ApplicationRoundResultRequest) { ... }
export function deleteApplicationRound(applicationId: number, roundId: number) { ... }
```

### 3. 详情页改动

在 `ApplicationDetailPage.vue` 的"投递进度"区块增加轮次时间线：

- 按 `roundOrder` 排序的卡片列表
- 每张卡片显示：轮次名、面试时间、结果标签
- 底部"添加轮次"按钮 → 弹窗输入：轮次名称、面试时间、备注
- 每轮可操作：编辑、删除、标记通过/淘汰
- 标记淘汰时提示"将投递状态改为已拒绝"，确认后自动更新主状态

### 4. 新建/编辑页面

来源已改为下拉框。主状态选项调整为简化后的流程。

---

## 执行计划

1. 建表 SQL（init.sql）
2. 后端：ApplicationRound 实体 + Mapper
3. 后端：DTO/VO
4. 后端：Service 接口 + 实现
5. 后端：Controller 端点
6. 后端：调整主状态验证逻辑
7. 前端：类型 + API
8. 前端：详情页轮次时间线组件
9. 测试：后端单测
