# AI 简历优化与生成功能设计文档

> 日期：2026-06-03
> 状态：设计完成，待实现

## 1. 概述

为"面试吧"平台新增 AI 驱动的个人简历优化与生成功能。用户可以：

- **从零生成**：基于用户资料和结构化表单，AI 一键生成完整简历
- **对话式填充**：通过 AI 对话口述经历，AI 提取结构化数据填入模块
- **模块级优化**：对单个模块（如工作经历描述）调用 AI 润色
- **简历评分**：AI 从完整性、专业性、匹配度三个维度评分
- **多模板预览**：支持多种简历模板，在线预览并导出 PDF
- **版本管理**：简历保存到数据库，支持历史版本查看和回滚

目标用户：**技术岗位求职者**（开发者、工程师），简历模块侧重技术栈、项目经历等。

## 2. 方案选型

采用 **方案 A（模板驱动 + AI 增强）+ 方案 B（AI 对话式填充）** 混合架构：

- **主体**：结构化表单填写 → 模板渲染预览 → 按需 AI 优化
- **辅助**：AI 对话面板，用户可口述经历，AI 提取后填入对应模块

选择理由：
1. 结构化数据天然支持多模板切换和版本管理
2. AI 作为增强工具按需触发，成本低、响应快
3. 对话模式降低填写门槛，适合不擅长写简历的用户
4. 与已有用户资料（targetPosition、techDirection、workYears）自然衔接

## 3. 数据模型

### 3.1 resume 表（简历主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| user_id | BIGINT NOT NULL | 所属用户 |
| title | VARCHAR(128) | 简历标题，如"Java 后端工程师简历" |
| template_type | VARCHAR(32) DEFAULT 'minimal_tech' | 模板类型 |
| status | VARCHAR(16) DEFAULT 'draft' | 状态：draft / published |
| source | VARCHAR(16) DEFAULT 'scratch' | 来源：scratch（手动）/ ai_chat（对话生成） |
| version | INT DEFAULT 1 | 当前版本号 |
| create_time | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| is_delete | TINYINT DEFAULT 0 | 逻辑删除 |

索引：`idx_user_id`、`idx_status`

### 3.2 resume_section 表（简历模块表）

一条记录 = 一个模块实例（如一条工作经历、一条项目经历）。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| resume_id | BIGINT NOT NULL | 所属简历 |
| section_type | VARCHAR(32) NOT NULL | 模块类型：basic / education / work / project / skills / summary |
| section_data | JSON NOT NULL | 模块结构化数据 |
| sort_order | INT DEFAULT 0 | 同类型模块排序 |
| ai_generated | TINYINT DEFAULT 0 | 是否由 AI 生成 |
| create_time | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 更新时间 |
| is_delete | TINYINT DEFAULT 0 | 逻辑删除 |

索引：`idx_resume_id`、`idx_section_type`

### 3.3 section_data JSON 结构

各模块的 `section_data` JSON 格式：

**basic（基本信息）**——每个简历只有一条：
```json
{
  "name": "张三",
  "email": "zhangsan@example.com",
  "phone": "13800138000",
  "targetPosition": "Java 后端工程师",
  "city": "北京",
  "github": "https://github.com/zhangsan",
  "blog": "https://zhangsan.dev"
}
```

**education（教育经历）**：
```json
{
  "school": "北京大学",
  "major": "计算机科学与技术",
  "degree": "本科",
  "startDate": "2018-09",
  "endDate": "2022-06",
  "gpa": "3.8/4.0",
  "highlights": ["专业排名前 10%", "获得国家奖学金"]
}
```

**work（工作经历）**：
```json
{
  "company": "字节跳动",
  "position": "Java 后端工程师",
  "startDate": "2022-07",
  "endDate": "2025-06",
  "description": "负责抖音电商后端核心系统的设计和开发",
  "highlights": ["独立设计订单系统，支撑日均 500 万订单", "优化缓存策略，QPS 提升 300%"]
}
```

