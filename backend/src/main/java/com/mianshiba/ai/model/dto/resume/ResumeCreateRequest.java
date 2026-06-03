package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建简历请求
 */
@Data
public class ResumeCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "简历标题不能为空")
    @Size(max = 128, message = "简历标题最长 128 个字符")
    private String title;

    private String templateType;
}
