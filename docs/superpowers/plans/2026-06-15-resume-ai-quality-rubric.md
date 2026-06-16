# Resume AI Quality Rubric Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Improve resume AI generation, optimization, and scoring so section content follows module-specific writing rules and low-quality resumes cannot receive inflated scores.

**Architecture:** Keep the change inside the existing Spring Boot resume AI service. Strengthen module-specific prompts, add lightweight post-processing for optimized section data, and add deterministic scoring quality gates before/after AI scoring. No Controller API or frontend data model changes.

**Tech Stack:** Java 17, Spring Boot, Spring AI `ChatClient`, Jackson `ObjectMapper`, JUnit 5, Mockito, AssertJ, MyBatis-Plus model objects.

---

## File Structure

- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
  - Owns AI prompt text, AI response parsing, resume section optimization, whole-resume optimization, and score post-processing.
  - Add private helpers for module rules, section output cleanup, HTML list normalization, quality signals, and score caps.
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java`
  - Extend existing unit tests for optimization cleanup and strict score capping.
- Reference only: `docs/superpowers/specs/2026-06-15-resume-ai-quality-rubric-design.md`
  - Source of accepted requirements.

---

### Task 1: Add Tests For Module-Specific Optimization Cleanup

**Files:**
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java`

- [ ] **Step 1: Add test for work optimization cleanup**

Append this test after `optimizeSectionReturnsOptimizedData`:

```java
@Test
void optimizeWorkSectionNormalizesDescriptionAndLimitsHighlights() {
    mockAiResponse("""
            ```json
            {
                "company": "XX科技有限公司",
                "position": "Java开发实习生",
                "description": "参与CRM订单模块开发，完成接口联调，支持日处理订单量5000+。优化SQL查询。",
                "highlights": [
                    "CRM订单模块",
                    "接口联调",
                    "这是一条过长的亮点内容，包含大量工作描述，不应该继续停留在亮点标签里，因为它本质上是正文描述",
                    "SQL优化"
                ]
            }
            ```
            """);

    AiOptimizeRequest request = new AiOptimizeRequest();
    request.setSectionId(10L);
    request.setSectionType("work");
    request.setSectionData(Map.of("company", "XX科技有限公司", "description", "参与订单模块开发"));

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) service.optimizeSection(request, "Java开发工程师");

    assertThat(result.get("description").toString()).startsWith("<ul>");
    assertThat(result.get("description").toString()).contains("<li>");
    assertThat(result.get("description").toString()).doesNotContain("这是一条过长的亮点内容");
    assertThat((List<?>) result.get("highlights")).hasSizeLessThanOrEqualTo(3);
}
```

- [ ] **Step 2: Add test for education highlights cleanup**

Append this test after the work cleanup test:

```java
@Test
void optimizeEducationSectionKeepsOnlyShortHighlights() {
    mockAiResponse("""
            ```json
            {
                "school": "江西财经大学",
                "major": "软件工程",
                "degree": "本科",
                "highlights": [
                    "专业前15%",
                    "数据库课程设计",
                    "校级奖学金",
                    "参与公司内部CRM系统订单模块的迭代开发，独立完成订单查询、状态变更等接口编码与测试"
                ]
            }
            ```
            """);

    AiOptimizeRequest request = new AiOptimizeRequest();
    request.setSectionId(11L);
    request.setSectionType("education");
    request.setSectionData(Map.of("school", "江西财经大学"));

    @SuppressWarnings("unchecked")
    Map<String, Object> result = (Map<String, Object>) service.optimizeSection(request, "Java开发工程师");

    assertThat((List<?>) result.get("highlights"))
            .containsExactly("专业前15%", "数据库课程设计", "校级奖学金");
}
```

- [ ] **Step 3: Run tests and verify the new tests fail before implementation**

Run:

```powershell
.\mvnw.cmd test -pl . "-Dtest=ResumeAiServiceImplTest#optimizeWorkSectionNormalizesDescriptionAndLimitsHighlights+optimizeEducationSectionKeepsOnlyShortHighlights"
```

Expected: tests fail because `description` is not normalized and long `highlights` are retained.

---

### Task 2: Implement Section Output Cleanup

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`

- [ ] **Step 1: Add constants near existing constants**

Add after `MAX_AI_SECTION_VALUE_LENGTH`:

```java
private static final int MAX_HIGHLIGHT_COUNT = 3;

private static final int MAX_HIGHLIGHT_LENGTH = 24;

private static final Pattern HTML_LIST_PATTERN = Pattern.compile("<ul>.*<li>.*</li>.*</ul>", Pattern.DOTALL);