**project（项目经历）**：
```json
{
  "name": "抖音电商订单系统",
  "role": "核心开发者",
  "techStack": ["Java", "Spring Boot", "Redis", "Kafka", "MySQL"],
  "startDate": "2023-03",
  "endDate": "2024-01",
  "description": "从零设计和实现高并发订单处理系统",
  "highlights": ["采用 CQRS 架构分离读写", "引入本地消息表保证最终一致性"]
}
```

**skills（技能标签）**：
```json
{
  "categories": [
    {
      "name": "编程语言",
      "items": ["Java (精通)", "Python (熟练)", "Go (了解)"]
    },
    {
      "name": "框架与中间件",
      "items": ["Spring Boot", "MyBatis-Plus", "Redis", "Kafka"]
    },
    {
      "name": "工具",
      "items": ["Git", "Docker", "Kubernetes", "IntelliJ IDEA"]
    }
  ]
}
```

**summary（自我评价）**：
```json
{
  "content": "5 年 Java 后端开发经验，熟悉高并发系统设计..."
}
```

### 3.4 resume_chat_message 表（AI 对话记录）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| resume_id | BIGINT NOT NULL | 所属简历 |
| role | VARCHAR(16) NOT NULL | 角色：user / assistant |
| content | TEXT NOT NULL | 消息内容 |
| related_section_type | VARCHAR(32) | 关联的模块类型（可选） |
| create_time | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |

索引：`idx_resume_id`

### 3.5 resume_version 表（版本快照）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT AUTO_INCREMENT | 主键 |
| resume_id | BIGINT NOT NULL | 所属简历 |
| version | INT NOT NULL | 版本号 |
| snapshot | JSON NOT NULL | 该版本的完整简历数据快照，结构为 `{ "sections": [{ "sectionType": "work", "sectionData": {...}, "sortOrder": 1 }, ...] }` |
| change_summary | VARCHAR(256) | 变更摘要（如"AI 优化了工作经历"） |
| create_time | DATETIME DEFAULT CURRENT_TIMESTAMP | 创建时间 |

索引：`idx_resume_id_version`（resume_id, version 联合唯一索引）

### 3.6 版本管理策略

- 每次用户手动"保存"时，`version++`，将当前所有 section_data 序列化为 snapshot 存入 resume_version
- AI 优化后自动保存一次，change_summary 记录"AI 优化了 xxx 模块"
- 版本列表按时间倒序展示，支持查看任意版本快照
- 暂不支持一键回滚（用户可参考历史版本手动修改，避免实现复杂度）

## 4. AI 功能设计

### 4.1 AI 功能矩阵

| 功能 | 触发方式 | 输入 | 输出 | DeepSeek 调用 |
|------|---------|------|------|--------------|
| 一键生成 | 编辑页底部按钮 | 用户 profile + 目标岗位 | 所有模块的完整数据 | 单次调用 |
| 模块优化 | 每个模块右上角按钮 | 当前模块内容 + 目标岗位 | 优化后的模块 JSON | 单次调用 |
| 对话填充 | 右侧 AI 面板输入框 | 用户口述 + 当前简历上下文 | AI 回复文本 + 结构化数据提取 | SSE 流式 |
| 简历评分 | 编辑页底部按钮 | 完整简历数据 | 评分（0-100）+ 分维度分析 + 改进建议 | 单次调用 |

### 4.2 提示词策略

使用 Spring AI 的 `ChatClient` 调用 `deepseek-chat` 模型。每个功能对应不同的 System Prompt，均要求 AI 以 JSON 格式返回结构化数据。

**一键生成** System Prompt 要点：
- 角色：资深技术简历顾问
- 输入：用户 profile（targetPosition、techDirection、workYears、city）
- 要求：生成匹配目标岗位的完整简历框架，包含真实感的示例内容（用户可修改）
- 输出格式：按模块分组的 JSON

**模块优化** System Prompt 要点：
- 角色：简历润色专家
- 输入：当前模块 JSON + 目标岗位
- 要求：按 STAR 法则优化描述，突出量化成果，保持技术准确性
- 输出格式：与输入相同结构的 JSON

