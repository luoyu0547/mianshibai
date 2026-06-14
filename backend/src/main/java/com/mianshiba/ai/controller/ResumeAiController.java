package com.mianshiba.ai.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mianshiba.ai.common.BaseResponse;
import com.mianshiba.ai.common.ResultUtils;
import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.dto.resume.ChatRequest;
import com.mianshiba.ai.model.dto.resume.ResumeImportRequest;
import com.mianshiba.ai.model.dto.resume.ResumeWholeOptimizeRequest;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ChatMessageVO;
import com.mianshiba.ai.model.vo.resume.ResumeChatStreamEventVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeImportPreviewVO;
import com.mianshiba.ai.model.vo.resume.ResumeWholeOptimizeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import com.mianshiba.ai.service.ResumeAiService;
import com.mianshiba.ai.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resume")
@Tag(name = "简历 AI 接口")
public class ResumeAiController {

    private final ResumeAiService resumeAiService;
    private final ResumeService resumeService;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/ai/generate")
    @Operation(summary = "AI 生成简历")
    public BaseResponse<ResumeDetailVO> generateResume(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @Valid @RequestBody AiGenerateRequest request) {
        return ResultUtils.success(resumeAiService.generateResume(authorizationHeader, request));
    }

    @PostMapping("/{resumeId}/ai/optimize-section")
    @Operation(summary = "AI 优化模块")
    public BaseResponse<Map<String, Object>> optimizeSection(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("resumeId") Long resumeId,
            @Valid @RequestBody AiOptimizeRequest request) {
        ResumeDetailVO detail = resumeService.getResumeDetail(authorizationHeader, resumeId);
        String targetPosition = extractTargetPosition(detail.getSections());
        return ResultUtils.success(resumeAiService.optimizeSection(request, targetPosition));
    }

    @PostMapping("/{resumeId}/ai/score")
    @Operation(summary = "AI 评分")
    public BaseResponse<AiScoreVO> scoreResume(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("resumeId") Long resumeId) {
        ResumeDetailVO detail = resumeService.getResumeDetail(authorizationHeader, resumeId);
        String targetPosition = extractTargetPosition(detail.getSections());
        return ResultUtils.success(resumeAiService.scoreResume(detail.getSections(), targetPosition));
    }

    @PostMapping(value = "/{resumeId}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话")
    public SseEmitter chat(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("resumeId") Long resumeId,
            @Valid @RequestBody ChatRequest chatRequest) {
        SseEmitter emitter = new SseEmitter(60000L);
        sseExecutor.execute(() -> {
            Flux<ResumeChatStreamEventVO> stream = resumeAiService.chatStream(authorizationHeader, resumeId, chatRequest.getMessage());
            stream.subscribe(event -> {
                try {
                    if (ResumeChatStreamEventVO.EVENT_PROPOSAL.equals(event.getEvent())) {
                        emitter.send(SseEmitter.event()
                                .name(ResumeChatStreamEventVO.EVENT_PROPOSAL)
                                .data(toJson(event.getProposal())));
                    } else {
                        emitter.send(SseEmitter.event().data(event.getContent()));
                    }
                } catch (IOException e) {
                    emitter.completeWithError(e);
                }
            }, emitter::completeWithError, emitter::complete);
        });
        return emitter;
    }

    @GetMapping("/{resumeId}/chat/history")
    @Operation(summary = "获取 AI 对话历史")
    public BaseResponse<List<ChatMessageVO>> getChatHistory(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("resumeId") Long resumeId) {
        resumeService.getResumeDetail(authorizationHeader, resumeId);
        return ResultUtils.success(resumeAiService.getChatHistory(resumeId));
    }

    @PostMapping("/ai/import-preview")
    @Operation(summary = "AI 导入简历预览")
    public BaseResponse<ResumeImportPreviewVO> importPreview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody ResumeImportRequest request) {
        return ResultUtils.success(resumeAiService.importResumePreview(authorizationHeader, request));
    }

    @PostMapping("/{resumeId}/ai/optimize-whole")
    @Operation(summary = "整份简历 AI 优化")
    public BaseResponse<ResumeWholeOptimizeVO> optimizeWhole(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @PathVariable("resumeId") Long resumeId,
            @RequestBody ResumeWholeOptimizeRequest request) {
        request.setResumeId(resumeId);
        return ResultUtils.success(resumeAiService.optimizeWholeResume(authorizationHeader, request));
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

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_PARSE_ERROR, "SSE 数据序列化失败");
        }
    }
}
