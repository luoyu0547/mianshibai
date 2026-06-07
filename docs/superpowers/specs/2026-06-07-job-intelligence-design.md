# 职位情报与岗位推荐功能设计文档

> 日期：2026-06-07  
> 状态：设计完成，待实现

## 1. 目标

为“面试吧”平台新增职位情报与岗位推荐能力，将招聘平台职位链接、公司官网招聘页和企业公开信息转化为可用于求职决策的结构化数据。该模块不追求帮助用户盲目海投，而是帮助技术求职者判断目标公司是否值得投、目标岗位是否匹配、投递前简历应如何调整、面试前应重点准备哪些内容。

职位模块是现有 AI 简历和 AI 语音模拟面试的上游输入，最终形成闭环：

```text
抓取岗位 -> AI 解析 -> 判断是否值得投 -> 优化简历 -> 模拟面试 -> 根据报告继续优化
```

## 2. 首版范围

### 2.1 包含能力

- 支持用户粘贴招聘平台单职位链接，抓取并解析职位详情。
- 支持用户粘贴公司官网或官网招聘页，抓取并解析公司信息与官网岗位。
- 支持识别公司画像，包括行业、主营业务、技术方向、官网介绍、企业荣誉。
- 重点识别专精特新、小巨人、高成长技术型企业等信号。
- 支持将网页内容解析为结构化职位数据，包括职位名、公司、薪资、城市、经验、学历、职责、要求、技术栈。
- 支持结合用户简历输出岗位匹配度、投递建议、简历优化建议和面试准备重点。
- 支持从职位详情进入“针对该岗位优化简历”和“针对该岗位开始模拟面试”。
- 支持职位收藏，形成用户目标岗位清单。

### 2.2 不包含能力

- 不做自动投递。
- 不做批量海投工具。
- 不做大规模全网职位爬取。
- 不承诺绕过招聘平台反爬限制。
- 不在首版建设复杂后台采集调度系统。
- 不将无证据的 AI 判断直接标记为确定企业资质。

## 3. 方案选择

采用分阶段组合方案：

1. 第一阶段完成链接解析、官网解析、单岗位分析、单公司画像和简历匹配。
2. 第二阶段沉淀职位库、企业画像库、用户收藏和推荐排序。
3. 第三阶段扩展定时刷新、批量官网抓取、官方名单导入和更完整的个性化推荐。

首个实现计划只覆盖第一阶段，但数据表设计预留职位库和企业画像能力。这样可以避免一开始陷入大规模爬虫复杂度，同时不会把系统做成一次性分析工具。

## 4. 总体架构

```text
frontend/ Vue 3
├── 职位解析页
├── 职位详情页
├── 公司画像页
├── 收藏职位列表页
├── job api
├── job store
└── 与 resume/interview 页面联动

backend/ Spring Boot
├── JobController
├── JobCrawlService
├── JobParseService
├── CompanyProfileService
├── JobRecommendService
├── ResumeJobMatchService
├── AiJobAnalysisService
├── JobMapper / CompanyMapper / JobMatchMapper
└── DTO / VO / Entity

external data
├── 招聘平台职位页面
├── 公司官网与官网招聘页
├── 官方专精特新/小巨人名单
└── 公开新闻、百科、企业官网荣誉页
```

后端继续沿用现有 `controller -> service -> mapper` 分层。网页抓取、内容解析、AI 理解、推荐评分必须拆开，避免让大模型直接控制不可控爬虫。

## 5. 后端模块职责

### 5.1 JobController

职位模块 API 入口，负责链接解析、职位详情、公司画像、职位收藏、职位匹配等接口编排。

### 5.2 JobCrawlService

只负责抓取网页内容，不负责理解内容。首版可以使用 HTTP 抓取工具或外部爬取服务，后续可替换为 Jsoup、Playwright、Firecrawl 或独立爬虫服务。

输入是 URL，输出是页面标题、正文、HTML、来源平台、最终跳转 URL 和抓取状态。

### 5.3 JobParseService

负责将抓取内容解析为结构化职位数据。对于规则明显的平台，可优先使用规则解析；对于官网招聘页和结构不稳定页面，调用 AI 做结构化提取。

### 5.4 CompanyProfileService

负责公司画像，包括公司官网、行业、主营业务、技术方向、企业荣誉、专精特新和小巨人识别证据。

### 5.5 AiJobAnalysisService

负责调用 Spring AI `ChatClient`，让 AI 做岗位理解、JD 结构化提取、企业信息总结、匹配解释和面试准备建议。AI 输出必须尽量结构化，并保留证据字段。

### 5.6 JobRecommendService

负责综合评分和推荐结论，不直接抓网页。推荐逻辑要可解释，不能只返回一个分数。

### 5.7 ResumeJobMatchService