**对话填充** System Prompt 要点：
- 角色：简历助手，引导用户描述经历
- 输入：对话历史 + 当前简历状态
- 要求：从用户自然语言中提取结构化信息，告知用户将填入哪个模块
- 当检测到用户描述包含工作/项目经历时，自动提取并建议填入

**简历评分** System Prompt 要点：
- 角色：HR + 技术面试官双重视角
- 评分维度：完整性（30%）、专业性（40%）、岗位匹配度（30%）
- 输出格式：`{ "score": 85, "dimensions": { "completeness": {...}, "professionalism": {...}, "matching": {...} }, "suggestions": [...] }`

### 4.3 对话模式数据提取

对话模式下，AI 回复中包含结构化数据时，以特殊标记返回：

```
已了解你在字节跳动的工作经历，我帮你整理如下：

[EXTRACTED_DATA]
{
  "sectionType": "work",
  "sectionData": {
    "company": "字节跳动",
    "position": "Java 后端工程师",
    ...
  }
}
[/EXTRACTED_DATA]

你可以确认后我会自动填入工作经历模块。
```

前端解析 `[EXTRACTED_DATA]` 标记，提取 JSON 后在编辑区高亮提示"是否采纳此内容"。

## 5. 后端 API 设计

所有接口需要 `Authorization: Bearer <token>` 认证。

### 5.1 简历 CRUD

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/resume` | 创建空简历 | `{ title, templateType }` | `ResumeVO` |
| GET | `/api/resume/list` | 获取用户简历列表 | - | `List<ResumeVO>` |
| GET | `/api/resume/{id}` | 获取简历详情（含所有模块） | - | `ResumeDetailVO` |
| PUT | `/api/resume/{id}` | 更新简历标题/模板/状态 | `{ title?, templateType?, status? }` | `ResumeVO` |
| DELETE | `/api/resume/{id}` | 删除简历（逻辑删除） | - | `BaseResponse<null>` |

### 5.2 模块 CRUD

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/resume/{resumeId}/section` | 添加模块 | `{ sectionType, sectionData, sortOrder? }` | `SectionVO` |
| PUT | `/api/resume/{resumeId}/section/{sectionId}` | 更新模块内容 | `{ sectionData, sortOrder? }` | `SectionVO` |
| DELETE | `/api/resume/{resumeId}/section/{sectionId}` | 删除模块 | - | `BaseResponse<null>` |
| PUT | `/api/resume/{resumeId}/section/sort` | 调整模块排序 | `{ orders: [{ sectionId, sortOrder }] }` | `BaseResponse<null>` |

### 5.3 AI 接口

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/resume/ai/generate` | 一键生成完整简历 | `{ targetPosition, techDirection?, workYears? }` | `ResumeDetailVO`（含所有模块） |
| POST | `/api/resume/{resumeId}/ai/optimize-section` | 优化单个模块 | `{ sectionId, sectionType, sectionData }` | `{ sectionData }`（优化后的 JSON） |
| POST | `/api/resume/{resumeId}/ai/score` | 简历评分 | - | `{ score, dimensions, suggestions }` |

### 5.4 对话接口（SSE 流式）

| 方法 | 路径 | 说明 | 请求体 | 响应 |
|------|------|------|--------|------|
| POST | `/api/resume/{resumeId}/chat` | 发送对话消息 | `{ message }` | SSE: `text/event-stream` |

SSE 事件格式：
```
data: {"type":"text","content":"已了解你的工作经历..."}

data: {"type":"extracted","sectionType":"work","sectionData":{...}}

