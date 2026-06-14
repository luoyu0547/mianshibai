# Frontend Visual System Design

Date: 2026-06-13

## Context

`mianshiba` is now more than an interview simulator. The frontend contains resume editing, AI resume assistance, voice interviews, interview reports, job intelligence, application tracking, training plans, mistake review, mastery analytics, AI career coaching, and an admin console. The backend already exposes structured VO data for these flows, while the frontend has functional pages but inconsistent visual hierarchy, interaction patterns, layout treatment, loading states, and page-level polish.

Existing documents and styles define a Neubrutalism direction: warm ivory background, saturated purple/cyan/pink accents, black borders, hard shadows, crisp button press feedback, and card-based structure. This redesign keeps that identity but raises it into a more refined, unified, and high-quality product interface.

The chosen direction is **Neubrutalism Job Search Command Center 2.0**. It uses the existing brutalist visual DNA with stricter typography, spacing, semantic color, calmer motion, and clearer information hierarchy. `ui-ux-pro-max` recommendations were used as guidance, especially for learning-product friendliness, loading/empty feedback, accessibility, and chart usage. Its Claymorphism result is not adopted directly because it would make the product feel too toy-like for a professional job-search platform.

## Goals

- Build a unified, elegant, and visually elevated frontend experience.
- Preserve the project's Neubrutalism identity while making it feel mature and professional.
- Make the main user experience feel like a job-search command center: clear priorities, progress, risk, next action, and feedback.
- Align UI rendering with backend VO data, especially scores, statuses, progress, AI results, todos, reports, and training states.
- Standardize high-frequency UI patterns: page headers, cards, stats, forms, loading, empty states, error states, tags, dialogs, drawers, charts, and AI workflow panels.
- Improve frontend issues that directly affect visual trust or interaction quality.

## Non-Goals

- Do not replace Element Plus with another UI library.
- Do not introduce dark mode.
- Do not add internationalization.
- Do not create a full resume layout designer.
- Do not redesign backend contracts unless a frontend bug requires a small compatibility fix.
- Do not make the product look like a generic flat SaaS dashboard or a playful children's education app.

## Design Principles

### Noble Brutalism

The visual direction should feel bold but not cheap. Use the existing black border and hard shadow language with better rhythm, spacing, and restrained accent usage. Avoid random color blocks, emoji-style decoration, and overly cute rounded shapes.

### Action First

Every major page should answer: what is happening, what matters most, and what should the user do next? Priority cards, status labels, and CTA placement should be consistent across modules.

### Structured Density

This product has dense data. The design should support complex forms, lists, reports, charts, and AI output without visual clutter. Important content gets visual weight; secondary details become quieter.

### AI As Workflow

AI features are not decorative chat widgets. AI generation, optimization, review, diagnosis, and report enhancement should share one interaction model: input, processing, result summary, details, apply/retry/cancel.

### Trustworthy Feedback

Loading, empty, error, success, disabled, and dangerous states must be visible and consistent. No blank screens, frozen buttons, unclear success messages, or hidden failures.

## Global Visual Language

### Colors

Keep the existing base palette and formalize it into semantic roles.

- Background: warm ivory, used for the app shell.
- Surface: white, used for cards and form panels.
- Ink: dark border/text color.
- Primary: purple, used for main CTAs and current navigation.
- Secondary: cyan, used for supporting progress and secondary highlights.
- Accent: pink, used sparingly for AI or special emphasis.
- Success: green, used for completed states.
- Warning: yellow/orange, used for risk, urgency, due tasks, and incomplete steps.
- Danger: red, used for destructive and failed states.
- Muted: low-contrast surface and text tokens for metadata.

The palette should not become a rainbow. Each color must have a purpose. Page-level accents should be derived from semantic meaning rather than module-by-module randomness.

### Typography

Keep the current heading/body direction, but tighten hierarchy.

- Display headings: strong, geometric, used for page title and hero areas.
- Body text: readable, neutral, and suitable for dense forms and reports.
- Numeric text: emphasized for scores, counts, progress, timers, and rankings.
- Avoid childish typography suggested by generic education-app guidance.

### Borders And Shadows

Use border and shadow levels rather than one global effect everywhere.

- Standard card: 2px border, medium hard shadow.
- Important card: stronger border/shadow and accent strip.
- Compact card: lighter shadow for dense lists.
- Dialog/drawer: strong border, large shadow, clear title area.
- Form inputs: stable border and focus ring; avoid hover movement in dense forms.

Fix the current CSS variable self-reference by separating border color from border shorthand.

### Motion

Use short transitions, typically 150-250ms.

- Buttons keep physical press feedback.
- Cards can lift or shift only when clearly clickable.
- Dense form fields should not shift on hover.
- Continuous decorative animations are avoided.
- Respect `prefers-reduced-motion`.

