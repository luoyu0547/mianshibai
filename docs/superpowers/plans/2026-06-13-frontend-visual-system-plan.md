# Frontend Visual System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a unified, elevated Neubrutalism job-search command center visual system across the frontend while fixing high-impact rendering and interaction issues.

**Architecture:** Start with design tokens and shared components, then centralize status/data rendering helpers, then migrate layouts and priority pages in controlled batches. Keep Element Plus and existing page structure, but replace ad hoc styling with reusable `Nb*` components, semantic classes, and consistent patterns.

**Tech Stack:** Vue 3, TypeScript, Vite, Pinia, Vue Router, Element Plus, ECharts, CSS variables, scoped CSS.

---

## Scope And Sequencing

This plan implements the approved spec in phases because the scope touches most frontend modules. Each task should leave the app buildable and manually smoke-testable.

Do not rewrite backend APIs. Only make minimal frontend behavior fixes where rendering, trust, or interaction quality is directly affected.

Do not commit automatically unless the user explicitly asks for commits. The original plan workflow recommends frequent commits, but this repository's active operating rule requires explicit user approval before committing.

## File Structure Map

### Core Design System

- Modify `frontend/src/assets/styles/variables.css`: design tokens, Element Plus overrides, global utility classes.
- Modify `frontend/src/components/NbButton.vue`: button variants, native type, focus/loading/disabled states.
- Modify `frontend/src/components/NbCard.vue`: visual variants, clickable behavior, compact/elevated states.
- Create `frontend/src/components/NbPageHeader.vue`: reusable page header.
- Create `frontend/src/components/NbStatCard.vue`: reusable metric card.
- Create `frontend/src/components/NbEmptyState.vue`: reusable empty/error action state.
- Create `frontend/src/components/NbLoadingBlock.vue`: reusable skeleton/loading block.
- Create `frontend/src/components/NbStatusBadge.vue`: reusable semantic badge.
- Create `frontend/src/components/NbSectionTitle.vue`: reusable section title.

### Shared Rendering Helpers

- Create `frontend/src/utils/statusMaps.ts`: labels, variants, and descriptions for statuses, roles, priorities, recommendations, mastery levels, and interview states.
- Create `frontend/src/utils/text.ts`: safe plain-text formatting helpers.
- Create `frontend/src/utils/date.ts`: compact display helpers for dates and date-times.

### Layouts

- Modify `frontend/src/layouts/MainLayout.vue`: responsive grouped navigation, improved active state, avatar fallback/image handling.
- Modify `frontend/src/layouts/AdminLayout.vue`: align tokens, sidebar states, responsive behavior.
- Modify `frontend/src/layouts/AuthLayout.vue`: refine login shell without changing auth behavior.

### Priority Pages

- Modify `frontend/src/views/home/HomePage.vue`: command center layout.
- Modify `frontend/src/views/resume/ResumeListPage.vue`, `ResumeEditPage.vue`, `ResumePreviewPage.vue`: resume workbench visual upgrade.
- Modify `frontend/src/components/resume/*.vue` and `frontend/src/components/resume/sections/*.vue`: AI panels, dialogs, editor sections.
- Modify `frontend/src/views/interview/*.vue`: interview setup, room, report, list visual system.
- Modify `frontend/src/views/training/*.vue`: training center, question, mistakes, mastery.
- Modify `frontend/src/views/job/*.vue`: job intelligence pages and safe text rendering.
- Modify `frontend/src/views/application/*.vue`: application pipeline and todos.
- Modify `frontend/src/views/coach/*.vue`: career coach command center and plan/diagnosis pages.
- Modify `frontend/src/views/admin/*.vue`: admin console consistency.
- Modify `frontend/src/views/analytics/AnalyticsOverviewPage.vue`, `frontend/src/components/charts/BaseChart.vue`, `frontend/src/utils/charts/reviewCharts.ts`: chart containers and empty states.

### High-Impact Fixes

- Modify `frontend/src/utils/request.ts` only if runtime response usage is confirmed broken during implementation.
- Modify affected stores only when a response shape mismatch breaks rendering.
- Replace plain-text `v-html` usages with safe text rendering in page files.

