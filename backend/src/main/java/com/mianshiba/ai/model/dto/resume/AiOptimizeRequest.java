package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * AI 优化模块请求
 */
@Data
public class AiOptimizeRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long sectionId;

    @NotBlank(message = "模块类型不能为空")
    private String sectionType;

    @NotNull(message = "模块数据不能为空")
    private Map<String, Object> sectionData;

    private Long jobId;
}