private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("[。；;\\n]+|");
```

Then correct the sentence split constant to this exact final line:

```java
private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("[。；;\\n]+");
```

- [ ] **Step 2: Apply cleanup in `optimizeSection`**

Replace:

```java
return objectMapper.readValue(json, Object.class);
```

with:

```java
Object optimized = objectMapper.readValue(json, Object.class);
return normalizeOptimizedSectionData(request.getSectionType(), optimized);
```

- [ ] **Step 3: Apply cleanup in whole-resume optimization loop**

Inside the `optimizedSectionMaps` loop, after `sectionData` is read and basic avatar is preserved, add:

```java
sectionData = normalizeSectionDataMap((String) item.get("sectionType"), sectionData);
```

Ensure `sectionVO.setSectionData(sectionData);` uses the normalized map.

- [ ] **Step 4: Add normalization helpers before `extractTargetPosition`**

Add this code:

```java
private Object normalizeOptimizedSectionData(String sectionType, Object optimized) {
    if (optimized instanceof List<?> list) {
        List<Object> normalized = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                normalized.add(normalizeSectionDataMap(sectionType, toStringKeyMap(map)));
            } else {
                normalized.add(item);
            }
        }
        return normalized;
    }
    if (optimized instanceof Map<?, ?> map) {
        return normalizeSectionDataMap(sectionType, toStringKeyMap(map));
    }
    return optimized;
}

private Map<String, Object> normalizeSectionDataMap(String sectionType, Map<String, Object> sectionData) {
    if (sectionData == null) {
        return null;
    }
    Map<String, Object> normalized = new LinkedHashMap<>(sectionData);
    normalizeHighlights(normalized);
    if ("work".equals(sectionType) || "project".equals(sectionType)) {
        Object description = normalized.get("description");
        if (description != null) {
            normalized.put("description", normalizeDescriptionList(description.toString()));
        }
    }
    return normalized;
}

private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
    Map<String, Object> result = new LinkedHashMap<>();
    source.forEach((key, value) -> {
        if (key != null) {
            result.put(key.toString(), value);
        }
    });
    return result;
}

private void normalizeHighlights(Map<String, Object> sectionData) {
    Object highlights = sectionData.get("highlights");
    if (!(highlights instanceof List<?> list)) {
        return;
    }
    List<String> cleaned = list.stream()
            .filter(item -> item != null && StringUtils.isNotBlank(item.toString()))
            .map(item -> item.toString().trim())
            .filter(item -> item.length() <= MAX_HIGHLIGHT_LENGTH)
            .limit(MAX_HIGHLIGHT_COUNT)
            .collect(Collectors.toList());
    if (cleaned.isEmpty()) {
        sectionData.remove("highlights");
        return;
    }
    sectionData.put("highlights", cleaned);
}

private String normalizeDescriptionList(String description) {
    String text = description == null ? "" : description.trim();
    if (StringUtils.isBlank(text)) {
        return "";
    }
    if (HTML_LIST_PATTERN.matcher(text).find()) {
        return text;
    }
    List<String> points = SENTENCE_SPLIT_PATTERN.splitAsStream(text)
            .map(String::trim)
            .filter(StringUtils::isNotBlank)
            .limit(5)
            .collect(Collectors.toList());
    if (points.isEmpty()) {
        return text;
    }
    return points.stream()
            .map(point -> "<li>" + point + "。</li>")
            .collect(Collectors.joining("", "<ul>", "</ul>"));
}
```

- [ ] **Step 5: Run optimization cleanup tests**

Run:

```powershell
.\mvnw.cmd test -pl . "-Dtest=ResumeAiServiceImplTest#optimizeWorkSectionNormalizesDescriptionAndLimitsHighlights+optimizeEducationSectionKeepsOnlyShortHighlights"
```

Expected: both tests pass.

- [ ] **Step 6: Commit task**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java
git commit -m "fix(resume): normalize AI optimized section content"
```

---