---

## Task 1: Stabilize Design Tokens And Global Element Plus Styling

**Files:**

- Modify: `frontend/src/assets/styles/variables.css`

- [ ] **Step 1: Read current token usage**

Read `frontend/src/assets/styles/variables.css`, `frontend/src/components/NbButton.vue`, `frontend/src/components/NbCard.vue`, `frontend/src/layouts/MainLayout.vue`, and `frontend/src/layouts/AdminLayout.vue`.

Confirm all existing custom properties referenced by components and layouts. Pay special attention to `--nb-border`, `--nb-shadow`, and `--nb-shadow-sm`.

- [ ] **Step 2: Fix border token self-reference and add semantic tokens**

Update `variables.css` so the border color and border shorthand are separate. Preserve existing color values unless the file already uses a different value.

Expected token shape:

```css
:root {
  --nb-bg: #FEF9EF;
  --nb-surface: #FFFFFF;
  --nb-ink: #2D3436;
  --nb-primary: #6C5CE7;
  --nb-secondary: #00CEC9;
  --nb-accent: #FD79A8;
  --nb-success: #00B894;
  --nb-warning: #FDCB6E;
  --nb-danger: #E84393;
  --nb-muted: #6B7280;
  --nb-muted-surface: #FFF3D8;
  --nb-border-width: 2px;
  --nb-border-color: var(--nb-ink);
  --nb-border: var(--nb-border-width) solid var(--nb-border-color);
  --nb-radius-sm: 4px;
  --nb-radius: 8px;
  --nb-radius-lg: 14px;
  --nb-shadow-xs: 2px 2px 0 var(--nb-border-color);
  --nb-shadow-sm: 3px 3px 0 var(--nb-border-color);
  --nb-shadow: 4px 4px 0 var(--nb-border-color);
  --nb-shadow-lg: 6px 6px 0 var(--nb-border-color);
  --nb-shadow-xl: 8px 8px 0 var(--nb-border-color);
  --nb-transition: 180ms ease;
  --nb-container: 1200px;
}
```

- [ ] **Step 3: Add global utility patterns**

Add reusable classes for common page surfaces and accessibility. Keep names project-specific.

Expected classes:

```css
.nb-page-shell {
  min-height: 100vh;
  background: var(--nb-bg);
}

.nb-container {
  width: min(100% - 32px, var(--nb-container));
  margin: 0 auto;
}

.nb-focus-ring:focus-visible {
  outline: 3px solid var(--nb-secondary);
  outline-offset: 3px;
}

.nb-prewrap {
  white-space: pre-wrap;
  word-break: break-word;
}

@media (prefers-reduced-motion: reduce) {
  *, *::before, *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    scroll-behavior: auto !important;
    transition-duration: 0.01ms !important;
  }
}
```

- [ ] **Step 4: Normalize Element Plus overrides**

Ensure Element Plus inputs, selects, date pickers, dialogs, drawers, tabs, tables, dropdowns, and messages use the same tokens. Avoid hover transform on inputs.

Expected behavior:

- Form controls keep stable layout on hover.
- Focus is visibly stronger than hover.
- Dialogs/drawers use `--nb-border` and `--nb-shadow-lg`.
- Tables have readable headers and row hover without excessive motion.

- [ ] **Step 5: Verify CSS build**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: TypeScript check passes or reports only pre-existing unrelated errors. If errors appear from this task, fix them before continuing.

---

## Task 2: Upgrade Core `Nb` Components

**Files:**

- Modify: `frontend/src/components/NbButton.vue`
- Modify: `frontend/src/components/NbCard.vue`

- [ ] **Step 1: Update `NbButton` API without breaking existing callers**

Keep the existing `type` prop for compatibility, but add `variant` and `nativeType`. Resolve the native button `type` conflict by binding `nativeType` to the underlying button.

Expected script shape:

```ts
const props = withDefaults(
  defineProps<{
    type?: 'primary' | 'secondary' | 'accent' | 'success'
    variant?: 'primary' | 'secondary' | 'accent' | 'success' | 'warning' | 'danger' | 'ghost'
    nativeType?: 'button' | 'submit' | 'reset'
    loading?: boolean
    disabled?: boolean
    block?: boolean
  }>(),
  {
    type: 'primary',
    variant: undefined,
    nativeType: 'button',
    loading: false,
    disabled: false,
    block: false,
  },
)

const resolvedVariant = computed(() => props.variant ?? props.type)
```

- [ ] **Step 2: Update `NbButton` template and classes**

Expected behavior:

- `:type="nativeType"` on `<button>`.
- Variant class uses `resolvedVariant`.
- Loading and disabled both prevent click.
- Focus-visible style is visible.
- `cursor: pointer` applies only when enabled.

- [ ] **Step 3: Update `NbCard` variants**

Add props for `variant`, `clickable`, and `compact`. Keep `hoverable` working.

Expected script shape:

```ts
withDefaults(
  defineProps<{
    hoverable?: boolean
    clickable?: boolean
    compact?: boolean
    variant?: 'default' | 'accent' | 'success' | 'warning' | 'danger' | 'ai' | 'muted'
  }>(),
  {
    hoverable: false,
    clickable: false,
    compact: false,
    variant: 'default',
  },
)
```

- [ ] **Step 4: Verify existing usage compiles**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: No new type errors from `NbButton` or `NbCard`.

---

## Task 3: Add Shared Visual Components

**Files:**

- Create: `frontend/src/components/NbPageHeader.vue`
- Create: `frontend/src/components/NbStatCard.vue`
- Create: `frontend/src/components/NbEmptyState.vue`
- Create: `frontend/src/components/NbLoadingBlock.vue`
- Create: `frontend/src/components/NbStatusBadge.vue`
- Create: `frontend/src/components/NbSectionTitle.vue`

- [ ] **Step 1: Create `NbPageHeader.vue`**

Implement props `eyebrow`, `title`, and `description`, with `actions` slot.

Required template shape:

```vue
<template>
  <header class="nb-page-header">
    <div>
      <p v-if="eyebrow" class="nb-page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="nb-page-header__title">{{ title }}</h1>
      <p v-if="description" class="nb-page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="nb-page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>
```

- [ ] **Step 2: Create `NbStatCard.vue`**

Implement props `label`, `value`, `hint`, `variant`, and optional `trend`. Include icon and action slots.

Use `NbCard` internally so visual changes stay centralized.

- [ ] **Step 3: Create `NbEmptyState.vue`**

Implement props `title`, `description`, and `variant`. Include `action` slot.

Required behavior:

- Use SVG-like decorative blocks via CSS, not emoji.
- Provide clear title and next action.

- [ ] **Step 4: Create `NbLoadingBlock.vue`**

Implement props `rows`, `height`, and `title`. Render skeleton rows with accessible loading text.

Required accessibility:

```vue
<div class="nb-loading-block" role="status" aria-live="polite">
  <span class="sr-only">加载中</span>
</div>
```

If `.sr-only` does not exist globally, add it to `variables.css`.

- [ ] **Step 5: Create `NbStatusBadge.vue`**

Implement props `label`, `variant`, and `title`.

Supported variants:

```ts
type BadgeVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'ai'
```

- [ ] **Step 6: Create `NbSectionTitle.vue`**

Implement props `title`, `description`, with `meta` and `actions` slots.

- [ ] **Step 7: Verify component type safety**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: New components compile without errors.

---

## Task 4: Centralize Status, Date, And Text Rendering

**Files:**

- Create: `frontend/src/utils/statusMaps.ts`
- Create: `frontend/src/utils/text.ts`
- Create: `frontend/src/utils/date.ts`

- [ ] **Step 1: Create status map types and helpers**

Create `statusMaps.ts` with a shared descriptor type.

Expected code shape:

```ts
export type StatusVariant = 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'muted' | 'ai'

export interface StatusDescriptor {
  label: string
  variant: StatusVariant
  description?: string
}

export function getStatusDescriptor(
  map: Record<string, StatusDescriptor>,
  value: string | number | null | undefined,
  fallbackLabel = '未知',
): StatusDescriptor {
  if (value === null || value === undefined || value === '') {
    return { label: fallbackLabel, variant: 'muted' }
  }
  return map[String(value)] ?? { label: String(value), variant: 'muted' }
}
```

- [ ] **Step 2: Add concrete maps**

Include maps for:

- `applicationStatusMap`
- `todoPriorityMap`
- `interviewStatusMap`
- `trainingMasteryMap`
- `jobRecommendationMap`
- `userRoleMap`
- `userStatusMap`
- `coachTaskStatusMap`
- `aiProcessStatusMap`

Use the labels currently present in pages, but centralize them.

- [ ] **Step 3: Create safe text helper**

Create `text.ts`.

Expected helpers:

```ts
export function displayText(value: string | null | undefined, fallback = '暂无') {
  const text = value?.trim()
  return text ? text : fallback
}

export function splitTags(value: string | null | undefined) {
  return (value ?? '')
    .split(/[,，、;；\n]/)
    .map((item) => item.trim())
    .filter(Boolean)
}
```

- [ ] **Step 4: Create date helper**

Create `date.ts`.

Expected helpers:

```ts
export function formatDateTime(value: string | null | undefined, fallback = '暂无') {
  if (!value) return fallback
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', { hour12: false })
}

export function formatDate(value: string | null | undefined, fallback = '暂无') {
  if (!value) return fallback
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleDateString('zh-CN')
}
```

- [ ] **Step 5: Verify utilities compile**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: Utility modules compile.

---

## Task 5: Upgrade Layouts And Navigation

**Files:**

- Modify: `frontend/src/layouts/MainLayout.vue`
- Modify: `frontend/src/layouts/AdminLayout.vue`
- Modify: `frontend/src/layouts/AuthLayout.vue`

- [ ] **Step 1: Refactor main navigation into a config array**

In `MainLayout.vue`, define nav items in script so labels, routes, and admin visibility are centralized.

Expected item shape:

```ts
interface NavItem {
  label: string
  to: string
  group: 'core' | 'growth' | 'account'
  adminOnly?: boolean
}
```

- [ ] **Step 2: Add responsive navigation behavior**

Implement a small-screen menu toggle using local `ref(false)`. Keep desktop top nav.

Expected behavior:

- Desktop: inline nav.
- Mobile: menu button opens vertical nav panel.
- Active route is clearly highlighted.

- [ ] **Step 3: Improve avatar rendering**

If `userStore.userInfo?.userAvatar` exists, render image. Otherwise use first character of `userName` or `userAccount`.

- [ ] **Step 4: Align admin layout**

Update `AdminLayout.vue` to use the same tokens, active state, responsive sidebar behavior, and stable content container.

- [ ] **Step 5: Refine auth shell**

Update `AuthLayout.vue` with the refined command-center visual language while preserving its slot behavior and existing login/register flow.

- [ ] **Step 6: Verify routing shell**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: Layout changes compile. Then manually inspect `/login`, `/`, and `/admin` if a dev server is available.

---

## Task 6: Convert Homepage Into Command Center

**Files:**

- Modify: `frontend/src/views/home/HomePage.vue`

- [ ] **Step 1: Identify existing data dependencies**

Keep existing `dashboardStore` and `trainingStore` data usage. Do not change API contracts.

Preserve these interactions:

- Pending question navigation.
- Review question navigation.
- Weak mastery navigation.
- Algorithm recommendation completion.

- [ ] **Step 2: Replace top section with `NbPageHeader` and action hero**

Use `NbPageHeader` for the page title and a hero card for today's main action. The hero should show user name, today priorities, and primary CTA.

- [ ] **Step 3: Replace ad hoc stat cards with `NbStatCard`**

Render application stats and mastery summary through `NbStatCard` or consistent card classes.

- [ ] **Step 4: Add clear loading and empty states**

Use `NbLoadingBlock` while dashboard loads. Use `NbEmptyState` when no actionable data exists.

