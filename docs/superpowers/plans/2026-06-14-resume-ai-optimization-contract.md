# Resume AI Optimization Contract Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 统一简历 AI 单模块优化、整份优化和评分的前后端字段契约，让 AI 只返回前端实际展示和保存的字段。

**Architecture:** 后端在 `ResumeAiServiceImpl` 中集中强化提示词，并在 `ResumeWholeOptimizeVO` 增加模块摘要 VO。前端在 `types/resume.ts` 对齐返回类型，在 `AiOptimizeDialog.vue` 使用字段白名单显示单模块优化结果，在 `WholeResumeOptimizeDialog.vue` 展示整份优化的模块摘要并继续只用 `optimizedSections` 应用内容。

**Tech Stack:** Java 17, Spring Boot 3.5, Spring AI `ChatClient`, Vue 3, TypeScript, Element Plus, Vite.

---

## File Map

- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
  - 更新单模块优化、整份优化、评分提示词。
  - 解析整份优化新增的 `sectionSummaries`。
  - 对 AI 返回的模块数据做字段白名单过滤，避免 `highlights`、`description` 等错误字段污染模块。
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeWholeOptimizeVO.java`
  - 增加 `sectionSummaries` 字段和内部类。
- Modify: `frontend/src/types/resume.ts`
  - 增加 `ResumeOptimizeSectionSummary` 类型。
  - 扩展 `ResumeWholeOptimizeVO.sectionSummaries`。
- Modify: `frontend/src/components/resume/AiOptimizeDialog.vue`
  - 使用模块字段白名单和模块专属字段标签。
  - 不再默认展示 `highlights`。
  - 教育经历显示 `activities` 为“在校经历”。
- Modify: `frontend/src/components/resume/WholeResumeOptimizeDialog.vue`
  - 展示 `sectionSummaries`。
  - 当 `optimizedSections` 为空时禁用应用按钮。

## Task 1: Backend Whole Optimize Contract

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeWholeOptimizeVO.java`
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`

- [ ] **Step 1: Extend `ResumeWholeOptimizeVO`**

Replace `ResumeWholeOptimizeVO.java` with this structure, preserving package and imports style:

```java
package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
public class ResumeWholeOptimizeVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Integer beforeScore;
    private Integer estimatedAfterScore;
    private List<String> globalSuggestions;
    private List<SectionSummary> sectionSummaries;
    private List<SectionVO> optimizedSections;

    @Data
    public static class SectionSummary implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String sectionType;
        private String sectionTitle;
        private String summary;
    }
}
```

- [ ] **Step 2: Replace whole optimize prompt**

In `ResumeAiServiceImpl.java`, replace `WHOLE_OPTIMIZE_PROMPT` with a prompt that asks for only this JSON object:

```java
private static final String WHOLE_OPTIMIZE_PROMPT =
        "你是一位专业的技术简历优化顾问。请根据当前简历模块进行整体优化。%s\n\n" +
        "当前简历模块 JSON：\n%s\n\n" +
        "只允许返回 JSON 对象，不要包含解释文字。JSON 字段只能包含：\n" +
        "- globalSuggestions：字符串数组，最多 5 条，全局优化建议\n" +
        "- sectionSummaries：模块摘要数组，每个元素包含 sectionType、sectionTitle、summary\n" +
        "- optimizedSections：优化后的模块数组，每个元素包含 sectionType 和 sectionData\n\n" +
        "必须遵守以下字段契约：\n" +
        "1. basic 只允许 name,email,phone,targetPosition,city,github,blog,avatar,currentStatus,expectedLocation,expectedSalary,wechat,website。保留头像和联系方式，不虚构个人信息。\n" +
        "2. education 只允许 school,major,degree,startDate,endDate,gpa,activities。只优化 activities 作为在校经历，不允许 description 或 highlights。\n" +
        "3. work 只允许 company,position,startDate,endDate,description。description 输出 2-4 条分点，每条包含动作、技术或方法、结果，不允许 highlights。\n" +
        "4. project 只允许 name,role,techStack,startDate,endDate,description。description 输出 3-5 条分点，覆盖职责、核心实现、技术难点、结果，不允许 highlights。\n" +
        "5. skills 只允许 categories，categories 每项只包含 name 和 items。\n" +
        "6. summary 只允许 content，内容应简短突出定位、技能、项目经验和岗位匹配。\n" +
        "7. optimizedSections 必须保持原模块类型和顺序，不新增虚构经历，不删除用户已有模块。\n" +
        "8. 分点文本使用换行符分隔，不要写成一大段。\n\n" +
        "返回示例：{\"globalSuggestions\":[\"补充量化结果\"],\"sectionSummaries\":[{\"sectionType\":\"project\",\"sectionTitle\":\"项目经历\",\"summary\":\"拆分项目描述并补充技术结果。\"}],\"optimizedSections\":[{\"sectionType\":\"project\",\"sectionData\":{\"name\":\"项目名\",\"role\":\"核心开发\",\"techStack\":[\"Java\"],\"startDate\":\"2026-01\",\"endDate\":\"2026-05\",\"description\":\"1. 负责核心模块设计，使用 Spring Boot 完成接口开发，支撑主要业务流程。\\n2. 优化查询链路，将响应时间从 800ms 降至 300ms。\"}}]}";
