# PatchCompareCard — AI 优化对比卡片设计

## 目标

将当前 AI 建议的"查看对比 → 弹窗 JSON 对比"流程改为聊天气泡内嵌的字段级对比卡片，提供"同意"/"反对"直接操作。

## 当前问题

- `ResumePatchConfirmDialog` 以原始 JSON 展示对比，用户难以直观看出具体差异
- 弹窗打断操作流程，需要关闭弹窗再继续对话
- 只有"应用到简历"一个操作，缺少拒绝/忽略的快捷入口

## 组件架构

```
ResumeEditPage (sectionDataMap)
  └── AiChatPanel.vue
        └── PatchCompareCard.vue  ← 新增
```

### 新增 prop

`AiChatPanel` 新增 prop:

```ts
sectionDataMap: Record<string, Record<string, unknown> | Array<Record<string, unknown>>>
```

由 `ResumeEditPage` 传入（已有 `sectionDataMap` computed 属性）。

### 新增组件

`PatchCompareCard.vue` — 放在 `src/components/resume/` 下。

**Props:**
- `proposal: ResumePatchProposal` — AI 建议数据
- `currentData: Record<string, unknown> | Record<string, unknown>[]` — 当前该模块数据
- `sectionType: SectionType` — 模块类型

**Emits:**
- `accept` — 用户同意，父组件应用提案
- `reject` — 用户拒绝，卡片关闭

**内部状态：**
- `expanded: boolean` — 是否展开对比视图

## 状态与交互

每个 proposal 有 4 种状态：

```
Collapsed ──点击[查看对比]──→ Expanded ──点击[同意]──→ Applied (短暂显示后移除)
   ↑                              │
   └───────点击[收起]─────────────┘
                                  └──点击[反对]──→ Rejected (立即移除)
```

1. **Collapsed（默认）** — 当前 mini 卡片样式，显示"AI 建议修改XX" + reason + [查看对比] [忽略] 按钮
2. **Expanded** — 展开为完整对比卡片，字段级左右对比，底部 [同意] [反对]
3. **Accepted** — 展示"已应用"状态 1.5 秒后从列表中移除
4. **Rejected** — 立即从列表中移除（调用已有 `ignoreProposal`）

## 对比卡片布局（Expanded 状态）

```
┌──────────────────────────────────────────────┐
│ 📝 基本信息 · AI 建议修改         [收起 ▲]   │
│ 理由：基本信息可以更规范                     │
├────────────────┬─────────────────────────────┤
│   当前内容      │     AI 建议                 │
│                │                             │
│  姓名           │  姓名                       │
│  张三           │  张三                       │
│                │                             │
│  电话           │  电话                       │
│  138001         │  13800138000  ← 修改       │
│                │                             │
│  意向岗位       │  意向岗位                   │
│  无             │  Java 后端工程师  ← 新增    │
│                │                             │
│  GitHub         │  GitHub                     │
│  github.com/a   │  github.com/a   ——          │
├────────────────┴─────────────────────────────┤
│        [✓ 同意]       [✗ 反对]               │
└──────────────────────────────────────────────┘
```

## 字段对比逻辑

以数据结构类型区分处理：

### 扁平对象（basic、summary、skills）

取新旧数据的 key 并集，逐字段比较：

| 情况 | 当前侧 | 建议侧 | 标记 |
|------|--------|--------|------|
| 值相同 | 正常值 | 正常值 | 无标记 |
| 值不同 | 旧值 | 新值 | 新值高亮 + "修改"标签 |
| 新增字段 | 灰显"无" | 新值 | "新增"标签 |
| 删除字段 | 旧值 | 灰显"已移除" | "删除"标签 |

### 对象数组（education、work、project）

- 按数组索引一对一对比
- 每个数组元素按扁平对象逻辑逐字段对比
- 数组长度不同时，多余项标"新增"或"已移除"

### 嵌套数组（techStack、highlights）

- 用逗号连接为字符串后按文本对比
- 在卡片内用标签样式展示差异

### Skills 特殊处理

- `categories` 数组逐个对比 category 的 name + items
- 新增/删除的 category 标"新增"/"删除"

## CSS 样式

遵循 Neubrutalism 设计（与项目一致的 `--nb-*` 变量）：

- 卡片：`var(--nb-border)` 2px 边框，`var(--nb-shadow-xs)` 阴影
- 对比区域：两列网格布局 `grid-template-columns: 1fr 1fr`
- 字段行：每行独立，hover 时浅色背景
- 新增标签：绿色背景 `#dcfce7`
- 修改标签：蓝色背景 `#dbeafe`
- 删除标签：红色背景 `#fee2e2`
- 按钮："同意"为 primary 变体，"反对"为 ghost 变体

## 影响范围

| 文件 | 变更 |
|------|------|
| `src/components/resume/PatchCompareCard.vue` | 新增 |
| `src/components/resume/AiChatPanel.vue` | 新增 prop `sectionDataMap`，提案卡片改为可展开，渲染对比卡片 |
| `src/views/resume/ResumeEditPage.vue` | 传 `sectionDataMap` 给 `AiChatPanel` |
| `src/types/resume.ts` | 可能新增类型 |
- 可以删除 `ResumePatchConfirmDialog.vue`（不再需要弹窗），或保留但不再使用
- 无需修改后端

## 不包含的范围

- 不涉及后端 API 修改
- 不涉及 PDF 导出
- 不涉及"批量同意"功能
- 不对比简历模板样式（只对比字段数据）