负责读取用户简历，与职位画像做匹配，输出能力匹配、缺口分析、简历优化建议和面试准备重点。

## 6. 数据库设计

### 6.1 company 表

```sql
CREATE TABLE IF NOT EXISTS company (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '公司 id',
  name VARCHAR(128) NOT NULL COMMENT '公司名称',
  normalized_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '归一化公司名，用于名单匹配',
  website VARCHAR(512) NOT NULL DEFAULT '' COMMENT '公司官网',
  industry VARCHAR(128) NOT NULL DEFAULT '' COMMENT '行业方向',
  city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '主要城市',
  scale VARCHAR(64) NOT NULL DEFAULT '' COMMENT '公司规模',
  description TEXT DEFAULT NULL COMMENT '公司简介',
  main_business TEXT DEFAULT NULL COMMENT '主营业务',
  tech_direction VARCHAR(256) NOT NULL DEFAULT '' COMMENT '技术方向',
  is_specialized_new TINYINT NOT NULL DEFAULT 0 COMMENT '是否专精特新：0-否/未确认，1-是',
  is_little_giant TINYINT NOT NULL DEFAULT 0 COMMENT '是否小巨人：0-否/未确认，1-是',
  certification_confidence VARCHAR(32) NOT NULL DEFAULT 'unknown' COMMENT '资质可信度：confirmed/suspected/unknown',
  source_url VARCHAR(512) NOT NULL DEFAULT '' COMMENT '公司信息来源 URL',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_name (name),
  KEY idx_normalized_name (normalized_name),
  KEY idx_industry (industry),
  KEY idx_city (city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司画像表';
```

### 6.2 company_certification 表

```sql
CREATE TABLE IF NOT EXISTS company_certification (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '资质证据 id',
  company_id BIGINT NOT NULL COMMENT '公司 id',
  certification_type VARCHAR(64) NOT NULL COMMENT '资质类型：specialized_new/little_giant/high_tech/other',
  status VARCHAR(32) NOT NULL DEFAULT 'suspected' COMMENT '状态：confirmed/suspected/rejected',
  evidence_source VARCHAR(64) NOT NULL COMMENT '证据来源：official_list/website/news/ai_inferred',
  evidence_url VARCHAR(512) NOT NULL DEFAULT '' COMMENT '证据 URL',
  evidence_text TEXT DEFAULT NULL COMMENT '证据文本',
  confidence_score INT NOT NULL DEFAULT 0 COMMENT '可信度 0-100',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_company_id (company_id),
  KEY idx_certification_type (certification_type),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公司资质证据表';
```

### 6.3 job 表

```sql
CREATE TABLE IF NOT EXISTS job (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '职位 id',
  company_id BIGINT DEFAULT NULL COMMENT '公司 id',
  title VARCHAR(128) NOT NULL COMMENT '职位名称',
  company_name VARCHAR(128) NOT NULL DEFAULT '' COMMENT '公司名称快照',
  source_platform VARCHAR(64) NOT NULL DEFAULT '' COMMENT '来源平台，如 boss/lagou/company_website',
  source_url VARCHAR(1024) NOT NULL COMMENT '职位来源 URL',
  city VARCHAR(64) NOT NULL DEFAULT '' COMMENT '工作城市',
  salary_range VARCHAR(64) NOT NULL DEFAULT '' COMMENT '薪资范围',
  experience_requirement VARCHAR(64) NOT NULL DEFAULT '' COMMENT '经验要求',
  education_requirement VARCHAR(64) NOT NULL DEFAULT '' COMMENT '学历要求',
  job_description TEXT DEFAULT NULL COMMENT '岗位职责',
  job_requirement TEXT DEFAULT NULL COMMENT '岗位要求',
  tech_stack JSON DEFAULT NULL COMMENT '技术栈列表',
  raw_content MEDIUMTEXT DEFAULT NULL COMMENT '抓取原始内容',
  status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态：active/expired/unknown',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_delete TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0-未删，1-已删',
  PRIMARY KEY (id),
  KEY idx_company_id (company_id),
  KEY idx_title (title),
  KEY idx_city (city),
  KEY idx_source_platform (source_platform)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位表';
```

### 6.4 job_analysis 表

```sql
CREATE TABLE IF NOT EXISTS job_analysis (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '岗位分析 id',
  job_id BIGINT NOT NULL COMMENT '职位 id',
  requirement_summary TEXT NOT NULL COMMENT '岗位要求总结',
  core_skills JSON NOT NULL COMMENT '核心技能列表',
  hidden_requirements JSON NOT NULL COMMENT '隐含能力要求',
  interview_focus JSON NOT NULL COMMENT '面试准备重点',
  resume_suggestions JSON NOT NULL COMMENT '简历优化建议',
  risk_points JSON NOT NULL COMMENT '风险点或不匹配点',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位 AI 分析表';
```

