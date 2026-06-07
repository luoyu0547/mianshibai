package com.mianshiba.ai.model.dto.job;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 职位匹配请求
 */
@Data
public class JobMatchRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "简历 ID 不能为空")
    private Long resumeId;
}