### Task 3: Strengthen Module-Specific Prompts

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
- Test: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java`

- [ ] **Step 1: Add tests that prompts include module rules**

Add this test near other optimize tests:

```java
@Test
void optimizeSectionPromptContainsModuleSpecificRules() {
    mockAiResponse("""
            ```json
            {"description":"<ul><li>负责订单接口开发，支撑日处理订单量5000+。</li></ul>"}
            ```
            """);

    AiOptimizeRequest request = new AiOptimizeRequest();
    request.setSectionId(12L);
    request.setSectionType("project");
    request.setSectionData(Map.of("name", "在线考试系统", "description", "考试系统"));

    service.optimizeSection(request, "Java开发工程师");

    ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
    verify(chatModel).call(captor.capture());
    String prompt = captor.getValue().getInstructions().toString();
    assertThat(prompt).contains("模块化写作规则");
    assertThat(prompt).contains("project");
    assertThat(prompt).contains("<ul><li>");
    assertThat(prompt).contains("highlights 只能作为短标签");
}
```

- [ ] **Step 2: Replace prompt constants**

Replace `GENERATE_SYSTEM_PROMPT`, `OPTIMIZE_SYSTEM_PROMPT`, and `WHOLE_OPTIMIZE_PROMPT` with stricter text that includes:

```java
private static final String MODULE_WRITING_RULES = """
        模块化写作规则：
        1. basic：只处理姓名、联系方式、目标岗位、城市等短字段，不生成经历或亮点。
        2. education：只表达学校、专业、学历、GPA、课程、奖项、在校项目或活动；highlights 最多 3 条短标签，不写工作职责或系统开发长描述。
        3. skills：只整理技能分类和熟练度，不生成项目成果，不虚构未出现的技术栈。
        4. work：description 必须是 HTML 列表 <ul><li>...</li></ul>；每条说明负责/参与了什么、使用什么技术/方法、产生什么结果；highlights 只能作为短标签，最多 3 条。
        5. project：description 必须是 HTML 列表 <ul><li>...</li></ul>；每条说明项目职责、技术实现、结果/价值；必须突出个人贡献，不能只写平台背景。
        6. summary：输出 2-4 句短摘要，突出岗位方向、核心技术栈、项目/实习亮点和求职匹配度，不堆砌标签。
        通用约束：保持原有 JSON 字段结构，不新增无关字段，不编造事实字段。
        """;
```

Update prompt constants to append `MODULE_WRITING_RULES` and explicitly require JSON-only responses.

- [ ] **Step 3: Run prompt test**

Run:

```powershell
.\mvnw.cmd test -pl . "-Dtest=ResumeAiServiceImplTest#optimizeSectionPromptContainsModuleSpecificRules"
```

Expected: PASS.

- [ ] **Step 4: Commit task**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java
git commit -m "fix(resume): add module-specific AI resume prompts"
```

---

