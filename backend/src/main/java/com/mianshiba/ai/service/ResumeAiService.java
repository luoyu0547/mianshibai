package com.mianshiba.ai.service;

import com.mianshiba.ai.model.dto.resume.AiOptimizeRequest;
import com.mianshiba.ai.model.vo.resume.AiScoreVO;
import com.mianshiba.ai.model.vo.resume.ResumeDetailVO;
import com.mianshiba.ai.model.vo.resume.SectionVO;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

public interface ResumeAiService {

    ResumeDetailVO generateResume(String authorizationHeader, com.mianshiba.ai.model.dto.resume.AiGenerateRequest request);

    Map<String, Object> optimizeSection(AiOptimizeRequest request, String targetPosition);

    AiScoreVO scoreResume(List<SectionVO> sections, String targetPosition);

    Flux<String> chatStream(String authorizationHeader, Long resumeId, String message);
}
