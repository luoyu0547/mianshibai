package com.mianshiba.ai.model.vo.training;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TrainingMasteryVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String targetType;
    private String targetName;
    private Integer practiceCount;
    private Integer questionCount;
    private BigDecimal averageScore;
    private Integer weakCount;
    private Integer masteredCount;
    private String masteryLevel;
    private LocalDateTime lastPracticedAt;
}