- [ ] **Step 5: Verify home interactions**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: Home compiles. Manual smoke check: page renders with data, algorithm complete action still calls `trainingStore.completeAlgorithm(id)`.

---

## Task 7: Upgrade Resume Workbench

**Files:**

- Modify: `frontend/src/views/resume/ResumeListPage.vue`
- Modify: `frontend/src/views/resume/ResumeEditPage.vue`
- Modify: `frontend/src/views/resume/ResumePreviewPage.vue`
- Modify: `frontend/src/components/resume/AiChatPanel.vue`
- Modify: `frontend/src/components/resume/AiScorePanel.vue`
- Modify: `frontend/src/components/resume/AiOptimizeDialog.vue`
- Modify: `frontend/src/components/resume/VersionHistory.vue`
- Modify: `frontend/src/components/resume/TemplateSelector.vue`
- Modify: `frontend/src/components/resume/sections/*.vue`

- [ ] **Step 1: Upgrade resume list**

Use `NbPageHeader`, `NbCard`, `NbEmptyState`, and `NbLoadingBlock`. Keep create, AI generate, edit, preview, and delete behavior unchanged.

- [ ] **Step 2: Upgrade resume editor layout**

Make the editor feel like a workbench:

- Left/main area: section forms.
- Right area: live preview and AI tools.
- Sticky save action where appropriate.
- Stable responsive stacking on mobile.

- [ ] **Step 3: Improve section editor visual consistency**

Use shared section headings and consistent spacing. Keep current model updates intact.

- [ ] **Step 4: Improve AI panels**

Convert AI score, optimize dialog, chat panel, and version history into consistent AI workflow surfaces.

For `AiOptimizeDialog`, keep raw JSON available only as secondary detail if already useful, but make the main view user-readable.

- [ ] **Step 5: Protect PDF preview**

Do not alter resume template internals in a way that changes exported PDF dimensions unless specifically required. Page chrome around preview may change.

- [ ] **Step 6: Verify resume flows**

Run from `frontend/`:

```powershell
npm run type-check
```

Manual smoke checks:

- `/resume`
- `/resume/new`
- `/resume/:id/edit`
- `/resume/:id/preview`

---

## Task 8: Upgrade Interview Experience

**Files:**

- Modify: `frontend/src/views/interview/InterviewListPage.vue`
- Modify: `frontend/src/views/interview/InterviewNewPage.vue`
- Modify: `frontend/src/views/interview/InterviewRoomPage.vue`
- Modify: `frontend/src/views/interview/InterviewReportPage.vue`

- [ ] **Step 1: Apply centralized status maps**

Replace local status label/tag helpers with `statusMaps.ts` where practical.

- [ ] **Step 2: Upgrade interview list and new page**

Use page header, setup-card structure, and clear status/next action cards.

- [ ] **Step 3: Upgrade room state visuals**

Keep the existing room state machine. Rework presentation around:

- Current question.
- TTS/playback state.
- Recording state.
- Partial/final transcript.
- Timer.
- Submit/next action.
- Error recovery.

- [ ] **Step 4: Upgrade report page**

Make report page a battle report:

- Score summary.
- Dimension cards.
- Radar chart area.
- Turn review sections.
- Comparison panel.
- Action items.

- [ ] **Step 5: Verify interview flows**

Run from `frontend/`:

```powershell
npm run type-check
```

Manual smoke checks with available data:

- `/interview`
- `/interview/new`
- `/interview/:id/room`
- `/interview/:id/report`

---

## Task 9: Upgrade Training, Analytics, And Charts

**Files:**

- Modify: `frontend/src/views/training/TrainingCenterPage.vue`
- Modify: `frontend/src/views/training/TrainingPlanDetailPage.vue`
- Modify: `frontend/src/views/training/TrainingQuestionPage.vue`
- Modify: `frontend/src/views/training/TrainingMistakePage.vue`
- Modify: `frontend/src/views/training/TrainingMasteryPage.vue`
- Modify: `frontend/src/views/analytics/AnalyticsOverviewPage.vue`
- Modify: `frontend/src/components/charts/BaseChart.vue`
- Modify: `frontend/src/utils/charts/reviewCharts.ts`

