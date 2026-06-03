package com.mianshiba.ai.model.vo.resume;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 简历模块 VO
 */
@Data
public class SectionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long resumeId;

    private String sectionType;

    private Map<String, Object> sectionData;

    private Integer sortOrder;

    private Integer aiGenerated;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