### 6.5 job_match 表

```sql
CREATE TABLE IF NOT EXISTS job_match (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '匹配记录 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  resume_id BIGINT DEFAULT NULL COMMENT '简历 id',
  job_id BIGINT NOT NULL COMMENT '职位 id',
  match_score INT NOT NULL DEFAULT 0 COMMENT '岗位匹配度 0-100',
  growth_score INT NOT NULL DEFAULT 0 COMMENT '企业成长性 0-100',
  tech_growth_score INT NOT NULL DEFAULT 0 COMMENT '技术成长价值 0-100',
  salary_city_score INT NOT NULL DEFAULT 0 COMMENT '薪资城市匹配 0-100',
  experience_fit_score INT NOT NULL DEFAULT 0 COMMENT '经验门槛适配 0-100',
  total_score INT NOT NULL DEFAULT 0 COMMENT '综合推荐分 0-100',
  recommendation VARCHAR(32) NOT NULL DEFAULT 'cautious' COMMENT '推荐结论：recommended/cautious/stretch/not_recommended',
  reason TEXT NOT NULL COMMENT '推荐原因',
  gaps JSON NOT NULL COMMENT '能力缺口',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_resume_id (resume_id),
  KEY idx_job_id (job_id),
  KEY idx_total_score (total_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户职位匹配表';
```

### 6.6 job_favorite 表

```sql
CREATE TABLE IF NOT EXISTS job_favorite (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '收藏 id',
  user_id BIGINT NOT NULL COMMENT '用户 id',
  job_id BIGINT NOT NULL COMMENT '职位 id',
  note VARCHAR(512) NOT NULL DEFAULT '' COMMENT '用户备注',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_job (user_id, job_id),
  KEY idx_user_id (user_id),
  KEY idx_job_id (job_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='职位收藏表';
```

## 7. 企业资质识别

专精特新、小巨人识别采用“官方名单强匹配 + 官网/新闻 AI 辅助判断”。

### 7.1 证据等级

- `confirmed`：官方名单匹配，或官网荣誉资质页明确写明。
- `suspected`：新闻、百科、招聘页或官网介绍提到相关资质，但未命中官方名单。
- `unknown`：未发现明确证据。

### 7.2 证据来源

- `official_list`：工信部或省市公开名单。
- `website`：公司官网、官网新闻、官网荣誉资质页。
- `news`：公开新闻报道。
- `ai_inferred`：AI 根据多处文本推断，只能作为辅助证据。

### 7.3 标记规则

- 只有 `official_list` 或明确官网证据可以标记为“已确认专精特新”或“已确认小巨人”。
- 只有新闻或 AI 推断时，只能标记为“疑似”。
- 没有证据时标记为“未发现明确资质”。
- 所有资质判断必须保存 `evidence_url` 和 `evidence_text`。

## 8. 推荐评分

推荐排序采用机会成本优先，而不是单纯匹配度优先。

```text
综合推荐分 =
岗位匹配度 * 0.4
+ 企业成长性 * 0.25
+ 技术成长价值 * 0.15
+ 薪资城市匹配 * 0.1
+ 经验门槛适配 * 0.1
```

### 8.1 评分维度

- 岗位匹配度：简历中的技术栈、项目经历、工作年限与 JD 的直接匹配程度。
- 企业成长性：专精特新、小巨人、高新技术企业、融资、行业景气度、业务稳定性等信号。
- 技术成长价值：岗位是否涉及高价值技术、核心业务、复杂工程场景。
- 薪资城市匹配：薪资范围、城市与用户期望是否匹配。
- 经验门槛适配：岗位要求是否明显高于或低于用户当前阶段。

### 8.2 推荐结论

- `recommended`：推荐投递，匹配度和机会成本都较好。
- `cautious`：谨慎投递，有明显短板或不确定信息。
- `stretch`：冲刺岗位，当前不完全匹配，但成长价值高。
- `not_recommended`：暂不建议，岗位不匹配或机会成本过高。

推荐解释必须包含原因、短板和行动建议，不能只返回分数。

## 9. AI 输出要求

AI 分析岗位时应输出结构化 JSON，包括：

```json
{
  "jobProfile": {
    "title": "Java 后端工程师",
    "companyName": "示例科技有限公司",
    "city": "杭州",
    "salaryRange": "15-25K",
    "techStack": ["Java", "Spring Boot", "Redis", "MySQL"]
  },
  "companySignals": {
    "industry": "工业软件",
    "mainBusiness": "工业设备数据采集与分析平台",
    "certifications": [
      {
        "type": "specialized_new",
        "status": "suspected",
        "evidenceText": "官网新闻提到公司入选专精特新企业名单",
        "evidenceUrl": "https://example.com/news"
      }
    ]
  },
  "matchAnalysis": {
    "recommendation": "recommended",
    "reason": "岗位要求与简历中的 Java、Redis、MySQL 项目经历匹配度较高。",
    "gaps": ["缺少工业设备数据处理相关表达"],
    "resumeSuggestions": ["在项目经历中补充消息队列、设备数据、时序数据相关描述"],
    "interviewFocus": ["Spring Boot 项目架构", "Redis 缓存设计", "设备数据高并发写入"]
  }
}
```

