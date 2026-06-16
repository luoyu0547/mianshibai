package com.mianshiba.ai.model.dto.application;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ApplicationRoundUpdateRequest {
    private String roundName;
    private LocalDateTime scheduledAt;
    private String notes;
}