- [ ] **Step 1: Apply status maps to training mastery and question states**

Use centralized descriptors for mastery levels and statuses.

- [ ] **Step 2: Upgrade training center and plan detail**

Create a clear training command center: active plan, progress, weak topics, daily question groups, algorithm recommendations.

- [ ] **Step 3: Upgrade question page workflow**

Make the flow explicit: read question, draft answer, submit, review, mark mastery, continue.

- [ ] **Step 4: Upgrade mistake and mastery pages**

Use consistent filters, badges, cards, and empty states.

- [ ] **Step 5: Upgrade chart containers**

`BaseChart.vue` should render a consistent chart shell and support empty/loading fallback through parent content if needed. Keep ECharts lifecycle intact.

- [ ] **Step 6: Verify training and analytics**

Run from `frontend/`:

```powershell
npm run type-check
```

Manual smoke checks:

- `/training`
- `/training/plan/:id`
- `/training/question/:id`
- `/training/mistakes`
- `/training/mastery`
- `/analytics`

---

## Task 10: Upgrade Job Intelligence And Application Pipeline

**Files:**

- Modify: `frontend/src/views/job/JobImportPage.vue`
- Modify: `frontend/src/views/job/JobListPage.vue`
- Modify: `frontend/src/views/job/JobFavoritePage.vue`
- Modify: `frontend/src/views/job/JobDetailPage.vue`
- Modify: `frontend/src/views/job/CompanyDetailPage.vue`
- Modify: `frontend/src/views/job/JobQuestionsPage.vue`
- Modify: `frontend/src/views/application/ApplicationListPage.vue`
- Modify: `frontend/src/views/application/ApplicationDetailPage.vue`
- Modify: `frontend/src/views/application/ApplicationEditPage.vue`
- Modify: `frontend/src/views/application/ApplicationTodoPage.vue`

- [ ] **Step 1: Wrap isolated pages with correct layout if missing**

Ensure job list, job questions, and analytics-like pages use the same layout shell if they currently render standalone.

- [ ] **Step 2: Replace plain-text `v-html` usages**

For job descriptions, job requirements, and training/question plain text, use text rendering with `.nb-prewrap` unless sanitized HTML is truly required.

Expected replacement pattern:

```vue
<p class="nb-prewrap">{{ displayText(job.jobDescription) }}</p>
```

- [ ] **Step 3: Upgrade job decision pages**

Make job detail and company detail emphasize fit, growth, risk, keywords, evidence, and next action.

- [ ] **Step 4: Upgrade application pipeline pages**

Use pipeline-style visual hierarchy: status, next event, due todos, priority, and risk.

- [ ] **Step 5: Verify job and application flows**

Run from `frontend/`:

```powershell
npm run type-check
```

Manual smoke checks:

- `/job/import`
- `/job/favorites`
- `/job/:id`
- `/company/:id`
- `/applications`
- `/applications/new`
- `/applications/:id`
- `/applications/todos`

---

## Task 11: Upgrade Career Coach And Admin Console

**Files:**

- Modify: `frontend/src/views/coach/CoachHomePage.vue`
- Modify: `frontend/src/views/coach/CoachDiagnosisDetailPage.vue`
- Modify: `frontend/src/views/coach/CoachPlanDetailPage.vue`
- Modify: `frontend/src/views/admin/AdminDashboardPage.vue`
- Modify: `frontend/src/views/admin/AdminUserListPage.vue`
- Modify: `frontend/src/views/admin/AdminUserDetailPage.vue`

- [ ] **Step 1: Upgrade coach home**

Use command-center layout for latest diagnosis, active plan, today tasks, history, and generation actions.

- [ ] **Step 2: Upgrade diagnosis detail**

Render score, summary, data completeness, strengths, weaknesses, and suggestions as a professional diagnosis report.

- [ ] **Step 3: Upgrade coach plan detail**

Render tasks by day with priority/status badges and linked actions.

- [ ] **Step 4: Upgrade admin dashboard**

Use restrained metric cards and consistent admin shell spacing.

