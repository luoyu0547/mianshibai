package com.mianshiba.ai.model.vo.application;

import lombok.Data;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ApplicationRoundVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long applicationId;
    private String roundName;
    private Integer roundOrder;
    private LocalDateTime scheduledAt;
    private String result;
    private String notes;
}