### Task 4: Add Strict Score Quality Gates

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java`

- [ ] **Step 1: Add failing test for low-quality score cap**

Append after `scoreResumeReturnsScoreAndSuggestions`:

```java
@Test
void scoreResumeCapsInflatedScoreForWeakResume() {
    mockAiResponse("""
            ```json
            {
                "score": 88,
                "dimensions": {
                    "completeness": 90,
                    "completenessComment": "结构完整",
                    "professionalism": 85,
                    "professionalismComment": "项目较好",
                    "matching": 88,
                    "matchingComment": "匹配Java岗位"
                },
                "suggestions": ["继续补充项目细节"]
            }
            ```
            """);

    SectionVO basic = section("basic", Map.of("name", "张三", "targetPosition", "Java开发工程师"));
    SectionVO skills = section("skills", Map.of("categories", List.of(Map.of("name", "后端", "items", List.of("Java", "Spring Boot")))));
    SectionVO work = section("work", Map.of("company", "XX科技", "position", "Java实习生", "description", "负责后端接口开发。"));
    SectionVO project = section("project", Map.of("name", "在线考试系统", "description", "面向高校的在线考试平台。"));

    AiScoreVO result = service.scoreResume(List.of(basic, skills, work, project), "Java开发工程师");

    assertThat(result.getScore()).isLessThanOrEqualTo(70);
    assertThat(result.getDimensions().getProfessionalism()).isLessThanOrEqualTo(75);
    assertThat(result.getSuggestions()).anyMatch(item -> item.contains("项目经历") && item.contains("具体负责"));
}
```

Also add helper at bottom of test class before `mockAiResponse`:

```java
private SectionVO section(String type, Map<String, Object> data) {
    SectionVO section = new SectionVO();
    section.setSectionType(type);
    section.setSectionData(data);
    return section;
}
```

- [ ] **Step 2: Implement quality signal record and analysis helper**

In `ResumeAiServiceImpl`, add private record and helper before `scoreResume` or near private helpers:

```java
private record ResumeQualitySignals(
        boolean missingBasic,
        boolean weakWorkDescription,
        boolean weakProjectDescription,
        boolean missingQuantifiedResult,
        boolean missingJavaBackendEvidence,
        List<String> warnings) {
}
```

Add helper methods:

```java
private ResumeQualitySignals analyzeResumeQuality(List<SectionVO> sections, String targetPosition) {
    Map<String, List<Map<String, Object>>> grouped = sections.stream()
            .collect(Collectors.groupingBy(SectionVO::getSectionType,
                    Collectors.mapping(SectionVO::getSectionData, Collectors.toList())));
    List<String> warnings = new ArrayList<>();
    boolean missingBasic = isMissingBasicInfo(grouped.get("basic"));
    boolean weakWork = hasWeakExperience(grouped.get("work"));
    boolean weakProject = hasWeakExperience(grouped.get("project"));
    boolean noQuantified = sections.stream().noneMatch(section -> containsQuantifiedResult(section.getSectionData()));
    boolean javaTarget = targetPosition != null && targetPosition.toLowerCase().contains("java");
    boolean noJavaEvidence = javaTarget && sections.stream().noneMatch(section -> containsText(section.getSectionData(), "Java", "Spring Boot", "MyBatis", "JVM", "Redis", "MySQL"));
    if (missingBasic) warnings.add("基本信息缺少姓名、联系方式或目标岗位，影响完整性评分。");
    if (weakWork) warnings.add("工作经历缺少合格分点描述，需要说明具体负责的模块、技术实现和结果。");
    if (weakProject) warnings.add("项目经历缺少具体负责的模块、技术实现、数据库/接口设计和上线结果，不能判定为高质量项目经历。");
    if (noQuantified) warnings.add("简历缺少量化结果，例如处理量、响应时间、性能提升或业务效果。");
    if (noJavaEvidence) warnings.add("目标岗位为 Java 开发，但项目或工作经历没有充分体现 Java 后端核心能力。");
    return new ResumeQualitySignals(missingBasic, weakWork, weakProject, noQuantified, noJavaEvidence, warnings);
}
```

- [ ] **Step 3: Add scoring cap helper**

Add:

```java
private AiScoreVO applyScoreCaps(AiScoreVO score, ResumeQualitySignals signals) {
    int cap = 100;
    if (signals.weakWorkDescription() && signals.weakProjectDescription()) cap = Math.min(cap, 65);
    if (signals.weakProjectDescription()) cap = Math.min(cap, 70);
    if (signals.missingBasic()) cap = Math.min(cap, 60);
    if (signals.missingJavaBackendEvidence()) cap = Math.min(cap, 75);
    if (signals.missingQuantifiedResult()) cap = Math.min(cap, 78);
    score.setScore(Math.min(nullToZero(score.getScore()), cap));
    if (score.getDimensions() != null && (signals.weakWorkDescription() || signals.weakProjectDescription())) {
        score.getDimensions().setProfessionalism(Math.min(nullToZero(score.getDimensions().getProfessionalism()), 75));
    }
    List<String> suggestions = new ArrayList<>();
    if (score.getSuggestions() != null) suggestions.addAll(score.getSuggestions());
    signals.warnings().forEach(warning -> {
        if (!suggestions.contains(warning)) suggestions.add(warning);
    });
    score.setSuggestions(suggestions);
    return score;
}

private int nullToZero(Integer value) {
    return value == null ? 0 : value;
}
```

- [ ] **Step 4: Call analysis and cap in `scoreResume`**

Before serializing sections, add:

```java
ResumeQualitySignals qualitySignals = analyzeResumeQuality(sections, targetPosition);
```

After reading `AiScoreVO`, replace `return objectMapper.readValue(json, AiScoreVO.class);` with:

```java
AiScoreVO score = objectMapper.readValue(json, AiScoreVO.class);
return applyScoreCaps(score, qualitySignals);
```

- [ ] **Step 5: Implement primitive helper methods**

Add helpers used above:

```java
private boolean isMissingBasicInfo(List<Map<String, Object>> basics) {
    if (basics == null || basics.isEmpty()) return true;
    Map<String, Object> basic = basics.get(0);
    return isBlankValue(basic.get("name")) || isBlankValue(basic.get("targetPosition"));
}

private boolean hasWeakExperience(List<Map<String, Object>> experiences) {
    if (experiences == null || experiences.isEmpty()) return true;
    return experiences.stream().anyMatch(item -> {
        Object description = item.get("description");
        String text = description == null ? "" : description.toString();
        return StringUtils.isBlank(text) || !HTML_LIST_PATTERN.matcher(text).find() || countListItems(text) < 2;
    });
}

private int countListItems(String text) {
    Matcher matcher = Pattern.compile("<li>", Pattern.CASE_INSENSITIVE).matcher(text);
    int count = 0;
    while (matcher.find()) count++;
    return count;
}

private boolean containsQuantifiedResult(Map<String, Object> data) {
    return data != null && Pattern.compile("\\d+(\\+|%|ms|s|秒|分钟|万|千|人|次|条|笔)").matcher(data.toString()).find();
}