- [ ] **Step 5: Upgrade admin user list/detail**

Use consistent table, status badge, role badge, detail sections, and action feedback. Only show success messages after store actions indicate success.

- [ ] **Step 6: Verify coach and admin flows**

Run from `frontend/`:

```powershell
npm run type-check
```

Manual smoke checks:

- `/coach`
- `/coach/diagnoses/:id`
- `/coach/plans/:id`
- `/admin`
- `/admin/users`
- `/admin/users/:id`

---

## Task 12: Fix Confirmed High-Impact Runtime Mismatches

**Files:**

- Potentially modify: `frontend/src/utils/request.ts`
- Potentially modify: `frontend/src/stores/*.ts`
- Potentially modify: `frontend/src/api/*.ts`

- [ ] **Step 1: Confirm actual request return shape**

Read `request.ts` and one representative store from each module. Determine whether API calls return `BaseResponse<T>` directly or an axios response with `.data`.

- [ ] **Step 2: If mismatch is confirmed, choose one convention**

Preferred convention for this project:

```ts
const res = await someApi()
if (res.code === 0) {
  // use res.data
}
```

Do not mix `res.data.code` and `res.code` after standardization.

- [ ] **Step 3: Apply minimal edits only where broken**

Do not refactor every API file unless required by type-check or runtime usage. Prioritize stores/pages that block the visual upgrade from rendering.

- [ ] **Step 4: Verify compile and key stores**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: No request-shape type errors remain in touched files.

---

## Task 13: Full Verification And Polish Pass

**Files:**

- Review all touched frontend files.

- [ ] **Step 1: Run type-check**

Run from `frontend/`:

```powershell
npm run type-check
```

Expected: Pass.

- [ ] **Step 2: Run build**

Run from `frontend/`:

```powershell
npm run build
```

Expected: Type-check and Vite build pass.

- [ ] **Step 3: Run lint after functional checks**

Run from `frontend/`:

```powershell
npm run lint
```

Expected: ESLint completes. Because this command uses `--fix`, inspect resulting diffs afterward.

- [ ] **Step 4: Inspect changed files**

Run from repo root:

```powershell
git status --short
git diff -- frontend docs/superpowers/plans/2026-06-13-frontend-visual-system-plan.md docs/superpowers/specs/2026-06-13-frontend-visual-system-design.md
```

Expected: Diffs contain only intended visual-system, helper, layout, page, and doc changes.

- [ ] **Step 5: Manual responsive smoke checklist**

Check desktop and mobile widths for:

- `/login`
- `/`
- `/resume`
- `/resume/:id/edit`
- `/interview`
- `/interview/:id/room`
- `/interview/:id/report`
- `/training`
- `/training/question/:id`
- `/job/import`
- `/job/:id`
- `/applications`
- `/coach`
- `/admin`

Expected:

- No horizontal scroll at 375px.
- Navigation remains usable.
- Cards and forms stack cleanly.
- Loading/empty/error states are visible.
- Focus states are visible.
- No plain-text content depends on unsafe `v-html`.

---

## Self-Review

Spec coverage:

- Global visual language: Tasks 1-3.
- Navigation and layout: Task 5.
- AI interaction system: Tasks 7, 8, 11.
- Data rendering rules: Tasks 4, 10, 12.
- Chart system: Task 9.
- Priority page designs: Tasks 6-11.
- Accessibility and verification: Tasks 1, 3, 13.
- High-impact fixes: Tasks 1, 10, 12.

Completeness scan:

- No implementation step depends on an unspecified future decision.
- Page migration tasks specify exact target files and required preserved behavior.

Type consistency:

- Shared badge variants use the same `StatusVariant` naming across helper and component tasks.
- `NbButton` keeps `type` compatibility and adds `variant` plus `nativeType` to avoid breaking existing callers.
- Date/text helpers are intentionally small and typed for nullable backend VO fields.

## Execution Options

Use one of these approaches after user approval:

1. Subagent-Driven: dispatch a focused subagent for each task or page group, then review and verify between tasks.
2. Inline Execution: execute tasks in this session with checkpoints after each phase.
