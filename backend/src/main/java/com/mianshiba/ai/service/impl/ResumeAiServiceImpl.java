package com.mianshiba.ai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.mapper.ResumeChatMessageMapper;
import com.mianshiba.ai.mapper.ResumeMapper;
import com.mianshiba.ai.mapper.ResumeSectionMapper;
import com.mianshiba.ai.mapper.UserMapper;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.dto.resume.ResumeImportRequest;
import com.mianshiba.ai.model.dto.resume.ResumeWholeOptimizeRequest;
import com.mianshiba.ai.model.entity.Resume;
import com.mianshiba.ai.model.entity.ResumeChatMessage;
import com.mianshiba.ai.model.entity.ResumeSection;
import com.mianshiba.ai.model.entity.User;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ChatMessageVO;
import com.mianshiba.ai.model.vo.resume.ResumeChatStreamEventVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeImportPreviewVO;
import com.mianshiba.ai.model.vo.resume.ResumePatchProposalVO;
import com.mianshiba.ai.model.vo.resume.ResumeWholeOptimizeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeAiService;
import com.mianshiba.ai.service.tool.ResumePatchTools;
import com.mianshiba.ai.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeAiServiceImpl implements ResumeAiService {

    private static final String MODULE_WRITING_RULES = """
            模块化写作规则：
            1. basic：只处理姓名、联系方式、目标岗位、城市等短字段，不生成经历或亮点。
            2. education：只处理 school、major、degree、startDate、endDate、gpa、activities 等前端教育字段；在校经历必须写入 activities，使用 HTML 列表 <ul><li>...</li></ul>，可包含课程、竞赛、奖项、社团、学生工作、在校项目或活动；不要输出 highlights 作为正文。
            3. skills：必须使用固定的 JSON 结构 { "categories": [{"name": "分类名", "items": ["技能1", "技能2"]}] }；categories 数组至少包含一个分类；不要使用扁平数组或直接列出技能，必须按分类组织；只整理技能分类和熟练度，不生成项目成果，不虚构未出现的技术栈。
            4. work：description 必须是 HTML 列表 <ul><li>...</li></ul>；每条说明负责/参与了什么、使用什么技术/方法、产生什么结果；highlights 只能作为短标签，最多 3 条。
            5. project：description 必须是 HTML 列表 <ul><li>...</li></ul>；每条说明项目职责、技术实现、结果/价值；必须突出个人贡献，不能只写平台背景。
            6. summary：输出 2-4 句短摘要，突出岗位方向、核心技术栈、项目/实习亮点和求职匹配度，不堆砌标签。
            通用约束：保持原有 JSON 字段结构，不新增无关字段，不编造事实字段。
            """;

    private static final String GENERATE_SYSTEM_PROMPT =
            "你是一位专业的简历撰写助手。请根据以下信息生成一份简历：\n" +
            "- 目标岗位：%s\n" +
            "- 技术方向：%s\n" +
            "- 工作年限：%s\n\n" +
            "用户的真实背景描述（请基于此生成，不要编造）：\n%s\n\n" +
            MODULE_WRITING_RULES + "\n" +
            "请以 JSON 数组格式返回，每个元素包含 sectionType 和 sectionData 字段。\n" +
            "sectionType 可选值：basic, education, work, project, skills, summary\n" +
            "sectionData 是一个 JSON 对象，包含该模块的具体内容。\n" +
            "请直接返回 JSON 数组，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final String OPTIMIZE_SYSTEM_PROMPT =
            "你是一位专业的简历优化助手。请优化以下简历模块内容，目标岗位是 %s，模块类型是 %s。\n" +
            MODULE_WRITING_RULES + "\n" +
            "请返回优化后的 JSON 对象，保持原有的数据结构，只优化内容质量。\n" +
            "直接返回 JSON 对象，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final String SCORE_SYSTEM_PROMPT =
            "你是一位专业的简历评估助手。请对以下简历内容进行评分，目标岗位是 %s。\n" +
            "评分必须严格、克制，不要鼓励式评分。请按真实招聘筛选标准判断：\n" +
            "1. 低质量简历应落在 40-65 分区间，例如只有笼统职责、缺少项目细节、缺少量化结果或岗位匹配证据。\n" +
            "2. 70 分以上需要具备完整模块、清晰职责、明确技术栈和可验证成果。\n" +
            "3. 80 分以上必须同时满足：基础信息完整、工作/项目经历具体、个人贡献清楚、技术实现可信、有量化结果、与目标岗位高度匹配。\n" +
            "4. 不要因为格式完整就给高分；内容空泛、只有标签或只有项目背景时必须扣分。\n\n" +
            "请返回 JSON 格式的评分结果，包含：\n" +
            "- score：总分（0-100）\n" +
            "- dimensions：维度评分\n" +
            "  - completeness：完整性（0-100）\n" +
            "  - completenessComment：完整性评语\n" +
            "  - professionalism：专业度（0-100）\n" +
            "  - professionalismComment：专业度评语\n" +
            "  - matching：岗位匹配度（0-100）\n" +
            "  - matchingComment：匹配度评语\n" +
            "- suggestions：改进建议列表\n\n" +
            "直接返回 JSON 对象，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final String CHAT_SYSTEM_PROMPT =
            "你是一位专业的简历顾问助手。用户正在编辑简历，以下是当前简历的模块内容摘要：\n%s\n\n" +
            "请根据用户的问题提供建议。回答要简洁专业。\n" +
            "如果用户明确要求你修改、补充、润色、重写或填写简历内容，请调用 proposeResumePatch 工具生成待确认的修改提案。\n" +
            "调用 proposeResumePatch 时必须遵守前端字段结构：education 的在校经历写入 activities，不要写 highlights；work 和 project 的正文写入 description；不要新增前端没有使用的字段。\n" +
            MODULE_WRITING_RULES + "\n" +
            "工具只用于提出修改，不会保存简历；用户确认前不要声称已经修改完成。";

    private static final String IMPORT_PARSE_PROMPT =
            "你是一位专业的简历解析助手。请将以下简历文本解析为结构化的 JSON 格式。\n\n" +
            "简历文本：\n%s\n\n" +
            "请以 JSON 数组格式返回，每个元素包含 sectionType 和 sectionData 字段。\n" +
            "sectionType 可选值：basic, education, work, project, skills, summary\n" +
            "sectionData 是一个 JSON 对象，包含该模块的具体内容。\n" +
            "如果有无法归类的信息，放入 summary 模块。\n" +
            "请直接返回 JSON 数组，不要包含其他文字。可以用 ```json ``` 包裹。";

    private static final String WHOLE_OPTIMIZE_PROMPT =
            "你是一位专业的简历优化助手。请逐模块深度优化以下简历内容。\n\n" +
            "用户优化方向：%s\n\n" +
            "当前简历模块：\n%s\n\n" +
            "优化要求（严格执行）：\n" +
            "1. 逐模块重写：每个模块都必须输出优化后的 sectionData，不要跳过任何模块\n" +
            "2. 工作/项目经历：每条描述必须有具体的技术行为、个人贡献和量化结果（如 QPS 提升 30%%、响应从 3s 降至 0.8s）\n" +
            "3. 自我评价：重写为 2-4 句，突出核心技术栈、项目亮点和岗位匹配度，去掉套话\n" +
            "4. 技能标签：按前端/后端/工具等真实分类组织，不要臆造技术栈\n" +
            "5. 教育经历：补全在校活动和亮点，使用 <ul><li>...</li></ul> HTML 格式\n" +
            "6. 禁止笼统话术：不要使用'参与开发''协助完成'等模糊词，要写出具体负责了什么、用了什么技术、达成了什么效果\n" +
            MODULE_WRITING_RULES + "\n" +
            "请返回 JSON 格式，包含：\n" +
            "- globalSuggestions：全局建议列表（字符串数组，3-5 条具体建议，不能是套话）\n" +
            "- optimizedSections：优化后的完整模块数组，每个元素包含 sectionType 和 sectionData\n\n" +
            "必须返回完整 JSON，不要省略任何模块。可以用 ```json ``` 包裹。";

    private static final Pattern JSON_CODE_BLOCK_PATTERN =
            Pattern.compile("```(?:json)?\\s*\\n?(.*?)\\n?```", Pattern.DOTALL);

    private static final int MAX_CHAT_SECTION_VALUE_LENGTH = 1000;

    private static final int MAX_AI_SECTION_VALUE_LENGTH = 2000;

    private static final int MAX_HIGHLIGHT_COUNT = 3;

    private static final int MAX_HIGHLIGHT_LENGTH = 24;

    private static final Pattern HTML_LIST_PATTERN = Pattern.compile("<ul>.*<li>.*</li>.*</ul>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile("[。；;\\n]+");

    private static final Pattern INLINE_FILE_DATA_PATTERN =
            Pattern.compile("^data:[^;]+;base64,.*", Pattern.DOTALL);

    private final ChatClient chatClient;
    private final ResumeMapper resumeMapper;
    private final ResumeSectionMapper resumeSectionMapper;
    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final ResumeChatMessageMapper chatMessageMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ResumeDetailVO generateResume(String authorizationHeader, AiGenerateRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        String techDirection = request.getTechDirection() != null ? request.getTechDirection() : "不限";
        String workYears = request.getWorkYears() != null ? String.valueOf(request.getWorkYears()) : "不限";
        String background = request.getBackgroundDescription() != null && !request.getBackgroundDescription().isBlank()
                ? request.getBackgroundDescription().trim() : "无（AI 可自行生成合理内容）";
        String systemPrompt = String.format(GENERATE_SYSTEM_PROMPT,
                request.getTargetPosition(), techDirection, workYears, escapePercent(background));
        String userMessage = String.format("请为「%s」岗位生成一份简历。", request.getTargetPosition());

        String aiResponse = callAi(systemPrompt, userMessage);
        log.debug("AI 生成简历响应: {}", StringUtils.abbreviate(aiResponse, 1000));

        String json = extractJsonFromResponse(aiResponse);
        List<Map<String, Object>> sectionItems;
        try {
            sectionItems = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 生成简历响应解析失败，提取后 JSON: {}，原始响应: {}", json, aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR,
                    "AI 生成简历响应解析失败，请稍后重试。原始响应片段: " + StringUtils.abbreviate(aiResponse, 500));
        }

        Resume resume = new Resume();
        resume.setUserId(userId);
        resume.setTitle(request.getTargetPosition() + " - 简历");
        resume.setTemplateType("minimal_tech");
        resume.setStatus("draft");
        resume.setSource("ai");
        resume.setVersion(1);
        resume.setIsDelete(0);
        resumeMapper.insert(resume);

        List<ResumeSection> sectionEntities = new ArrayList<>();
        for (int i = 0; i < sectionItems.size(); i++) {
            Map<String, Object> item = sectionItems.get(i);
            ResumeSection section = new ResumeSection();
            section.setResumeId(resume.getId());
            section.setSectionType((String) item.get("sectionType"));
            @SuppressWarnings("unchecked")
            Map<String, Object> sectionData = (Map<String, Object>) item.get("sectionData");
            sectionData = normalizeSectionDataMap(section.getSectionType(), sectionData);
            section.setSectionData(sectionData);
            section.setSortOrder(i);
            section.setAiGenerated(1);
            section.setIsDelete(0);
            resumeSectionMapper.insert(section);
            sectionEntities.add(section);
        }

        return toResumeDetailVO(resume, sectionEntities);
    }

    @Override
    public Object optimizeSection(AiOptimizeRequest request, String targetPosition) {
        String sectionDataJson;
        try {
            sectionDataJson = objectMapper.writeValueAsString(request.getSectionData());
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        String systemPrompt = String.format(OPTIMIZE_SYSTEM_PROMPT,
                targetPosition != null ? targetPosition : "未知岗位",
                request.getSectionType());

        String aiResponse = callAi(systemPrompt, sectionDataJson);
        log.debug("AI 优化模块响应: {}", StringUtils.abbreviate(aiResponse, 1000));

        String json = extractJsonFromResponse(aiResponse);
        try {
            Object optimized = objectMapper.readValue(json, Object.class);
            return normalizeOptimizedSectionData(request.getSectionType(), optimized);
        } catch (JsonProcessingException e) {
            log.error("AI 优化模块响应解析失败，提取后 JSON: {}，原始响应: {}", json, aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR,
                    "AI 优化模块响应解析失败，请稍后重试。原始响应片段: " + StringUtils.abbreviate(aiResponse, 500));
        }
    }

    @Override
    public AiScoreVO scoreResume(List<SectionVO> sections, String targetPosition) {
        ResumeQualitySignals qualitySignals = analyzeResumeQuality(sections, targetPosition);
        List<SectionVO> truncatedSections = sections.stream()
                .map(this::toTruncatedSectionForAi)
                .collect(Collectors.toList());
        String sectionsJson;
        try {
            sectionsJson = objectMapper.writeValueAsString(truncatedSections);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR);
        }

        String systemPrompt = String.format(SCORE_SYSTEM_PROMPT,
                targetPosition != null ? targetPosition : "未知岗位");

        String aiResponse = callAi(systemPrompt, sectionsJson);
        log.debug("AI 评分响应: {}", StringUtils.abbreviate(aiResponse, 1000));

        String json = extractJsonFromResponse(aiResponse);
        json = normalizeScoreDimensions(json);
        try {
            AiScoreVO score = objectMapper.readValue(json, AiScoreVO.class);
            return applyScoreCaps(score, qualitySignals);
        } catch (JsonProcessingException e) {
            log.error("AI 评分响应解析失败，提取后 JSON: {}，原始响应: {}", json, aiResponse, e);
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR,
                    "AI 评分响应解析失败，请稍后重试。原始响应片段: " + StringUtils.abbreviate(aiResponse, 500));
        }
    }

    @Override
    public Flux<ResumeChatStreamEventVO> chatStream(String authorizationHeader, Long resumeId, String message) {
        Long userId = resolveUserId(authorizationHeader);
        getResumeAndCheckOwner(resumeId, userId);

        ResumeChatMessage userMsg = new ResumeChatMessage();
        userMsg.setResumeId(resumeId);
        userMsg.setRole("user");
        userMsg.setContent(message);
        chatMessageMapper.insert(userMsg);

        List<ResumeSection> sections = resumeSectionMapper.selectList(
                Wrappers.lambdaQuery(ResumeSection.class)
                        .eq(ResumeSection::getResumeId, resumeId)
                        .orderByAsc(ResumeSection::getSortOrder));

        String sectionsSummary = buildSectionsSummary(sections);
        String systemPrompt = String.format(CHAT_SYSTEM_PROMPT, sectionsSummary);
        StringBuilder fullResponse = new StringBuilder();
        List<ResumePatchProposalVO> proposals = new ArrayList<>();
        ResumePatchTools tools = new ResumePatchTools(proposals::add);

        Flux<ResumeChatStreamEventVO> textStream = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .tools(tools)
                .stream()
                .content()
                .doOnNext(fullResponse::append)
                .map(ResumeChatStreamEventVO::message);

        return textStream
                .concatWith(Flux.defer(() -> Flux.fromIterable(proposals).map(ResumeChatStreamEventVO::proposal)))
                .doOnComplete(() -> saveAssistantMessage(resumeId, fullResponse.toString()))
                .doOnError(e -> log.error("AI 对话流异常", e))
                .onErrorResume(e -> {
                    logAiProviderError("AI 对话流调用失败，尝试降级为非流式调用", e);
                    return Flux.defer(() -> {
                        String fallbackResponse = callAi(systemPrompt, message);
                        fullResponse.append(fallbackResponse);
                        saveAssistantMessage(resumeId, fallbackResponse);
                        return Flux.just(ResumeChatStreamEventVO.message(fallbackResponse));
                    });
                });
    }

    private void saveAssistantMessage(Long resumeId, String content) {
        try {
            ResumeChatMessage assistantMsg = new ResumeChatMessage();
            assistantMsg.setResumeId(resumeId);
            assistantMsg.setRole("assistant");
            assistantMsg.setContent(content);
            chatMessageMapper.insert(assistantMsg);
        } catch (Exception e) {
            log.error("保存 AI 回复失败：resumeId={}", resumeId, e);
        }
    }

    @Override
    public List<ChatMessageVO> getChatHistory(Long resumeId) {
        List<ResumeChatMessage> messages = chatMessageMapper.selectList(
                Wrappers.lambdaQuery(ResumeChatMessage.class)
                        .eq(ResumeChatMessage::getResumeId, resumeId)
                        .orderByAsc(ResumeChatMessage::getCreateTime));

        return messages.stream().map(m -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(m.getId());
            vo.setRole(m.getRole());
            vo.setContent(m.getContent());
            vo.setRelatedSectionType(m.getRelatedSectionType());
            vo.setCreateTime(m.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public ResumeImportPreviewVO importResumePreview(String authorizationHeader, ResumeImportRequest request) {
        resolveUserId(authorizationHeader);

        if (request.getRawText() == null || request.getRawText().isBlank()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        String systemPrompt = String.format(IMPORT_PARSE_PROMPT, request.getRawText());
        String aiResponse = callAi(systemPrompt, "请解析这份简历。");
        log.debug("AI 导入简历响应: {}", StringUtils.abbreviate(aiResponse, 1000));

        String json = extractJsonFromResponse(aiResponse);
        List<Map<String, Object>> sectionItems;
        try {
            sectionItems = objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 导入简历响应解析失败，提取后 JSON: {}，原始响应: {}", json, aiResponse, e);
            throw new BusinessException(ErrorCode.RESUME_IMPORT_ERROR,
                    "AI 导入简历响应解析失败，请稍后重试。原始响应片段: " + StringUtils.abbreviate(aiResponse, 500));
        }

        List<String> warnings = new ArrayList<>();
        String title = "导入简历";

        List<SectionVO> sectionVOs = new ArrayList<>();
        for (int i = 0; i < sectionItems.size(); i++) {
            Map<String, Object> item = sectionItems.get(i);
            String sectionType = (String) item.get("sectionType");
            @SuppressWarnings("unchecked")
            Map<String, Object> sectionData = (Map<String, Object>) item.get("sectionData");

            if (sectionType == null || sectionData == null) {
                warnings.add("第 " + (i + 1) + " 个模块缺少 sectionType 或 sectionData");
                continue;
            }

            if ("basic".equals(sectionType) && sectionData.containsKey("name")) {
                title = sectionData.get("name").toString() + " - 简历";
            }

            SectionVO sectionVO = new SectionVO();
            sectionVO.setSectionType(sectionType);
            sectionVO.setSectionData(sectionData);
            sectionVO.setSortOrder(i);
            sectionVO.setAiGenerated(1);
            sectionVOs.add(sectionVO);
        }

        ResumeImportPreviewVO vo = new ResumeImportPreviewVO();
        vo.setTitle(title);
        vo.setTemplateType("minimal_tech");
        vo.setSections(sectionVOs);
        vo.setWarnings(warnings);
        return vo;
    }

    @Override
    public ResumeWholeOptimizeVO optimizeWholeResume(String authorizationHeader, ResumeWholeOptimizeRequest request) {
        Long userId = resolveUserId(authorizationHeader);

        Resume resume = getResumeAndCheckOwner(request.getResumeId(), userId);

        List<ResumeSection> sections = resumeSectionMapper.selectList(
                Wrappers.lambdaQuery(ResumeSection.class)
                        .eq(ResumeSection::getResumeId, resume.getId())
                        .orderByAsc(ResumeSection::getSortOrder));

        List<SectionVO> currentSections = sections.stream().map(this::toSectionVO).collect(Collectors.toList());
        List<SectionVO> truncatedSections = currentSections.stream()
                .map(this::toTruncatedSectionForAi)
                .collect(Collectors.toList());
        String targetPosition = request.getTargetPosition() != null ? request.getTargetPosition() : extractTargetPosition(currentSections);

        AiScoreVO beforeScoreResult = scoreResume(truncatedSections, targetPosition);
        Integer beforeScore = beforeScoreResult.getScore();

        String jobContext = "";

        if (request.getOptimizeGoal() != null && !request.getOptimizeGoal().isBlank()) {
            jobContext += "\n\n用户优化目标：" + request.getOptimizeGoal();
        }

        String sectionsJson;
        try {
            sectionsJson = objectMapper.writeValueAsString(truncatedSections);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.RESUME_OPTIMIZE_ERROR);
        }

        String systemPrompt = String.format(WHOLE_OPTIMIZE_PROMPT, escapePercent(jobContext), escapePercent(sectionsJson));
        String aiResponse = callAi(systemPrompt, "请优化这份简历。");
        log.debug("AI 整份优化响应: {}", StringUtils.abbreviate(aiResponse, 1000));

        String json = extractJsonFromResponse(aiResponse);
        Map<String, Object> result;
        try {
            result = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 整份优化响应解析失败，提取后 JSON: {}，原始响应: {}", json, aiResponse, e);
            throw new BusinessException(ErrorCode.RESUME_OPTIMIZE_ERROR,
                    "AI 整份优化响应解析失败，请稍后重试。原始响应片段: " + StringUtils.abbreviate(aiResponse, 500));
        }

        @SuppressWarnings("unchecked")
        List<String> globalSuggestions = (List<String>) result.get("globalSuggestions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> optimizedSectionMaps = (List<Map<String, Object>>) result.get("optimizedSections");

        Map<String, Object> originalBasicData = currentSections.stream()
                .filter(s -> "basic".equals(s.getSectionType()))
                .findFirst()
                .map(SectionVO::getSectionData)
                .orElse(null);

        List<SectionVO> optimizedSections = new ArrayList<>();
        if (optimizedSectionMaps != null) {
            for (int i = 0; i < optimizedSectionMaps.size(); i++) {
                Map<String, Object> item = optimizedSectionMaps.get(i);
                SectionVO sectionVO = new SectionVO();
                sectionVO.setSectionType((String) item.get("sectionType"));
                @SuppressWarnings("unchecked")
                Map<String, Object> sectionData = (Map<String, Object>) item.get("sectionData");
                if ("basic".equals(item.get("sectionType")) && originalBasicData != null && sectionData != null) {
                    Object origAvatar = originalBasicData.get("avatar");
                    if (origAvatar != null && !sectionData.containsKey("avatar")) {
                        sectionData.put("avatar", origAvatar);
                    }
                }
                sectionData = normalizeSectionDataMap((String) item.get("sectionType"), sectionData);
                sectionVO.setSectionData(sectionData);
                sectionVO.setSortOrder(i);
                sectionVO.setAiGenerated(1);
                optimizedSections.add(sectionVO);
            }
        }

        ResumeWholeOptimizeVO vo = new ResumeWholeOptimizeVO();
        vo.setBeforeScore(beforeScore);

        List<SectionVO> truncatedOptimized = optimizedSections.stream()
                .map(this::toTruncatedSectionForAi)
                .collect(Collectors.toList());
        AiScoreVO afterScoreResult = scoreResume(truncatedOptimized, targetPosition);
        vo.setEstimatedAfterScore(afterScoreResult.getScore());
        vo.setGlobalSuggestions(globalSuggestions != null ? globalSuggestions : new ArrayList<>());
        vo.setOptimizedSections(optimizedSections);
        return vo;
    }

    private Object normalizeOptimizedSectionData(String sectionType, Object optimized) {
        if ("skills".equals(sectionType)) {
            return normalizeSkillsSectionData(optimized);
        }
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
        if ("education".equals(sectionType)) {
            normalizeEducationActivities(normalized);
        } else {
            normalizeHighlights(normalized);
        }
        if ("work".equals(sectionType) || "project".equals(sectionType)) {
            Object description = normalized.get("description");
            if (description != null) {
                normalized.put("description", normalizeDescriptionList(description.toString()));
            }
        }
        return normalized;
    }

    @SuppressWarnings("unchecked")
    private Object normalizeSkillsSectionData(Object optimized) {
        if (optimized instanceof List<?> list) {
            List<Map<String, Object>> categories = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> itemMap) {
                    Map<String, Object> cat = new LinkedHashMap<>();
                    Object name = itemMap.get("name");
                    if (name == null) name = itemMap.get("category");
                    cat.put("name", name instanceof String && !((String) name).isBlank() ? name : "技能");
                    Object items = itemMap.get("items");
                    if (items == null) items = itemMap.get("skills");
                    cat.put("items", items instanceof List<?> l ? new ArrayList<>(l) : List.of());
                    categories.add(cat);
                } else if (item instanceof String s) {
                    if (categories.isEmpty()) {
                        Map<String, Object> cat = new LinkedHashMap<>();
                        cat.put("name", "技能");
                        cat.put("items", new ArrayList<>(List.of(s)));
                        categories.add(cat);
                    } else {
                        Object existingItems = categories.get(categories.size() - 1).get("items");
                        if (existingItems instanceof List existing) {
                            existing.add(s);
                        }
                    }
                }
            }
            if (categories.isEmpty()) {
                Map<String, Object> fallback = new LinkedHashMap<>();
                fallback.put("categories", List.of());
                return fallback;
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("categories", categories);
            return result;
        }
        if (optimized instanceof Map<?, ?> map) {
            if (map.containsKey("categories")) {
                return normalizeSectionDataMap("skills", toStringKeyMap(map));
            }
            if (map.containsKey("name") || map.containsKey("items")) {
                Map<String, Object> cat = new LinkedHashMap<>();
                Object name = map.get("name");
                if (name == null) name = map.get("category");
                cat.put("name", name instanceof String && !((String) name).isBlank() ? name : "技能");
                Object items = map.get("items");
                if (items == null) items = map.get("skills");
                cat.put("items", items instanceof List<?> l ? new ArrayList<>(l) : List.of());
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("categories", List.of(cat));
                return result;
            }
            Map<String, Object> cat = new LinkedHashMap<>();
            cat.put("name", "技能");
            List<String> skillItems = map.values().stream()
                    .filter(v -> v instanceof String)
                    .map(Object::toString)
                    .collect(Collectors.toList());
            cat.put("items", skillItems.isEmpty() ? List.of() : skillItems);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("categories", List.of(cat));
            return result;
        }
        return optimized;
    }

    private void normalizeEducationActivities(Map<String, Object> sectionData) {
        Object activities = sectionData.get("activities");
        if (activities == null) {
            activities = sectionData.remove("description");
        }
        if (activities == null) {
            activities = sectionData.remove("highlights");
        } else {
            sectionData.remove("highlights");
        }
        if (activities != null) {
            sectionData.put("activities", normalizeDescriptionList(toDescriptionText(activities)));
        }
    }

    private String toDescriptionText(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item != null && StringUtils.isNotBlank(item.toString()))
                    .map(item -> item.toString().trim())
                    .collect(Collectors.joining("。"));
        }
        return value == null ? "" : value.toString();
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

    private ResumeQualitySignals analyzeResumeQuality(List<SectionVO> sections, String targetPosition) {
        if (sections == null || sections.isEmpty()) {
            return new ResumeQualitySignals(true, true, true, 0);
        }
        boolean missingBasicInfo = isMissingBasicInfo(sections, targetPosition);
        int experiencePointCount = 0;
        boolean hasExperienceSection = false;
        boolean hasQuantifiedResult = false;
        for (SectionVO section : sections) {
            if (section == null || section.getSectionData() == null) {
                continue;
            }
            String sectionType = section.getSectionType();
            if ("work".equals(sectionType) || "project".equals(sectionType)) {
                hasExperienceSection = true;
                Object description = section.getSectionData().get("description");
                experiencePointCount += countListItems(description);
                hasQuantifiedResult = hasQuantifiedResult || containsQuantifiedResult(section.getSectionData());
            }
        }
        boolean weakExperience = hasExperienceSection && experiencePointCount < 3;
        boolean lacksQuantifiedResult = hasExperienceSection && !hasQuantifiedResult;
        return new ResumeQualitySignals(missingBasicInfo, weakExperience, lacksQuantifiedResult, experiencePointCount);
    }

    private AiScoreVO applyScoreCaps(AiScoreVO score, ResumeQualitySignals qualitySignals) {
        if (score == null || qualitySignals == null) {
            return score;
        }
        int cap = 100;
        List<String> extraSuggestions = new ArrayList<>();
        if (qualitySignals.missingBasicInfo()) {
            cap = Math.min(cap, 60);
            extraSuggestions.add("补全姓名、目标岗位等基础信息后再评估简历竞争力。");
        }
        if (qualitySignals.weakExperience()) {
            cap = Math.min(cap, 70);
            extraSuggestions.add("项目经历和工作经历需要补充具体负责内容、技术实现和结果，避免只写背景或笼统职责。");
        }
        if (qualitySignals.lacksQuantifiedResult()) {
            cap = Math.min(cap, 75);
            extraSuggestions.add("补充可验证的量化结果，例如性能提升、规模、效率、用户量或交付成果。");
        }
        if (cap == 100) {
            return score;
        }
        score.setScore(capValue(score.getScore(), cap));
        AiScoreVO.ScoreDimensions dimensions = score.getDimensions();
        if (dimensions != null) {
            dimensions.setCompleteness(capValue(dimensions.getCompleteness(), cap));
            dimensions.setProfessionalism(capValue(dimensions.getProfessionalism(), Math.min(cap, 75)));
            dimensions.setMatching(capValue(dimensions.getMatching(), cap));
        }
        List<String> suggestions = score.getSuggestions() == null ? new ArrayList<>() : new ArrayList<>(score.getSuggestions());
        for (String suggestion : extraSuggestions) {
            if (!suggestions.contains(suggestion)) {
                suggestions.add(suggestion);
            }
        }
        score.setSuggestions(suggestions);
        return score;
    }

    private Integer capValue(Integer value, int cap) {
        if (value == null) {
            return null;
        }
        return Math.min(value, cap);
    }

    private boolean isMissingBasicInfo(List<SectionVO> sections, String targetPosition) {
        Map<String, Object> basicData = sections.stream()
                .filter(section -> section != null && "basic".equals(section.getSectionType()))
                .map(SectionVO::getSectionData)
                .filter(data -> data != null)
                .findFirst()
                .orElse(Map.of());
        boolean missingName = isBlankValue(basicData.get("name"));
        boolean missingTarget = isBlankValue(targetPosition) && isBlankValue(basicData.get("targetPosition"));
        return missingName || missingTarget;
    }

    private int countListItems(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof List<?> list) {
            return list.size();
        }
        String text = value.toString();
        Matcher matcher = Pattern.compile("<li\\b[^>]*>", Pattern.CASE_INSENSITIVE).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        if (count > 0) {
            return count;
        }
        return (int) SENTENCE_SPLIT_PATTERN.splitAsStream(text)
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .count();
    }

    private boolean containsQuantifiedResult(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(this::containsQuantifiedResult);
        }
        if (value instanceof List<?> list) {
            return list.stream().anyMatch(this::containsQuantifiedResult);
        }
        String text = value.toString();
        return Pattern.compile("\\d|[一二三四五六七八九十百千万]+(个|项|次|人|天|周|月|年|倍|%|％)").matcher(text).find();
    }

    private boolean isBlankValue(Object value) {
        return value == null || StringUtils.isBlank(value.toString());
    }

    private record ResumeQualitySignals(
            boolean missingBasicInfo,
            boolean weakExperience,
            boolean lacksQuantifiedResult,
            int experiencePointCount) {
    }

    private String extractTargetPosition(List<SectionVO> sections) {
        return sections.stream()
                .filter(s -> "basic".equals(s.getSectionType()))
                .findFirst()
                .map(SectionVO::getSectionData)
                .filter(data -> data != null && data.containsKey("targetPosition"))
                .map(data -> data.get("targetPosition").toString())
                .orElse(null);
    }

    private Long resolveUserId(String authorizationHeader) {
        String token = jwtUtils.resolveToken(authorizationHeader);
        JwtUtils.JwtUserClaims claims = jwtUtils.parseToken(token);
        User user = userMapper.selectById(claims.userId());
        if (user == null || Integer.valueOf(1).equals(user.getIsDelete())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        if (Integer.valueOf(1).equals(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR);
        }
        return user.getId();
    }

    private Resume getResumeAndCheckOwner(Long resumeId, Long userId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!resume.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return resume;
    }

    private String callAi(String systemPrompt, String userMessage) {
        try {
            String response = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            if (response == null || response.isBlank()) {
                log.warn("AI 返回空响应");
                throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 返回空响应");
            }
            return response;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logAiProviderError("AI 服务调用失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private void logAiProviderError(String message, Throwable e) {
        if (e instanceof WebClientResponseException webClientException) {
            log.error("{}，status={}，responseBody={}",
                    message,
                    webClientException.getStatusCode(),
                    webClientException.getResponseBodyAsString(),
                    e);
            return;
        }
        log.error(message, e);
    }

    private String escapePercent(String value) {
        return value == null ? "" : value.replace("%", "%%");
    }

    /**
     * AI may return dimensions as nested objects {score, comment} instead of flat fields.
     * Flatten: {"completeness":{"score":65,"comment":"..."}} → {"completeness":65,"completenessComment":"..."}
     */
    private String normalizeScoreDimensions(String json) {
        if (json == null || json.isBlank()) return json;
        String[] dims = {"completeness", "professionalism", "matching"};
        String result = json;
        for (String dim : dims) {
            String key = "\"" + dim + "\"";
            int keyStart = result.indexOf(key);
            if (keyStart < 0) continue;
            int colonPos = result.indexOf(':', keyStart + key.length());
            if (colonPos < 0) continue;
            int valStart = colonPos + 1;
            valStart = skipWhitespace(result, valStart);
            if (valStart >= result.length()) continue;
            if (result.charAt(valStart) == '{') {
                int objEnd = findJsonObjectEnd(result, valStart);
                if (objEnd < 0) continue;
                String nestedObj = result.substring(valStart + 1, objEnd);
                String score = extractJsonField(nestedObj, "score");
                String comment = extractJsonField(nestedObj, "comment");
                if (score != null) {
                    result = result.substring(0, valStart) + score + ","
                            + "\"" + dim + "Comment\": " + (comment != null ? comment : "\"\"")
                            + result.substring(objEnd + 1);
                }
            }
        }
        return result;
    }

    private String extractJsonField(String jsonObj, String fieldName) {
        String key = "\"" + fieldName + "\"";
        int idx = jsonObj.indexOf(key);
        if (idx < 0) return null;
        int colon = jsonObj.indexOf(':', idx + key.length());
        if (colon < 0) return null;
        int valStart = colon + 1;
        valStart = skipWhitespace(jsonObj, valStart);
        if (valStart >= jsonObj.length()) return null;
        if (jsonObj.charAt(valStart) == '"') {
            int end = jsonObj.indexOf('"', valStart + 1);
            if (end < 0) return null;
            int esc = jsonObj.indexOf('\\', valStart + 1);
            while (esc >= 0 && esc < end) {
                end = jsonObj.indexOf('"', end + 1);
                if (end < 0) return null;
                esc = jsonObj.indexOf('\\', esc + 2);
            }
            return jsonObj.substring(valStart, end + 1);
        }
        int end = jsonObj.indexOf(',', valStart);
        if (end < 0) end = jsonObj.length();
        return jsonObj.substring(valStart, end).trim();
    }

    private int findJsonObjectEnd(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escape) { escape = false; } else if (c == '\\') { escape = true; } else if (c == '"') { inString = false; }
                continue;
            }
            if (c == '"') { inString = true; continue; }
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private int skipWhitespace(String text, int pos) {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
        return pos;
    }

    private String extractJsonFromResponse(String text) {
        if (text == null) {
            return "";
        }
        // 1. 清洗常见污染字符（BOM、零宽字符、控制字符）
        String cleaned = sanitizeJsonText(text);

        // 2. 优先匹配 Markdown JSON 代码块
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // 3. 未匹配到代码块时，尝试定位第一个 JSON 对象/数组边界
        String json = extractFirstJson(cleaned);
        if (json != null) {
            return json;
        }

        // 4. 兜底返回清洗后的原文，便于上层记录和排查
        return cleaned;
    }

    private String sanitizeJsonText(String text) {
        return text
                .replace("\uFEFF", "")
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "")
                .replaceAll("[\\u200B-\\u200D\\uFEFF]", "")
                .trim();
    }

    private String extractFirstJson(String text) {
        int objectStart = text.indexOf('{');
        int arrayStart = text.indexOf('[');

        int start;
        char openChar;
        char closeChar;
        if (objectStart < 0 && arrayStart < 0) {
            return null;
        }
        if (objectStart < 0) {
            start = arrayStart;
            openChar = '[';
            closeChar = ']';
        } else if (arrayStart < 0) {
            start = objectStart;
            openChar = '{';
            closeChar = '}';
        } else {
            start = Math.min(objectStart, arrayStart);
            openChar = start == objectStart ? '{' : '[';
            closeChar = openChar == '{' ? '}' : ']';
        }

        int end = findMatchingClose(text, start, openChar, closeChar);
        if (end < 0) {
            return null;
        }
        return text.substring(start, end + 1).trim();
    }

    private int findMatchingClose(String text, int start, char openChar, char closeChar) {
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String buildSectionsSummary(List<ResumeSection> sections) {
        return sections.stream()
                .map(s -> {
                    try {
                        return s.getSectionType() + ": " + objectMapper.writeValueAsString(sanitizeSectionData(s.getSectionData()));
                    } catch (JsonProcessingException e) {
                        return s.getSectionType() + ": (序列化失败)";
                    }
                })
                .collect(Collectors.joining("\n"));
    }

    private Map<String, Object> sanitizeSectionData(Map<String, Object> sectionData) {
        return sanitizeSectionData(sectionData, MAX_CHAT_SECTION_VALUE_LENGTH);
    }

    private Map<String, Object> sanitizeSectionDataForAi(Map<String, Object> sectionData) {
        return sanitizeSectionData(sectionData, MAX_AI_SECTION_VALUE_LENGTH);
    }

    private Map<String, Object> sanitizeSectionData(Map<String, Object> sectionData, int maxLength) {
        if (sectionData == null) {
            return Map.of();
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();
        sectionData.forEach((key, value) -> {
            Object sanitizedValue = sanitizeSectionValue(value, maxLength);
            if (sanitizedValue != null) {
                sanitized.put(key, sanitizedValue);
            }
        });
        return sanitized;
    }

    private Object sanitizeSectionValue(Object value) {
        return sanitizeSectionValue(value, MAX_CHAT_SECTION_VALUE_LENGTH);
    }

    private Object sanitizeSectionValue(Object value, int maxLength) {
        if (value instanceof String text) {
            if (INLINE_FILE_DATA_PATTERN.matcher(text).matches()) {
                return null;
            }
            return text.length() > maxLength
                    ? text.substring(0, maxLength) + "...(已截断)"
                    : text;
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((key, itemValue) -> {
                Object sanitizedValue = sanitizeSectionValue(itemValue, maxLength);
                if (sanitizedValue != null) {
                    sanitized.put(String.valueOf(key), sanitizedValue);
                }
            });
            return sanitized;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> sanitizeSectionValue(item, maxLength))
                    .filter(item -> item != null)
                    .collect(Collectors.toList());
        }
        return value;
    }

    private SectionVO toTruncatedSectionForAi(SectionVO section) {
        SectionVO truncated = new SectionVO();
        truncated.setSectionType(section.getSectionType());
        truncated.setSectionData(sanitizeSectionDataForAi(section.getSectionData()));
        truncated.setSortOrder(section.getSortOrder());
        return truncated;
    }

    private ResumeDetailVO toResumeDetailVO(Resume resume, List<ResumeSection> sections) {
        ResumeDetailVO vo = new ResumeDetailVO();
        vo.setId(resume.getId());
        vo.setTitle(resume.getTitle());
        vo.setTemplateType(resume.getTemplateType());
        vo.setStatus(resume.getStatus());
        vo.setSource(resume.getSource());
        vo.setVersion(resume.getVersion());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        vo.setSections(sections.stream().map(this::toSectionVO).collect(Collectors.toList()));
        return vo;
    }

    private SectionVO toSectionVO(ResumeSection section) {
        SectionVO vo = new SectionVO();
        vo.setId(section.getId());
        vo.setResumeId(section.getResumeId());
        vo.setSectionType(section.getSectionType());
        vo.setSectionData(section.getSectionData());
        vo.setSortOrder(section.getSortOrder());
        vo.setAiGenerated(section.getAiGenerated());
        vo.setCreateTime(section.getCreateTime());
        vo.setUpdateTime(section.getUpdateTime());
        return vo;
    }
}