## 10. 前端页面设计

### 10.1 职位解析页 `/job/import`

用户粘贴招聘平台职位链接、公司官网或官网招聘页，选择解析类型后提交。解析完成后跳转职位详情或公司画像。

### 10.2 职位详情页 `/job/:id`

展示职位基础信息、技术栈、岗位要求、AI 分析、匹配度、投递建议、简历优化建议、面试准备重点。

主要操作：

- 收藏职位
- 针对该岗位优化简历
- 针对该岗位开始模拟面试
- 重新分析职位

### 10.3 公司画像页 `/company/:id`

展示公司行业、主营业务、技术方向、官网信息、专精特新/小巨人证据、官网岗位列表。

### 10.4 收藏职位页 `/job/favorites`

展示用户收藏职位，支持按推荐结论、城市、技术栈、企业资质筛选。

## 11. API 设计

接口前缀为 `/api/job`，所有接口沿用 `BaseResponse<T>`。

### 11.1 解析链接

```text
POST /api/job/import-url
Authorization: Bearer <token>
```

请求：

```json
{
  "url": "https://example.com/jobs/123",
  "importType": "job"
}
```

`importType` 可选值：`job`、`company_website`、`company_career_page`。

### 11.2 获取职位详情

```text
GET /api/job/{jobId}
Authorization: Bearer <token>
```

### 11.3 分析职位与简历匹配

```text
POST /api/job/{jobId}/match
Authorization: Bearer <token>
```

请求：

```json
{
  "resumeId": 1
}
```

### 11.4 收藏职位

```text
POST /api/job/{jobId}/favorite
Authorization: Bearer <token>
```

### 11.5 获取公司画像

```text
GET /api/job/company/{companyId}
Authorization: Bearer <token>
```

## 12. 与现有模块联动

### 12.1 简历模块

- 简历评分增加岗位匹配维度。
- 简历优化可以接收 `jobId`，按岗位 JD 优化项目经历、技能栈和自我评价。
- 简历预览页可展示当前简历适配的目标岗位。

### 12.2 面试模块

- 创建面试时可传入 `jobId`。
- AI 生成问题时同时读取简历和职位分析。
- 面试报告增加 JD 对齐度、岗位关键技能覆盖度、面试风险点。

## 13. 错误处理

- URL 无法访问：返回抓取失败，并提示用户复制 JD 文本作为替代输入。
- 页面内容不足：返回解析失败，提示用户换用职位详情页或官网招聘页。
- AI 解析失败：保存原始抓取内容，允许用户重试。
- 企业资质无法确认：标记为 `unknown`，不输出确定性结论。
- 重复职位链接：复用已有职位记录并刷新分析。

## 14. 测试策略

### 14.1 后端测试

- `JobParseService`：验证职位结构化解析。
- `CompanyProfileService`：验证资质证据等级和状态判断。
- `JobRecommendService`：验证综合评分和推荐结论。
- `ResumeJobMatchService`：验证简历与岗位匹配输出。
- Controller 测试：验证鉴权、参数校验、响应结构。

### 14.2 前端测试

- 职位解析页：验证 URL 输入、提交状态、错误提示。
- 职位详情页：验证匹配结果、推荐标签、操作按钮。
- 收藏职位页：验证收藏、取消收藏、筛选展示。

## 15. 风险与约束

- 招聘平台可能存在反爬限制，首版应支持失败提示和手动复制 JD 替代路径。
- 公司名称匹配可能存在简称、别名、集团主体不一致问题，需要归一化公司名和证据链。
- AI 可能误判企业资质，必须使用证据等级约束输出。
- 官网招聘页结构差异大，规则解析难以完全覆盖，需要保留 AI 结构化提取能力。
- 推荐分数只是辅助决策，页面文案应避免承诺“必过”“高薪保证”等不可靠结论。

## 16. 实施顺序

1. 建设职位、公司、资质证据、职位分析、职位匹配、收藏数据表。
2. 实现职位链接抓取和基础解析。
3. 实现 AI 岗位结构化分析。
4. 实现公司画像和企业资质证据识别。
5. 实现简历与岗位匹配评分。
6. 实现职位解析页、职位详情页和收藏职位页。
7. 打通“针对岗位优化简历”和“针对岗位开始模拟面试”。
