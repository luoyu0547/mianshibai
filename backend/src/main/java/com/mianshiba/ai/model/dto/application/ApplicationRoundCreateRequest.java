package com.mianshiba.ai.model.dto.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationRoundCreateRequest {
    @NotBlank(message = "轮次名称不能为空")
    @Size(max = 64, message = "轮次名称不能超过64个字符")
    private String roundName;

    private LocalDateTime scheduledAt;
    private String notes;
}