private boolean containsText(Map<String, Object> data, String... keywords) {
    if (data == null) return false;
    String text = data.toString().toLowerCase();
    for (String keyword : keywords) {
        if (text.contains(keyword.toLowerCase())) return true;
    }
    return false;
}

private boolean isBlankValue(Object value) {
    return value == null || StringUtils.isBlank(value.toString());
}
```

- [ ] **Step 6: Run strict score test**

Run:

```powershell
.\mvnw.cmd test -pl . "-Dtest=ResumeAiServiceImplTest#scoreResumeCapsInflatedScoreForWeakResume"
```

Expected: PASS.

- [ ] **Step 7: Commit task**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java
git commit -m "fix(resume): cap inflated AI resume scores"
```

---

### Task 5: Strengthen Score Prompt Rubric

**Files:**
- Modify: `backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java`
- Modify: `backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java`

- [ ] **Step 1: Add prompt test for strict rubric**

Add:

```java
@Test
void scorePromptContainsStrictRubric() {
    mockAiResponse("""
            ```json
            {"score":60,"dimensions":{"completeness":60,"completenessComment":"一般","professionalism":60,"professionalismComment":"一般","matching":60,"matchingComment":"一般"},"suggestions":["补充项目细节"]}
            ```
            """);

    SectionVO basic = section("basic", Map.of("name", "张三", "targetPosition", "Java开发工程师"));
    service.scoreResume(List.of(basic), "Java开发工程师");

    ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
    verify(chatModel).call(captor.capture());
    String prompt = captor.getValue().getInstructions().toString();
    assertThat(prompt).contains("不要鼓励式评分");
    assertThat(prompt).contains("低质量简历应落在 40-65 分区间");
    assertThat(prompt).contains("80 分以上必须同时满足");
}
```

- [ ] **Step 2: Replace `SCORE_SYSTEM_PROMPT`**

Update `SCORE_SYSTEM_PROMPT` to include strict score ranges, hard criteria, and JSON shape. Include exact phrases used in test:

```java
"不要鼓励式评分。不要因为模块齐全就给高分。\n" +
"低质量简历应落在 40-65 分区间；结构完整但内容浅最多 65-75；80 分以上必须同时满足内容完整、工作/项目分点清晰、技术细节具体、结果可量化、岗位匹配明确。\n" +
"如果项目只有一句平台介绍，必须指出项目经历缺少具体负责模块、接口/数据库设计、技术实现和上线结果。\n"
```

- [ ] **Step 3: Run prompt test**

Run:

```powershell
.\mvnw.cmd test -pl . "-Dtest=ResumeAiServiceImplTest#scorePromptContainsStrictRubric"
```

Expected: PASS.

- [ ] **Step 4: Commit task**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java
git commit -m "fix(resume): enforce strict AI scoring rubric"
```

---

### Task 6: Full Verification

**Files:**
- Verify all changed files.

- [ ] **Step 1: Run focused tests**

Run:

```powershell
.\mvnw.cmd test -pl . "-Dtest=ResumeAiServiceImplTest,ResumeControllerTest,ResumeVersionServiceImplTest"
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Compile backend**

Run:

```powershell
.\mvnw.cmd compile -DskipTests
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Review diff**

Run:

```powershell
git diff -- backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java docs/superpowers/specs/2026-06-15-resume-ai-quality-rubric-design.md docs/superpowers/plans/2026-06-15-resume-ai-quality-rubric.md
```

Expected: diff only includes prompt/rubric/normalization changes, tests, and docs.

- [ ] **Step 4: Final commit if previous tasks were not committed individually**

```powershell
git add backend/src/main/java/com/mianshiba/ai/service/impl/ResumeAiServiceImpl.java backend/src/test/java/com/mianshiba/ai/service/impl/ResumeAiServiceImplTest.java docs/superpowers/specs/2026-06-15-resume-ai-quality-rubric-design.md docs/superpowers/plans/2026-06-15-resume-ai-quality-rubric.md
git commit -m "fix(resume): improve AI optimization and scoring quality"
```

---

## Self-Review

- Spec coverage: module-specific optimization, HTML list descriptions, `highlights` limits, strict scoring, score caps, and tests are covered by Tasks 1-6.
- Placeholder scan: no `TBD`, `TODO`, or vague “implement later” steps remain.
- Type consistency: plan uses existing `ResumeAiServiceImpl`, `AiOptimizeRequest`, `AiScoreVO`, `SectionVO`, `Prompt`, and existing test setup names.
