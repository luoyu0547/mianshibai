package com.mianshiba.ai.model.dto.resume;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 更新简历请求
 */
@Data
public class ResumeUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Size(max = 128, message = "简历标题最长 128 个字符")
    private String title;

    private String templateType;

    private String status;

    private Map<String, Object> styleSettings;
}
