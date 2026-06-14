package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.resume.AiGenerateRequest;
import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.dto.resume.ResumeImportRequest;
import com.mianshiba.ai.model.dto.resume.ResumeWholeOptimizeRequest;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ChatMessageVO;
import com.mianshiba.ai.model.vo.resume.ResumeChatStreamEventVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.ResumeImportPreviewVO;
import com.mianshiba.ai.model.vo.resume.ResumeWholeOptimizeVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface ResumeAiService {

    ResumeDetailVO generateResume(String authorizationHeader, AiGenerateRequest request);

    Map<String, Object> optimizeSection(AiOptimizeRequest request, String targetPosition);

    AiScoreVO scoreResume(List<SectionVO> sections, String targetPosition);

    Flux<ResumeChatStreamEventVO> chatStream(String authorizationHeader, Long resumeId, String message);

    List<ChatMessageVO> getChatHistory(Long resumeId);

    ResumeImportPreviewVO importResumePreview(String authorizationHeader, ResumeImportRequest request);

    ResumeWholeOptimizeVO optimizeWholeResume(String authorizationHeader, ResumeWholeOptimizeRequest request);
}
