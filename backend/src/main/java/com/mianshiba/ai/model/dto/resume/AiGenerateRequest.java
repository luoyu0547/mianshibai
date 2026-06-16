package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 生成简历请求
 */
@Data
public class AiGenerateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "目标岗位不能为空")
    private String targetPosition;

    private String techDirection;

    private Integer workYears;

    private String backgroundDescription;
}