## Information Architecture

### Command Center Pages

Pages: home, training center, coach home, analytics overview, application list.

Structure:

- Page hero with current objective and primary next action.
- Key metrics row.
- Priority/action panel.
- Main progress or work queue.
- Secondary insight lists.

These pages should show what the user should do today, what is stuck, and what progress has changed.

### Workspace Pages

Pages: resume editor, interview room, training question, application detail.

Structure:

- Stable task header with current state.
- Main work area.
- Side assistant, preview, progress, or context panel.
- Clear sticky or repeated save/submit/next action.

These pages should reduce distractions and make one task easy to finish.

### Report And Profile Pages

Pages: resume preview, interview report, coach diagnosis, job detail, company detail, admin user detail.

Structure:

- Summary conclusion.
- Score/status/label cluster.
- Evidence or detail sections.
- Recommended next actions.
- Related links.

These pages should support reading, judgment, and confident decisions.

## Navigation And Layout

### Main Layout

The main navigation currently has many modules. It should be visually grouped and responsive.

- Keep top navigation on desktop.
- Show stronger active route state.
- Add responsive handling for narrow screens, such as collapsed menu or grouped overflow.
- Preserve admin-only visibility for admin entry.
- Use avatar image if `userAvatar` exists; otherwise fall back to initials.

### Admin Layout

Admin pages keep a separate shell but should share the same design tokens. The admin console should feel orderly and restrained, not visually louder than user-facing pages.

## Component System

### Existing Components

Extend the existing `NbButton` and `NbCard` instead of replacing them.

`NbButton` should support visual variants and native button type without naming conflict. It should clearly display loading, disabled, hover, focus, and active states.

`NbCard` should support compact, elevated, accent, clickable, warning, and AI-like variants if needed. Hover behavior should only apply to interactive cards.

### New Shared Patterns

Add small reusable components or shared class patterns where they reduce duplication.

- `NbPageHeader`: title, eyebrow, description, primary action, secondary action.
- `NbStatCard`: metric label, value, trend, icon slot, variant.
- `NbEmptyState`: title, description, action, optional illustration slot.
- `NbLoadingBlock`: skeleton/loading state for cards and lists.
- `NbSectionTitle`: section header with metadata or action slot.
- `NbStatusBadge`: consistent status, priority, role, recommendation, and mastery rendering.
- `NbActionPanel`: grouped next-step actions for command center pages.

These should remain lightweight and local to the project.

### Element Plus Adaptation

Keep Element Plus for forms, tables, dialogs, drawers, tabs, selects, date pickers, pagination, and messages. Style overrides should be systematic instead of ad hoc.

- Inputs: stable layout, clear focus, clear error, no hover jump.
- Select/date picker: match border and shadow tokens.
- Dialog/drawer: branded header, strong boundary, clear footer actions.
- Table: readable density, strong header, clear row hover, no excessive borders.
- Tabs: strong active indicator and consistent spacing.
- Message/notification: match semantic colors and border language.

## AI Interaction System

AI appears in resume generation, resume optimization, AI chat, resume scoring, interview report enhancement, training review, job analysis, and career coaching. These should share one visual grammar.

### AI Workflow Card

Each AI flow should show:

- Input or source context.
- Processing state with clear message.
- Result summary.
- Detailed result sections.
- Apply, retry, cancel, or navigate actions.

### Resume AI

- `AiChatPanel` should feel like a structured assistant, not a disconnected chat box.
- `AiScorePanel` should show score hierarchy, dimensions, and suggestions clearly.
- `AiOptimizeDialog` should avoid raw JSON as the main user experience. JSON can remain as secondary developer-like detail if needed.
- `VersionHistory` should show version timeline and change summaries clearly.

### Report And Coach AI

- Interview enhancement should look like an enriched battle report.
- Coach diagnosis should emphasize overall score, strengths, weaknesses, and tasks.
- Generated plans should make task priority, day grouping, and completion obvious.

## Data Rendering Rules

### Backend VO Alignment

Status, score, and structured VO fields should be rendered through consistent mappings.

- User roles: `admin`, normal user.
- Interview statuses: `created`, `in_progress`, `generating_report`, `completed`, `cancelled`.
- Application statuses: `pending_submit`, `submitted`, `interviewing`, `offer`, `closed`.
- Todo priorities: `low`, `medium`, `high`.
- Training mastery: `weak`, `basic`, `good`, `mastered`.
- Job recommendation: `recommended`, `cautious`, `stretch`, `not_recommended`.
- AI/report status: pending/running/completed/failed-like states.

Mappings should define label, color, badge variant, and optional description. Avoid repeated hard-coded status logic in multiple pages.

### Text Rendering

External or AI-generated plain text should render as text with `white-space: pre-wrap` where line breaks matter. Avoid `v-html` for plain text job descriptions or training content unless sanitized.