data: {"type":"done"}
```

### 5.5 版本管理

| 方法 | 路径 | 说明 | 响应 |
|------|------|------|------|
| GET | `/api/resume/{id}/versions` | 获取版本列表 | `List<VersionVO>` |
| GET | `/api/resume/{id}/version/{version}` | 获取指定版本快照 | `VersionSnapshotVO` |

### 5.6 导出

PDF 导出完全在前端完成（html2canvas + jsPDF），将模板渲染的 DOM 直接截图转 PDF。后端不提供独立的导出接口，前端通过 `GET /api/resume/{id}` 获取简历数据后在客户端渲染并导出。

## 6. 前端设计

### 6.1 新增路由

| 路径 | 组件 | 说明 | 认证 |
|------|------|------|------|
| `/resume` | ResumeListPage | 简历列表页 | 需要 |
| `/resume/new` | ResumeEditPage | 新建并编辑简历 | 需要 |
| `/resume/:id/edit` | ResumeEditPage | 编辑已有简历 | 需要 |
| `/resume/:id/preview` | ResumePreviewPage | 全屏预览 + 导出 PDF | 需要 |

### 6.2 页面详细设计

**ResumeListPage（简历列表）**：
- 顶部"新建简历"按钮，点击后选择创建方式（空白简历 / AI 一键生成）
- 简历卡片网格展示，每个卡片显示标题、模板缩略图、更新时间、状态标签
- 卡片操作：编辑、预览、复制、删除
- 复用 NbCard 组件保持 Neubrutalism 风格

**ResumeEditPage（简历编辑器，核心页面）**：
- 顶栏：可编辑标题、模板选择下拉框、保存按钮、预览按钮
- 左右分栏布局（比例约 5:5）：
  - 左侧：结构化模块编辑区
    - 各模块折叠面板（Element Plus Collapse），每个面板右上角有"AI 优化"按钮
    - 多实例模块（教育/工作/项目）有"+ 添加"和拖拽排序功能
    - 底部"一键 AI 生成/优化全部"按钮
  - 右侧：
    - 上方：A4 比例缩放的实时预览（根据选中的模板渲染）
    - 下方：可折叠的 AI 对话面板（SSE 流式显示回复）
- 底栏：AI 评分按钮、版本历史抽屉

**ResumePreviewPage（全屏预览）**：
- A4 比例居中展示简历
- 顶部工具栏：切换模板、导出 PDF、返回编辑
- 导出 PDF 使用 html2canvas + jsPDF

### 6.3 模板系统

初期 3 种模板，每个模板是一个 Vue 组件，接收统一的 `ResumeData` props：

| 模板 ID | 名称 | 风格 |
|---------|------|------|
| minimal_tech | 简约技术风 | 单列、黑白为主、突出技术栈和项目，适合互联网公司 |
| modern_two_col | 现代双栏 | 左侧技能/联系方式侧边栏 + 右侧经历/项目主区域 |
| classic_formal | 经典正式 | 传统格式，适合投递国企/传统企业 |

模板组件接口：
```typescript
interface ResumeData {
  basic: BasicSection | null
  education: EducationSection[]
  work: WorkSection[]
  project: ProjectSection[]
  skills: SkillsSection | null
  summary: SummarySection | null
}

