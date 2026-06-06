package com.mianshiba.ai.model.dto.interview;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class InterviewCreateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "简历 id 不能为空")
    private Long resumeId;

    @NotBlank(message = "目标岗位不能为空")
    @Size(max = 128, message = "目标岗位过长")
    private String targetPosition;

    @Size(max = 128, message = "技术方向过长")
    private String techDirection;
}
