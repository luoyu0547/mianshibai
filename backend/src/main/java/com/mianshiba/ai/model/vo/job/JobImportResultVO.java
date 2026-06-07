package com.mianshiba.ai.model.vo.job;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 职位导入结果 VO
 */
@Data
public class JobImportResultVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String resultType;

    private Long jobId;

    private Long companyId;

    private JobVO job;

    private CompanyVO company;
}