### Date And Time

Dates should be displayed consistently in lists, cards, reports, and todos. Date-only fields from Element Plus should be checked against backend `LocalDateTime` expectations before any behavior changes.

## Chart System

ECharts remains the chart library through `BaseChart.vue`.

- Radar chart: use for 4-8 ability dimensions, with accompanying text summary.
- Line chart: use for score trend over time.
- Bar chart: use for top skill gaps and counts.
- Progress bars: use for task progress, mastery distribution, and completion ratios.
- Chart cards should share border, shadow, legend, tooltip, and empty-state styling.
- Do not rely on color alone; labels and text summaries remain visible.

## Priority Page Designs

### Home Command Center

The homepage becomes the primary job-search command center.

It should combine:

- Today priorities from `DashboardVO`.
- Application stats and stages.
- Active training plan.
- Pending questions.
- Weak topics and weak masteries.
- Algorithm recommendations.
- Review questions.
- Career coach entry or today tasks when available.

The layout should make the next action unmistakable.

### Resume Module

Pages: resume list, edit, preview.

Key improvements:

- Professional card grid for resume list.
- Editor as a focused workspace: form area, preview area, AI side panel.
- Template selector with clearer previews.
- AI scoring and optimization shown as structured recommendation panels.
- Version history as a readable timeline.

Visual polish should not break PDF template rendering.

### Interview Module

Pages: list, new, room, report.

Key improvements:

- Session cards show status, target role, progress, duration, and next action.
- New interview page reads like a setup checklist.
- Room page emphasizes current question, recording/recognition state, transcript, timer, and next action.
- Report page becomes a battle report with score summary, dimension cards, radar, turn reviews, comparison, and action items.

### Training Module

Pages: center, plan detail, question, mistake book, mastery.

Key improvements:

- Training center shows current plan, progress, weak areas, and daily tasks.
- Question page presents answer workflow clearly: question, draft, submit, review, next step.
- Mistake book and mastery pages use consistent filters, badges, and progress visuals.

### Job And Application Module

Pages: job import, job list, job detail, company detail, favorites, application list/detail/edit/todos.

Key improvements:

- Job pages emphasize decision support: fit, growth, risk, keywords, company evidence.
- Application pages emphasize pipeline stage, next event, due todos, and risk.
- Todo pages use priority and due states clearly.

### Career Coach Module

Pages: coach home, diagnosis detail, plan detail.

Key improvements:

- Coach home becomes a high-level command panel for diagnosis and plan tasks.
- Diagnosis detail reads like an executive summary with evidence.
- Plan detail shows day-by-day tasks with priority, status, and linked action.

### Admin Console

Pages: dashboard, user list, user detail.

Key improvements:

- Keep admin restrained and readable.
- Metrics and tables share the same token system.
- Role/status actions have clear confirmation and feedback.

## Allowed High-Impact Fixes

These fixes are allowed because they directly affect visual trust, rendering correctness, or interaction reliability.

- Fix CSS variable self-reference for border and shadow tokens.
- Add missing shadow tokens used by layouts.
- Align request/store response usage where runtime rendering would break.
- Wrap visually isolated pages with the correct layout where appropriate.
- Replace unsafe `v-html` for plain text with safe text rendering.
- Standardize status mappings used across pages.
- Improve loading and empty states where blank or misleading states exist.
- Fix obvious success-message behavior when actions may fail.
- Add responsive handling for main navigation and dense page layouts.

Behavior fixes should stay minimal and support the visual/interaction redesign. Larger product logic defects should be documented separately unless they block the redesign.

## Accessibility Requirements

- Maintain at least 4.5:1 contrast for normal text.
- All clickable cards and controls must have pointer and visible hover/focus states.
- Keyboard focus must be visible.
- Form controls need labels or accessible names.
- Color cannot be the only indicator of status.
- Loading states must not trap users without feedback.
- Mobile width around 375px must not horizontally scroll.

## Verification

Frontend verification should include:

- `npm run type-check`
- `npm run build`
- `npm run lint` when style changes are complete, noting that lint runs with `--fix`.
- Manual responsive checks at desktop and mobile widths.
- Manual smoke checks for key flows: login shell, home, resume editor, interview room/report, training center/question, coach home/plan, application list, admin dashboard.

## Acceptance Criteria

- All user-facing and admin-facing pages share one coherent visual system.
- The style feels elevated, unified, and professional while preserving Neubrutalism identity.
- Command center pages clearly expose next actions, progress, and risk.
- AI interactions use consistent loading, result, apply, retry, and failure patterns.
- Charts, stats, badges, empty states, and loading states are consistent.
- Dense forms remain stable and readable.
- Major pages work on desktop and mobile.
- High-impact rendering and interaction issues listed above are fixed or explicitly documented if deferred.
