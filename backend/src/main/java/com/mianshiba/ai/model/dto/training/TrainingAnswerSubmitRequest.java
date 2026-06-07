package com.mianshiba.ai.model.dto.training;

import lombok.Data;

/**
 * 提交训练答案请求
 */
@Data
public class TrainingAnswerSubmitRequest {

    /**
     * 答案文本
     */
    private String answerText;
}
