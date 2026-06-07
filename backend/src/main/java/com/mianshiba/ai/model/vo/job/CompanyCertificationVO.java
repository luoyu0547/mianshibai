package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 公司认证 VO
 */
@Data
public class CompanyCertificationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String certificationType;

    private String status;

    private String evidenceSource;

    private String evidenceUrl;

    private String evidenceText;

    private Integer confidenceScore;
}