```

- [ ] **Step 3: Parse `sectionSummaries`**

In `optimizeWholeResume`, after reading `globalSuggestions` and `optimizedSectionMaps`, add:

```java
@SuppressWarnings("unchecked")
List<Map<String, Object>> sectionSummaryMaps = (List<Map<String, Object>>) result.get("sectionSummaries");
List<ResumeWholeOptimizeVO.SectionSummary> sectionSummaries = new ArrayList<>();
if (sectionSummaryMaps != null) {
    for (Map<String, Object> item : sectionSummaryMaps) {
        ResumeWholeOptimizeVO.SectionSummary summary = new ResumeWholeOptimizeVO.SectionSummary();
        summary.setSectionType(toStringValue(item.get("sectionType")));
        summary.setSectionTitle(toStringValue(item.get("sectionTitle")));
        summary.setSummary(toStringValue(item.get("summary")));
        sectionSummaries.add(summary);
    }
}
```

Then set it on the VO before return:

```java
vo.setSectionSummaries(sectionSummaries);
```

- [ ] **Step 4: Add helper for safe string conversion**

Add this private method near other helpers in `ResumeAiServiceImpl`:

```java
private String toStringValue(Object value) {
    return value == null ? null : value.toString();
}
```

- [ ] **Step 5: Run backend compile**

Run in `backend/`:

```powershell
.\mvnw.cmd -DskipTests compile
```

Expected: Maven compile succeeds.

## Task 2: Backend Prompt Field Contracts

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`

- [ ] **Step 1: Replace single-section optimize prompt**

Replace `OPTIMIZE_SYSTEM_PROMPT` with:

```java
private static final String OPTIMIZE_SYSTEM_PROMPT =
        "你是一位专业的技术简历优化助手。目标岗位是 %s，当前模块类型是 %s。\n" +
        "你必须只返回优化后的 JSON，输入是数组就返回数组，输入是对象就返回对象，不要包含解释文字。\n" +
        "按模块类型严格遵守字段契约：\n" +
        "- basic：只允许 name,email,phone,targetPosition,city,github,blog,avatar,currentStatus,expectedLocation,expectedSalary,wechat,website。保留头像和联系方式，不虚构个人信息。\n" +
        "- education：只允许 school,major,degree,startDate,endDate,gpa,activities。只优化 activities 作为在校经历，不允许 description 或 highlights。\n" +
        "- work：只允许 company,position,startDate,endDate,description。description 输出 2-4 条分点，每条包含动作、技术或方法、结果，不允许 highlights。\n" +
        "- project：只允许 name,role,techStack,startDate,endDate,description。description 输出 3-5 条分点，覆盖职责、核心实现、技术难点、结果，不允许 highlights。\n" +
        "- skills：只允许 categories，categories 每项只包含 name 和 items，不写自然段。\n" +
        "- summary：只允许 content，内容应简短突出定位、技能、项目经验和岗位匹配。\n" +
        "分点文本使用换行符分隔，不要写成一大段。保持原有非优化字段不变。";
```

- [ ] **Step 2: Replace score prompt**

Replace `SCORE_SYSTEM_PROMPT` with:

```java
private static final String SCORE_SYSTEM_PROMPT =
        "你是一位专业的技术简历评估助手。请对以下简历内容进行评分，目标岗位是 %s。\n" +
        "只返回 JSON 对象，不要包含解释文字。字段只能包含：\n" +
        "- score：总分，0-100\n" +
        "- dimensions：包含 completeness、completenessComment、professionalism、professionalismComment、matching、matchingComment\n" +
        "- suggestions：字符串数组，最多 5 条\n\n" +
        "评分时必须区分模块字段：\n" +
        "1. education.activities 看在校经历是否具体，包括课程、竞赛、奖项、校园项目或论文。\n" +
        "2. work.description 看是否分点，是否体现职责、技术动作和成果。\n" +
        "3. project.description 看是否包含核心实现、技术难点和量化结果。\n" +
        "4. skills.categories 看技能分类是否清晰，是否匹配目标岗位。\n" +
        "5. summary.content 看个人定位是否清楚。\n" +
        "suggestions 必须具体到模块和字段，例如：项目经历 description 建议拆成核心实现、技术难点、量化结果三条。";
```

- [ ] **Step 3: Run backend compile**

Run in `backend/`:

```powershell
.\mvnw.cmd -DskipTests compile
```

Expected: Maven compile succeeds.

## Task 3: Frontend Types and Whole Optimize Dialog

**Files:**
- Modify: `frontend/src/types/resume.ts`
- Modify: `frontend/src/components/resume/WholeResumeOptimizeDialog.vue`

- [ ] **Step 1: Add TypeScript type**

In `frontend/src/types/resume.ts`, add before `ResumeWholeOptimizeVO`:

```ts
export interface ResumeOptimizeSectionSummary {
  sectionType: SectionType
  sectionTitle: string
  summary: string
}
```

Then update `ResumeWholeOptimizeVO`:

```ts
export interface ResumeWholeOptimizeVO {
  beforeScore: number
  estimatedAfterScore: number
  globalSuggestions: string[]
  sectionSummaries?: ResumeOptimizeSectionSummary[]
  optimizedSections: SectionVO[]
}
```

- [ ] **Step 2: Update whole optimize template**

In `WholeResumeOptimizeDialog.vue`, after the global suggestions block, add:

```vue
<div v-if="result.sectionSummaries?.length" class="whole-optimize__sections">
  <h3>模块优化摘要</h3>
  <div class="whole-optimize__section-list">
    <div
      v-for="item in result.sectionSummaries"
      :key="item.sectionType"
      class="whole-optimize__section-card"
    >
      <span class="whole-optimize__section-title">{{ item.sectionTitle }}</span>
      <p>{{ item.summary }}</p>
    </div>
  </div>
</div>
```

- [ ] **Step 3: Disable apply when optimized sections are empty**

Change the footer primary button disabled condition to:

```vue
<NbButton variant="primary" :disabled="loading || !result || result.optimizedSections.length === 0" @click="handleApply">应用优化</NbButton>
```

- [ ] **Step 4: Add styles for section summaries**

Add to `WholeResumeOptimizeDialog.vue` scoped style:

```css
.whole-optimize__sections {
  padding: 18px;
  border-radius: var(--nb-radius-lg);
  background: var(--nb-surface);
  border: 1px solid var(--nb-border-color-light);
}

.whole-optimize__sections h3 {
  margin: 0 0 12px;
  font-family: var(--font-heading);
  font-size: 16px;
}

.whole-optimize__section-list {
  display: grid;
  gap: 10px;
}

.whole-optimize__section-card {
  padding: 12px 14px;
  border: var(--nb-border);
  border-radius: var(--nb-radius);
  background: #fff;
}

.whole-optimize__section-title {
  display: block;
  margin-bottom: 4px;
  font-family: var(--font-heading);
  font-weight: 700;
  color: var(--nb-primary);
}

.whole-optimize__section-card p {
  margin: 0;
  color: var(--nb-ink);
  line-height: 1.6;
  font-size: 13px;
}
```

- [ ] **Step 5: Run frontend type-check**

Run in `frontend/`:

```powershell
npm run type-check
```

Expected: TypeScript check succeeds.

## Task 4: Frontend Single Module Field Display

**Files:**
- Modify: `frontend/src/components/resume/AiOptimizeDialog.vue`

- [ ] **Step 1: Replace field labels**

Replace `FIELD_LABELS` with labels that match real editor fields:

```ts
const FIELD_LABELS: Record<string, string> = {
  name: '姓名',
  email: '邮箱',
  phone: '手机号',
  targetPosition: '目标岗位',
  city: '城市',
  avatar: '头像',
  currentStatus: '当前状态',
  expectedLocation: '期望工作地',
  expectedSalary: '期望薪资',
  wechat: '微信',
  website: '个人网站',
  github: 'GitHub',
  blog: '博客',
  school: '学校',
  major: '专业',
  degree: '学历',
  gpa: 'GPA',
  startDate: '开始时间',
  endDate: '结束时间',
  activities: '在校经历',
  company: '公司',
  position: '职位',
  description: '描述',
  role: '角色',
  techStack: '技术栈',
  content: '内容',
  categories: '技能分类',
  items: '技能项',
}
```

- [ ] **Step 2: Add field allowlist**

After `FIELD_LABELS`, add:

```ts
const SECTION_FIELDS: Record<SectionType, string[]> = {
  basic: ['name', 'email', 'phone', 'targetPosition', 'city', 'github', 'blog', 'avatar', 'currentStatus', 'expectedLocation', 'expectedSalary', 'wechat', 'website'],
  education: ['school', 'major', 'degree', 'startDate', 'endDate', 'gpa', 'activities'],
  work: ['company', 'position', 'startDate', 'endDate', 'description'],
  project: ['name', 'role', 'techStack', 'startDate', 'endDate', 'description'],
  skills: ['categories'],
  summary: ['content'],
}
```

- [ ] **Step 3: Filter flattened object fields**

Change `flattenData` to pass `props.sectionType` into `flattenObject`:

```ts
function flattenData(data: Record<string, unknown> | unknown[]): FieldRow[] {
  const rows: FieldRow[] = []
  if (Array.isArray(data)) {
    data.forEach((item, idx) => {
      if (item && typeof item === 'object') {
        rows.push({ key: `__hdr_${idx}`, label: `# ${idx + 1}`, value: '' })
        const objRows = flattenObject(item as Record<string, unknown>, props.sectionType)
        objRows.forEach((r) => rows.push({ ...r, key: `${idx}-${r.key}` }))
      }
    })
    return rows
  }
  return flattenObject(data, props.sectionType)
}
```

Replace `flattenObject` with:

```ts
function flattenObject(obj: Record<string, unknown>, sectionType: SectionType): FieldRow[] {
  const rows: FieldRow[] = []
  const allowedFields = SECTION_FIELDS[sectionType]
  for (const key of allowedFields) {
    const value = obj[key]
    const label = FIELD_LABELS[key] || key
    const display = formatValue(value)
    if (display) {
      rows.push({ key, label, value: display })
    }
  }
  return rows
}
```

- [ ] **Step 4: Run frontend type-check**

Run in `frontend/`:

```powershell
npm run type-check
```

Expected: TypeScript check succeeds.

## Task 5: End-to-End Verification

**Files:**
- Review modified files only.

- [ ] **Step 1: Run backend tests or compile**

Run in `backend/`:

```powershell
.\mvnw.cmd -DskipTests compile
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run frontend type-check**

Run in `frontend/`:

```powershell
npm run type-check
```

Expected: command exits successfully.

- [ ] **Step 3: Review git diff**

Run from repo root:

```powershell
git diff -- backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/main/java/com/mianshiba/ai/model/vo/resume/ResumeWholeOptimizeVO.java frontend/src/types/resume.ts frontend/src/components/resume/AiOptimizeDialog.vue frontend/src/components/resume/WholeResumeOptimizeDialog.vue docs/superpowers/specs/2026-06-14-resume-ai-optimization-contract-design.md docs/superpowers/plans/2026-06-14-resume-ai-optimization-contract.md
```

Expected: diff only contains the planned contract, prompt, type, and UI display changes.

## Self-Review

- Spec coverage: backend prompt contracts, whole optimize section summaries, single module field display, scoring prompt, and verification are covered.
- Placeholder scan: no TBD/TODO/fill-later instructions are present.
- Type consistency: `sectionSummaries`, `sectionType`, `sectionTitle`, `summary`, and `optimizedSections` names match between Java VO and TypeScript type.
- Scope check: no template rewrite, field-level diff, save-flow change, or new module type is included.
