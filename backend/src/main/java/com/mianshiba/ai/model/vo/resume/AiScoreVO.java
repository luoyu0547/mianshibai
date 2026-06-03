package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * AI 评分 VO
 */
@Data
public class AiScoreVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer score;

    private ScoreDimensions dimensions;

    private List<String> suggestions;

    @Data
    public static class ScoreDimensions implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private Integer completeness;

        private String completenessComment;

        private Integer professionalism;

        private String professionalismComment;

        private Integer matching;

        private String matchingComment;
    }
}