// 模板组件
defineProps<{ data: ResumeData }>()
```

### 6.4 新增文件结构

```
src/
├── api/resume.ts                          # 简历 API 调用
├── types/resume.ts                        # 简历类型定义
├── stores/resume.ts                       # 简历 Pinia Store
├── views/resume/
│   ├── ResumeListPage.vue                 # 简历列表
│   ├── ResumeEditPage.vue                 # 编辑器（核心）
│   └── ResumePreviewPage.vue              # 全屏预览
├── components/resume/
│   ├── sections/                          # 各模块编辑组件
│   │   ├── BasicInfoEditor.vue
│   │   ├── EducationEditor.vue
│   │   ├── WorkExperienceEditor.vue
│   │   ├── ProjectEditor.vue
│   │   ├── SkillsEditor.vue
│   │   └── SummaryEditor.vue
│   ├── AiChatPanel.vue                    # AI 对话面板
│   ├── AiScorePanel.vue                   # AI 评分展示
│   ├── VersionHistory.vue                 # 版本历史抽屉
│   └── TemplateSelector.vue              # 模板选择器
├── templates/                             # 简历渲染模板
│   ├── MinimalTech.vue
│   ├── ModernTwoCol.vue
│   └── ClassicFormal.vue
```

### 6.5 新增 npm 依赖

| 包名 | 用途 |
|------|------|
| html2canvas | 将 DOM 渲染为 canvas 用于 PDF 导出 |
| jspdf | 生成 PDF 文件 |
| vuedraggable | 模块拖拽排序（基于 SortableJS） |
| @vueup/vue-quill | 富文本编辑器（用于自我评价等自由文本区域，可选） |

## 7. 后端文件结构

```
backend/src/main/java/com/mianshiba/ai/
├── model/
│   ├── entity/
│   │   ├── Resume.java
│   │   ├── ResumeSection.java
│   │   ├── ResumeChatMessage.java
│   │   └── ResumeVersion.java
│   ├── dto/resume/
│   │   ├── ResumeCreateRequest.java
│   │   ├── ResumeUpdateRequest.java
│   │   ├── SectionCreateRequest.java
│   │   ├── SectionUpdateRequest.java
│   │   ├── SectionSortRequest.java
│   │   ├── AiGenerateRequest.java
│   │   ├── AiOptimizeRequest.java
│   │   └── ChatRequest.java
│   └── vo/resume/
│       ├── ResumeVO.java
│       ├── ResumeDetailVO.java
│       ├── SectionVO.java
│       ├── AiScoreVO.java
│       ├── VersionVO.java
│       └── VersionSnapshotVO.java
├── mapper/
│   ├── ResumeMapper.java
│   ├── ResumeSectionMapper.java
│   ├── ResumeChatMessageMapper.java
│   └── ResumeVersionMapper.java
├── service/
│   ├── ResumeService.java
│   ├── ResumeAiService.java
│   └── impl/
│       ├── ResumeServiceImpl.java
│       └── ResumeAiServiceImpl.java
└── controller/
    ├── ResumeController.java
    └── ResumeAiController.java
```

## 8. 错误处理

在现有 `ErrorCode` 枚举中新增：

| 错误码 | 名称 | 说明 |
|--------|------|------|
| 40400 | NOT_FOUND_ERROR | 资源不存在 |
| 40001 | RESUME_LIMIT_ERROR | 简历数量超出限制（每用户最多 10 份） |
| 40002 | RESUME_SECTION_ERROR | 模块操作异常（如 basic 模块重复添加） |
| 50001 | AI_SERVICE_ERROR | AI 服务调用失败 |
| 50002 | AI_RESPONSE_PARSE_ERROR | AI 响应解析失败 |

## 9. 测试策略

### 后端测试

- `ResumeServiceTest`：简历 CRUD 逻辑、权限校验（只能操作自己的简历）
- `ResumeAiServiceTest`：Mock ChatClient，验证提示词构建和响应解析
- `ResumeControllerTest`：MockMvc 测试所有接口的请求/响应格式
- 所有测试不依赖真实 MySQL/Redis/DeepSeek

### 前端测试

- 各模块编辑组件的表单交互测试
- Pinia Store 的状态管理测试
- AI 对话面板的 SSE 解析测试

## 10. 实现优先级

建议按以下顺序迭代：

1. **P0 — 数据层 + 基础 CRUD**：建表、Entity、Mapper、DTO/VO、Service、Controller
2. **P1 — 编辑器前端**：结构化表单编辑组件、实时预览、模板系统
3. **P2 — AI 功能**：一键生成、模块优化、简历评分
4. **P3 — 对话模式**：SSE 接口、AI 对话面板、数据提取
5. **P4 — 版本管理 + PDF 导出**：版本快照、历史查看、html2canvas + jsPDF

## 11. 范围外（本次不实现）

- 简历分享（公开链接）
- 简历投递（对接招聘平台）
- 头像上传（使用 URL 输入代替）
- ATS 友好格式导出
- 离线编辑（LocalStorage 缓存）
- 多语言简历
