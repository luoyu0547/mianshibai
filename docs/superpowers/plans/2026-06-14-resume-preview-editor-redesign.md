# Resume Preview Editor Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the resume editing, preview, and AI optimization experience so programmer resumes resemble mature professional resume-builder templates rather than generic web cards.

**Architecture:** Keep the existing Vue component boundaries and backend data model. Improve the existing resume templates, editor shell, basic info editor, template selector, and whole-resume AI dialog with focused frontend changes only.

**Tech Stack:** Vue 3, TypeScript, Element Plus, existing `Nb*` components, html2canvas/jsPDF export path.

---

### Task 1: Improve Basic Information Editing

**Files:**
- Modify: `frontend/src/components/resume/sections/BasicInfoEditor.vue`
- Modify: `frontend/src/types/resume.ts`

- [ ] Add fields commonly seen in mature Chinese resume builders: avatar URL, current status, expected location, expected salary, WeChat, personal website.
- [ ] Keep fields stored in existing flexible `sectionData`; no backend migration.
- [ ] Style the form into grouped blocks: identity, job intent, links.

### Task 2: Rewrite Resume Templates For Programmer Hiring

**Files:**
- Modify: `frontend/src/templates/MinimalTech.vue`
- Modify: `frontend/src/templates/ModernTwoCol.vue`
- Modify: `frontend/src/templates/ClassicFormal.vue`
- Modify: `frontend/src/components/resume/TemplateSelector.vue`

- [ ] Replace current visual style with ATS-friendly, mature PDF-like layouts.
- [ ] Support optional avatar without making it required.
- [ ] Use blue accent section bars like the provided reference, but keep spacing and typography more polished.
- [ ] Ensure all templates prioritize: skills, quantified work achievements, projects, education.

### Task 3: Redesign Resume Editor Shell

**Files:**
- Modify: `frontend/src/views/resume/ResumeEditPage.vue`

- [ ] Add a left module navigation rail similar to the reference builder.
- [ ] Keep central edit area grouped by sections.
- [ ] Keep right A4 preview fixed and readable.
- [ ] Add a top preview toolbar with template selector, font size/line-height visual controls as UI-only placeholders for now.
- [ ] Wire whole-resume AI optimization dialog to the existing `WholeResumeOptimizeDialog` component.

### Task 4: Improve AI Optimization UI

**Files:**
- Modify: `frontend/src/components/resume/WholeResumeOptimizeDialog.vue`
- Modify: `frontend/src/components/resume/AiOptimizeDialog.vue`

- [ ] Replace inline styles with design-system classes.
- [ ] Present before/after scores and suggestions clearly.
- [ ] Keep existing apply events and API calls unchanged.

### Task 5: Verify

**Commands:**
- `npm run type-check`
- `npm run build-only`

**Expected:** both complete successfully. Build warnings about chunk size or third-party comments are acceptable if there are no TypeScript or compilation errors.
