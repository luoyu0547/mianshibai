package com.mianshiba.ai.model.dto.interview;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class InterviewAnswerRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "回答内容不能为空")
    private String answerText;

    @Min(value = 0, message = "回答时长不能为负数")
    private Integer answerDurationSeconds;
}
