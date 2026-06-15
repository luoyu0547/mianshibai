# Job Sourcing Agent Design

## Background

The current admin job crawl feature is task-based but still URL-driven. Admins must provide source URLs or manual URL lists, and the system does not autonomously discover programmer-related jobs across platforms, company career pages, and public websites.

The target capability is an AI-assisted sourcing agent that reduces manual search and review cost. It should discover many programmer-related job opportunities, extract multi-dimensional job intelligence, summarize results into review cards, and publish only admin-approved jobs to the user-facing job pool and recommendation flow.

## Goals

- Let admins configure sourcing intent instead of manually providing every job URL.
- Discover jobs from multiple public sources, including job platforms, company career pages, official websites, and public job listing pages.
- Extract structured job, company, source, tags, summary, quality, and confidence information from large crawled content.
- Keep crawled data separate from published jobs until admin review.
- Show admins concise review cards for approve, reject, duplicate, and edit-before-approve workflows.
- Make approved jobs available to user-facing job intelligence, recommendation, detail pages, and source-link jumps.
- Use a hybrid crawler: local low-cost fetch first, third-party search/scrape API for discovery, JavaScript-heavy pages, and fallback.

## Non-Goals

- Do not bypass login, captcha, paywall, privacy restrictions, or non-public pages.
- Do not publish crawled jobs directly to users without admin approval.
- Do not replace the existing job, company, analysis, recommendation, and admin modules wholesale.
- Do not require a complete browser automation system in the first version.

## Recommended Approach

Use a hybrid Job Sourcing Agent built on top of the existing `job_crawl_task`, `job_crawl_run`, and `job_crawl_item` model.

The agent has one orchestration service, one source discovery layer, one page fetch layer, one AI extraction layer, one quality/dedup layer, and one review/publish layer. Local fetching handles simple HTML pages. A third-party search/scrape provider handles open web discovery, official career page discovery, JavaScript-rendered pages, and local-fetch fallback.

This balances discovery capability, cost, and operational control.

## Architecture

### Core Services

`JobSourcingAgentService`

Coordinates one sourcing run. It reads task configuration, generates search plans, discovers candidate URLs, fetches content, extracts structured job cards, runs dedup and quality scoring, and writes review items.

`JobSourceDiscoveryService`

Generates search queries and discovers candidate URLs. It supports platform templates, company career page discovery, and third-party open web search.

`JobPageFetchService`

Fetches page content. It tries local `RestClient` first for simple pages and uses a third-party scrape provider when local fetch fails, content is too thin, or the page appears JavaScript-rendered.

`JobSourcingExtractService`

Uses AI to extract structured fields from page content. It produces a strict JSON object for the review card, including missing-field warnings and confidence.

`JobSourcingQualityService`

Scores extracted results by completeness, programmer relevance, source credibility, freshness, salary/city/company clarity, and duplicate risk.

`JobSourcingReviewService`

Handles approve, reject, mark duplicate, and edit-before-approve. Approval creates or updates `job`, `company`, and `job_analysis` records.

### Flow

```text
job_crawl_task
→ JobSourcingAgentService.runTask
→ generate search plan
→ discover candidate URLs
→ fetch page content
→ AI extract structured job card
→ deduplicate and score
→ save job_crawl_item as pending_review
→ admin reviews card
→ approved item publishes job
→ user-facing job intelligence and recommendation read approved jobs
```

## Data Model

### `job_crawl_task`

Keep the existing task table and extend task configuration through typed fields and `config_json`.

Important configuration:

- `keywords`: job directions such as Java backend, Go backend, frontend, AI application engineer.
- `cities`: target cities.
- `experience_levels`: target experience ranges.
- `config_json.targetCount`: desired number of reviewable jobs per run.
- `config_json.maxSearchQueries`: maximum generated search queries.
- `config_json.maxPagesPerSource`: page limit per source.
- `config_json.maxThirdPartyCalls`: third-party API budget per run.
- `config_json.sourceStrategy`: `hybrid` by default.
- `config_json.sourceAllowlist` and `config_json.sourceBlocklist`: optional source control.

### `job_crawl_run`

Represents one agent execution.

Add or populate these semantic fields:

- `total_count`: discovered URLs or candidate pages.
- `success_count`: successfully extracted review cards.
- `duplicate_count`: duplicate or likely duplicate items.
- `failed_count`: failed discovery, fetch, or extraction items.
- `summary`: agent run summary.
- `error_message`: run-level failure reason.
- `cost_json`: third-party calls, AI calls, token estimates, cached hits.

### `job_crawl_item`

Treat each item as a reviewable crawled job card before publishing.

Fields to support:

- `source_url`: original source URL.
- `normalized_url`: normalized URL for dedup.
- `source_platform`: detected source platform or website type.
- `raw_title`: raw page title or listing title.
- `raw_company_name`: raw company name.
- `raw_content`: fetched content snapshot or trimmed content.
- `extracted_json`: structured job card JSON.
- `summary`: AI-generated concise review summary.
- `tags_json`: skills, role type, seniority, city, industry, work mode.
- `quality_score`: 0-100 quality score.
- `confidence_score`: 0-100 extraction confidence.
- `duplicate_of_job_id`: existing job ID if likely duplicate.
- `review_status`: `pending_review`, `approved`, `rejected`, `duplicate`, or `failed`.
- `review_note`: admin note or rejection reason.
- `job_id`: published job ID after approval.

### Published `job`

