package com.mianshiba.ai.service.tool;

import com.mianshiba.ai.exception.BusinessException;
import com.mianshiba.ai.exception.ErrorCode;
import com.mianshiba.ai.model.dto.resume.ResumePatchRequest;
import com.mianshiba.ai.model.vo.resume.ResumePatchProposalVO;
import org.springframework.ai.tool.annotation.Tool;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ResumePatchTools {

    private static final Set<String> VALID_SECTION_TYPES = Set.of("basic", "education", "work", "project", "skills", "summary");
    private static final String OPERATION_REPLACE_SECTION = "replace_section";

    private final Consumer<ResumePatchProposalVO> proposalConsumer;

    public ResumePatchTools(Consumer<ResumePatchProposalVO> proposalConsumer) {
        this.proposalConsumer = proposalConsumer;
    }

    @Tool(description = "Propose a resume section edit for user review. Use this when the user asks to modify, polish, rewrite, add, or fill resume content. Do not save or apply changes. The only supported operation is replace_section.")
    public String proposeResumePatch(ResumePatchRequest request) {
        validate(request);
        ResumePatchProposalVO proposal = new ResumePatchProposalVO();
        proposal.setSectionType(request.getSectionType().trim());
        proposal.setOperation(request.getOperation().trim());
        proposal.setReason(request.getReason() == null ? "AI 建议修改该模块" : request.getReason().trim());
        proposal.setSectionData(normalizeSectionData(proposal.getSectionType(), request.getSectionData()));
        proposalConsumer.accept(proposal);
        return "已生成简历修改提案，等待用户确认后才会应用。";
    }

    private Map<String, Object> normalizeSectionData(String sectionType, Map<String, Object> sectionData) {
        Map<String, Object> normalized = new LinkedHashMap<>(sectionData);
        if ("education".equals(sectionType)) {
            Object activities = normalized.get("activities");
            if (activities == null) {
                activities = normalized.remove("description");
            }
            if (activities == null) {
                activities = normalized.remove("highlights");
            } else {
                normalized.remove("highlights");
            }
            if (activities != null) {
                normalized.put("activities", toHtmlList(activities));
            }
        }
        return normalized;
    }

    private String toHtmlList(Object value) {
        if (value instanceof Iterable<?> iterable) {
            String items = toStream(iterable)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .map(item -> "<li>" + item + "。</li>")
                    .collect(Collectors.joining());
            return items.isBlank() ? "" : "<ul>" + items + "</ul>";
        }
        String text = value == null ? "" : value.toString().trim();
        if (text.isBlank() || text.toLowerCase().contains("<ul>")) {
            return text;
        }
        String items = java.util.Arrays.stream(text.split("[。；;\\n]+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .map(item -> "<li>" + item + "。</li>")
                .collect(Collectors.joining());
        return items.isBlank() ? text : "<ul>" + items + "</ul>";
    }

    private java.util.stream.Stream<?> toStream(Iterable<?> iterable) {
        return java.util.stream.StreamSupport.stream(iterable.spliterator(), false);
    }

    private void validate(ResumePatchRequest request) {
        if (request == null || request.getSectionType() == null || request.getOperation() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历修改提案参数不完整");
        }
        String sectionType = request.getSectionType().trim();
        String operation = request.getOperation().trim();
        Map<String, Object> sectionData = request.getSectionData();
        if (!VALID_SECTION_TYPES.contains(sectionType)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历模块类型不合法");
        }
        if (!OPERATION_REPLACE_SECTION.equals(operation)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历修改操作不支持");
        }
        if (sectionData == null || sectionData.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "简历修改内容不能为空");
        }
    }
}