Only approved crawl items create or update `job` records. User-facing pages and recommendations must only read approved `active` jobs.

Approval maps `extracted_json` into existing fields:

- `title`
- `company_name`
- `source_platform`
- `source_url`
- `city`
- `salary_range`
- `experience_requirement`
- `education_requirement`
- `job_description`
- `job_requirement`
- `tech_stack`
- `raw_content`
- `crawl_task_id`
- `crawl_run_id`
- `normalized_fingerprint`
- `last_seen_at`
- `quality_score`

## Review Status Flow

```text
discovered
→ fetched
→ extracted
→ pending_review
→ approved → published job
→ rejected
→ duplicate
→ failed
```

Failed and duplicate items remain inspectable by admins but are not user-visible.

## Admin Experience

Add an admin review pool for crawled jobs. This can be a new page under the existing admin job crawl module.

Each card displays:

- Job title, company, city, salary.
- Source platform, source URL, crawl time.
- AI summary.
- Responsibilities and requirements.
- Tech stack tags.
- Candidate fit tags, such as Java backend, intern, 3-5 years, AI application.
- Quality score, confidence score, duplicate risk.
- Missing or suspicious fields, such as missing salary, weak company info, short JD, or stale source.

Admin actions:

- Approve: publish as a formal `job`.
- Reject: keep item, do not publish.
- Mark duplicate: link item to an existing `job`.
- Edit and approve: allow correction of title, salary, city, tech stack, summary, and requirements before publishing.

## User-Facing Job Intelligence

User-facing job intelligence and recommendations read only approved `active` jobs.

Recommendation inputs:

- User profile: target position, city, technical direction, work years.
- Resume keywords and skill sections.
- Approved job pool: title, tech stack, city, salary, company profile, quality score, source freshness.

Recommended job cards show:

- Job title, company, salary, city.
- Recommendation reason.
- Core tech stack.
- Job highlights.
- Risk hints.
- Detail page link.
- Original source jump link.

User pages must not expose raw crawl internals, failed items, rejected items, or admin notes.

## Error Handling

- Discovery failures record query and reason, then continue with other queries.
- Fetch failures retry local fetch first, then use third-party scrape fallback if budget allows.
- AI extraction failures keep raw content and mark the item `failed` for later retry.
- Low-quality extraction results enter `pending_review` with explicit warnings rather than auto-publishing.
- Duplicate results are marked `duplicate` or `pending_review` with duplicate risk, not automatically merged.
- Run-level failures update `job_crawl_run.status` and `error_message` without losing completed items.

## Cost Control

- Default task status remains disabled until an admin enables it.
- Each run enforces `targetCount`, `maxSearchQueries`, `maxPagesPerSource`, and `maxThirdPartyCalls`.
- Local fetch is attempted before third-party scrape.
- URL and content snapshots are cached per run to avoid repeated fetches.
- The run records discovered, fetched, extracted, pending review, duplicate, failed, local fetch count, third-party call count, and AI call count.
- Scheduled tasks should use conservative defaults; admins can run manually for immediate sourcing.

## Safety And Compliance

- Crawl public pages only.
- Do not perform login-based scraping.
- Do not bypass captcha, paywalls, private APIs, or platform protections.
- Respect source blocklists and failure frequency throttling.
- Allow admins to block unreliable or unwanted domains.

## Testing Strategy

Backend tests should not call real MySQL, Redis, AI, or third-party search APIs.

Unit tests:

- Search plan generation from task keywords, cities, and experience levels.
- URL normalization and duplicate fingerprint generation.
- Local-fetch fallback decision logic.
- Extraction JSON validation and missing-field warnings.
- Review status transitions.

Service tests:

- A mocked discovery result creates `pending_review` review items.
- Failed fetch or failed extraction marks only affected items failed.
- Approve creates `job`, `company`, and `job_analysis` records.
- Reject does not create a `job`.
- Duplicate item links to existing `job`.

Frontend tests:

- Review pool loads cards and status badges.
- Approve, reject, duplicate, and edit-before-approve update UI state.
- User-facing job list excludes unapproved crawl items.

## Implementation Order

1. Add backend interfaces for discovery, fetch, extraction, quality scoring, and review publishing.
2. Add first-version persistence fields for review status, scores, duplicate link, published job link, review note, extracted JSON, tags JSON, and run cost JSON.
3. Implement mockable hybrid agent orchestration.
4. Add admin review pool APIs.
5. Add frontend review cards and actions.
6. Connect approved jobs to existing user-facing job intelligence and recommendation pages.
7. Add tests for agent run, review flow, and user visibility boundary.

## First-Version Decisions

- Third-party search/scrape access is implemented behind a provider interface. The default adapter is Firecrawl-compatible and is enabled only when `JOB_SOURCING_PROVIDER=firecrawl` and `FIRECRAWL_API_KEY` are configured. If no provider is configured, the agent still runs with local fetch and records discovery fallback as unavailable.
- First version should add physical columns only for fields needed for filtering and status transitions: `review_status`, `quality_score`, `confidence_score`, `duplicate_of_job_id`, `job_id`, and `review_note`. Large or evolving data stays in JSON fields: `extracted_json`, `tags_json`, and `cost_json`.
- Development defaults are conservative: `targetCount=20`, `maxSearchQueries=5`, `maxPagesPerSource=5`, `maxThirdPartyCalls=20`. Production values remain admin-configurable per task, with hard server-side caps to prevent runaway jobs.
